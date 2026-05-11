package com.communitybot.workspace.service;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.channel.service.ChannelService;
import com.communitybot.organization.domain.Organization;
import com.communitybot.organization.service.OrganizationService;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.shared.util.SlugGenerator;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.domain.WorkspaceMember;
import com.communitybot.workspace.domain.WorkspaceRole;
import com.communitybot.workspace.dto.CreateWorkspaceRequest;
import com.communitybot.workspace.dto.WorkspaceResponse;
import com.communitybot.workspace.event.UserJoinedWorkspaceEvent;
import com.communitybot.workspace.repository.WorkspaceMemberRepository;
import com.communitybot.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository       wsRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final OrganizationService       orgService;
    private final UserService               userService;
    private final ChannelService            channelService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WorkspaceResponse create(UUID orgId, CreateWorkspaceRequest req, UUID creatorId) {
        orgService.requireMembership(orgId, creatorId);
        Organization org = orgService.getOrThrow(orgId);
        User creator = userService.getOrThrow(creatorId);

        String slug = uniqueSlug(orgId, req.name());

        Workspace ws = wsRepository.save(
                Workspace.builder()
                        .organization(org)
                        .name(req.name())
                        .slug(slug)
                        .description(req.description())
                        .build()
        );

        memberRepository.save(
                WorkspaceMember.builder()
                        .workspace(ws)
                        .user(creator)
                        .role(WorkspaceRole.ADMIN)
                        .build()
        );

        // Every new workspace starts with a #general channel
        channelService.createGeneralChannel(ws, creator);

        return WorkspaceResponse.from(ws, WorkspaceRole.ADMIN);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listForUser(UUID orgId, UUID userId) {
        orgService.requireMembership(orgId, userId);
        return wsRepository.findAllByMemberUserIdAndOrgId(userId, orgId).stream()
                .map(ws -> {
                    WorkspaceRole role = memberRepository
                            .findByWorkspaceIdAndUserId(ws.getId(), userId)
                            .map(WorkspaceMember::getRole)
                            .orElse(WorkspaceRole.MEMBER);
                    return WorkspaceResponse.from(ws, role);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getById(UUID orgId, UUID wsId, UUID userId) {
        orgService.requireMembership(orgId, userId);
        Workspace ws = getOrThrow(wsId);
        requireMembership(wsId, userId);
        WorkspaceRole role = memberRepository
                .findByWorkspaceIdAndUserId(wsId, userId)
                .map(WorkspaceMember::getRole)
                .orElse(WorkspaceRole.MEMBER);
        return WorkspaceResponse.from(ws, role);
    }

    /**
     * Lets an existing org member join a workspace they are not yet part of.
     * Automatically adds them to #general and fires a welcome message if one is configured.
     */
    @Transactional
    public WorkspaceResponse joinWorkspace(UUID orgId, UUID wsId, UUID userId) {
        orgService.requireMembership(orgId, userId);
        Workspace ws   = getOrThrow(wsId);
        User      user = userService.getOrThrow(userId);

        if (memberRepository.existsByWorkspaceIdAndUserId(wsId, userId)) {
            WorkspaceRole role = memberRepository.findByWorkspaceIdAndUserId(wsId, userId)
                    .map(WorkspaceMember::getRole).orElse(WorkspaceRole.MEMBER);
            return WorkspaceResponse.from(ws, role);
        }

        memberRepository.save(
                WorkspaceMember.builder()
                        .workspace(ws)
                        .user(user)
                        .role(WorkspaceRole.MEMBER)
                        .build()
        );

        channelService.joinGeneralChannel(wsId, user);

        // Publish after the member row is flushed; the welcome listener fires after commit.
        eventPublisher.publishEvent(new UserJoinedWorkspaceEvent(wsId, userId));

        return WorkspaceResponse.from(ws, WorkspaceRole.MEMBER);
    }

    // ── Welcome message ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String getWelcomeTemplate(UUID wsId, UUID userId) {
        requireMembership(wsId, userId);
        return getOrThrow(wsId).getWelcomeMessageTemplate();
    }

    @Transactional
    public void updateWelcomeTemplate(UUID wsId, UUID userId, String template) {
        requireModeratorOrAdmin(wsId, userId);
        Workspace ws = getOrThrow(wsId);
        ws.setWelcomeMessageTemplate(template);
    }

    // -------------------------------------------------------------------------
    // Helpers used by downstream services (channels, messages)

    public Workspace getOrThrow(UUID wsId) {
        return wsRepository.findById(wsId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    public void requireMembership(UUID wsId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(wsId, userId)) {
            throw new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
    }

    /** Throws MODERATION_INSUFFICIENT_ROLE if the user is only a MEMBER. */
    public void requireModeratorOrAdmin(UUID wsId, UUID userId) {
        WorkspaceRole role = memberRepository.findByWorkspaceIdAndUserId(wsId, userId)
                .map(WorkspaceMember::getRole)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        if (role == WorkspaceRole.MEMBER) {
            throw new AppException(ErrorCode.MODERATION_INSUFFICIENT_ROLE);
        }
    }

    // -------------------------------------------------------------------------

    private String uniqueSlug(UUID orgId, String name) {
        String base = SlugGenerator.from(name);
        if (!wsRepository.existsByOrganizationIdAndSlug(orgId, base)) return base;
        for (int i = 2; i <= 99; i++) {
            String candidate = base + "-" + i;
            if (!wsRepository.existsByOrganizationIdAndSlug(orgId, candidate)) return candidate;
        }
        throw new AppException(ErrorCode.WORKSPACE_SLUG_TAKEN);
    }
}
