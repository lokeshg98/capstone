package com.communitybot.channel.service;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.domain.ChannelMember;
import com.communitybot.channel.domain.ChannelType;
import com.communitybot.channel.dto.ChannelMemberResponse;
import com.communitybot.channel.dto.ChannelResponse;
import com.communitybot.channel.dto.CreateChannelRequest;
import com.communitybot.channel.repository.ChannelMemberRepository;
import com.communitybot.channel.repository.ChannelRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.shared.util.SlugGenerator;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.domain.WorkspaceMember;
import com.communitybot.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Channel CRUD.
 * Workspace membership is validated directly via {@link WorkspaceMemberRepository}
 * to avoid a circular service dependency with WorkspaceService.
 */
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository          channelRepository;
    private final ChannelMemberRepository    channelMemberRepository;
    private final WorkspaceMemberRepository  wsMemberRepository;
    private final UserService                userService;

    @Transactional
    public ChannelResponse create(UUID workspaceId, Workspace workspace,
                                  CreateChannelRequest req, UUID creatorId) {
        requireWorkspaceMember(workspaceId, creatorId);
        User creator = userService.getOrThrow(creatorId);

        String slug = uniqueSlug(workspaceId, req.name());

        Channel channel = channelRepository.save(
                Channel.builder()
                        .workspace(workspace)
                        .name(req.name())
                        .slug(slug)
                        .type(ChannelType.PUBLIC)
                        .description(req.description())
                        .createdBy(creator)
                        .build()
        );

        channelMemberRepository.save(
                ChannelMember.builder().channel(channel).user(creator).build()
        );

        addAllWorkspaceMembers(channel, workspaceId, creatorId);

        return ChannelResponse.from(channel, true);
    }

    /**
     * Called by {@link com.communitybot.workspace.service.WorkspaceService}
     * when auto-creating the #general channel.  Skips the workspace-membership check
     * because the workspace and creator are already verified by the caller.
     */
    @Transactional
    public Channel createGeneralChannel(Workspace workspace, User creator) {
        Channel general = channelRepository.save(
                Channel.builder()
                        .workspace(workspace)
                        .name("general")
                        .slug("general")
                        .type(ChannelType.PUBLIC)
                        .description("General discussion")
                        .createdBy(creator)
                        .build()
        );
        channelMemberRepository.save(
                ChannelMember.builder().channel(general).user(creator).build()
        );
        return general;
    }

    /**
     * Adds a user who has just joined a workspace to every public channel.
     * No-op for channels where the user is already a member.
     */
    @Transactional
    public void joinAllPublicChannels(UUID workspaceId, User user) {
        channelRepository.findAllByWorkspaceIdAndTypeOrderByNameAsc(workspaceId, ChannelType.PUBLIC)
                .forEach(channel -> {
                    if (!channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), user.getId())) {
                        channelMemberRepository.save(
                                ChannelMember.builder().channel(channel).user(user).build()
                        );
                    }
                });
    }

    /** @deprecated use {@link #joinAllPublicChannels(UUID, User)} */
    @Deprecated
    @Transactional
    public void joinGeneralChannel(UUID workspaceId, User user) {
        joinAllPublicChannels(workspaceId, user);
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> listForUser(UUID workspaceId, UUID userId) {
        requireWorkspaceMember(workspaceId, userId);
        return channelRepository.findAllByWorkspaceIdAndTypeOrderByNameAsc(workspaceId, ChannelType.PUBLIC)
                .stream()
                .map(c -> ChannelResponse.from(
                        c,
                        channelMemberRepository.existsByChannelIdAndUserId(c.getId(), userId)))
                .toList();
    }

    /** Join a public channel. */
    @Transactional
    public ChannelResponse join(UUID workspaceId, UUID channelId, UUID userId) {
        requireWorkspaceMember(workspaceId, userId);
        Channel channel = getOrThrow(channelId);

        if (channel.getType() != ChannelType.PUBLIC) {
            throw new AppException(ErrorCode.CHANNEL_ACCESS_DENIED, "This channel requires an invitation");
        }
        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            return ChannelResponse.from(channel, true);
        }

        User user = userService.getOrThrow(userId);
        channelMemberRepository.save(ChannelMember.builder().channel(channel).user(user).build());
        return ChannelResponse.from(channel, true);
    }

    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> listMembers(UUID workspaceId, UUID channelId, UUID userId) {
        requireWorkspaceMember(workspaceId, userId);
        Channel channel = getOrThrow(channelId);
        if (!channel.getWorkspace().getId().equals(workspaceId)) {
            throw new AppException(ErrorCode.CHANNEL_NOT_FOUND);
        }
        if (channel.getType() != ChannelType.PUBLIC) {
            requireChannelMember(channelId, userId);
        }

        if (channel.getType() == ChannelType.PUBLIC) {
            return wsMemberRepository.findAllByWorkspaceId(workspaceId).stream()
                    .map(wm -> toMemberResponse(workspaceId, wm))
                    .toList();
        }

        return channelMemberRepository.findAllByChannelIdWithUser(channelId).stream()
                .map(cm -> toMemberResponse(workspaceId, cm))
                .toList();
    }

    // -------------------------------------------------------------------------

    public Channel getOrThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
    }

    public void requireChannelMember(UUID channelId, UUID userId) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new AppException(ErrorCode.CHANNEL_ACCESS_DENIED);
        }
    }

    // -------------------------------------------------------------------------

    private void requireWorkspaceMember(UUID workspaceId, UUID userId) {
        if (!wsMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
    }

    private ChannelMemberResponse toMemberResponse(UUID workspaceId, ChannelMember cm) {
        var member = wsMemberRepository.findByWorkspaceIdAndUserId(workspaceId, cm.getUser().getId());
        List<String> roles = member.map(m -> m.getRoles().stream()
                .map(r -> r.getName()).sorted().toList()).orElse(List.of());
        return ChannelMemberResponse.from(cm, false, roles);
    }

    private ChannelMemberResponse toMemberResponse(UUID workspaceId, WorkspaceMember wm) {
        List<String> roles = wm.getRoles().stream().map(r -> r.getName()).sorted().toList();
        return ChannelMemberResponse.fromWorkspaceMember(wm, false, roles);
    }

    private void addAllWorkspaceMembers(Channel channel, UUID workspaceId, UUID creatorId) {
        for (WorkspaceMember wm : wsMemberRepository.findAllByWorkspaceId(workspaceId)) {
            UUID uid = wm.getUser().getId();
            if (uid.equals(creatorId)) {
                continue;
            }
            if (!channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), uid)) {
                channelMemberRepository.save(
                        ChannelMember.builder().channel(channel).user(wm.getUser()).build()
                );
            }
        }
    }

    private String uniqueSlug(UUID workspaceId, String name) {
        String base = SlugGenerator.from(name);
        if (!channelRepository.existsByWorkspaceIdAndSlug(workspaceId, base)) return base;
        for (int i = 2; i <= 99; i++) {
            String candidate = base + "-" + i;
            if (!channelRepository.existsByWorkspaceIdAndSlug(workspaceId, candidate)) return candidate;
        }
        throw new AppException(ErrorCode.CHANNEL_SLUG_TAKEN);
    }
}
