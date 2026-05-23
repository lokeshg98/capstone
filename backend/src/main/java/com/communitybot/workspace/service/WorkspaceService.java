package com.communitybot.workspace.service;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.channel.service.ChannelService;
import com.communitybot.organization.domain.Organization;
import com.communitybot.organization.service.OrganizationService;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.shared.util.SlugGenerator;
import com.communitybot.workspace.config.WelcomeProperties;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.domain.WorkspaceMember;
import com.communitybot.workspace.domain.WorkspaceRoleEntity;
import com.communitybot.workspace.dto.CreateWorkspaceRequest;
import com.communitybot.workspace.dto.MemberRolesResponse;
import com.communitybot.workspace.dto.WorkspaceResponse;
import com.communitybot.workspace.dto.WorkspaceRoleResponse;
import com.communitybot.workspace.event.UserJoinedWorkspaceEvent;
import com.communitybot.workspace.repository.WorkspaceMemberRepository;
import com.communitybot.workspace.repository.WorkspaceRepository;
import com.communitybot.workspace.repository.WorkspaceRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final List<String> DEFAULT_ROLE_NAMES = List.of("Admin", "Moderator", "User");

    private final WorkspaceRepository         wsRepository;
    private final WorkspaceMemberRepository   memberRepository;
    private final WorkspaceRoleRepository     roleRepository;
    private final OrganizationService         orgService;
    private final UserService                 userService;
    private final ChannelService              channelService;
    private final ApplicationEventPublisher   eventPublisher;
    private final WelcomeProperties         welcomeProperties;

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
                        .welcomeMessageTemplate(welcomeProperties.getDefaultTemplate())
                        .build()
        );

        seedDefaultRoles(ws, creator);
        WorkspaceRoleEntity adminRole = roleRepository.findByWorkspaceIdAndName(ws.getId(), "Admin").orElseThrow();
        WorkspaceMember member = memberRepository.save(
                WorkspaceMember.builder()
                        .workspace(ws)
                        .user(creator)
                        .roles(new java.util.HashSet<>(Set.of(adminRole)))
                        .build()
        );

        channelService.createGeneralChannel(ws, creator);

        return WorkspaceResponse.from(ws, member);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listForUser(UUID orgId, UUID userId) {
        orgService.requireMembership(orgId, userId);
        return wsRepository.findAllByMemberUserIdAndOrgId(userId, orgId).stream()
                .map(ws -> {
                    WorkspaceMember member = memberRepository
                            .findByWorkspaceIdAndUserId(ws.getId(), userId)
                            .orElse(null);
                    List<String> roles = member != null
                            ? member.getRoles().stream().map(r -> r.getName()).sorted().toList()
                            : List.of();
                    return WorkspaceResponse.from(ws, roles);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getById(UUID orgId, UUID wsId, UUID userId) {
        orgService.requireMembership(orgId, userId);
        Workspace ws = getOrThrow(wsId);
        requireMembership(wsId, userId);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(wsId, userId).orElse(null);
        List<String> roles = member != null
                ? member.getRoles().stream().map(r -> r.getName()).sorted().toList()
                : List.of();
        return WorkspaceResponse.from(ws, roles);
    }

    /**
     * Lets an existing org member join a workspace they are not yet part of.
     * New members start with no roles. Automatically added to #general.
     */
    @Transactional
    public WorkspaceResponse joinWorkspace(UUID orgId, UUID wsId, UUID userId) {
        orgService.requireMembership(orgId, userId);
        Workspace ws   = getOrThrow(wsId);
        User      user = userService.getOrThrow(userId);

        if (memberRepository.existsByWorkspaceIdAndUserId(wsId, userId)) {
            WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(wsId, userId).orElseThrow();
            return WorkspaceResponse.from(ws, member);
        }

        memberRepository.save(
                WorkspaceMember.builder()
                        .workspace(ws)
                        .user(user)
                        .build()
        );

        channelService.joinGeneralChannel(wsId, user);

        eventPublisher.publishEvent(new UserJoinedWorkspaceEvent(wsId, userId));

        return WorkspaceResponse.from(ws, Collections.emptyList());
    }

    // ── Roles ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorkspaceRoleResponse> listRoles(UUID wsId, UUID userId) {
        requireMembership(wsId, userId);
        return roleRepository.findAllByWorkspaceIdOrderByNameAsc(wsId).stream()
                .map(WorkspaceRoleResponse::from)
                .toList();
    }

    @Transactional
    public WorkspaceRoleResponse createRole(UUID wsId, String name, UUID userId) {
        requireModeratorOrAdmin(wsId, userId);
        if (roleRepository.existsByWorkspaceIdAndName(wsId, name)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "A role named '" + name + "' already exists in this workspace");
        }
        Workspace ws = getOrThrow(wsId);
        User creator = userService.getOrThrow(userId);
        WorkspaceRoleEntity role = roleRepository.save(
                WorkspaceRoleEntity.builder()
                        .workspace(ws)
                        .name(name)
                        .isSystem(false)
                        .createdBy(creator)
                        .build()
        );
        return WorkspaceRoleResponse.from(role);
    }

    @Transactional
    public void deleteRole(UUID wsId, UUID roleId, UUID userId) {
        requireModeratorOrAdmin(wsId, userId);
        WorkspaceRoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (!role.getWorkspace().getId().equals(wsId)) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        if (role.isSystem()) {
            throw new AppException(ErrorCode.ROLE_SYSTEM_PROTECTED, "System roles cannot be deleted");
        }
        roleRepository.delete(role);
    }

    // ── Member role management ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MemberRolesResponse> listMembersWithRoles(UUID wsId, UUID userId) {
        requireMembership(wsId, userId);
        return wsRepository.findById(wsId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND))
                .getMembers().stream()
                .map(MemberRolesResponse::from)
                .toList();
    }

    @Transactional
    public void assignRoleToMember(UUID wsId, UUID memberRecordId, UUID roleId, UUID requesterId) {
        requireModeratorOrAdmin(wsId, requesterId);
        WorkspaceMember member = memberRepository.findById(memberRecordId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        if (!member.getWorkspace().getId().equals(wsId)) {
            throw new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        WorkspaceRoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (!role.getWorkspace().getId().equals(wsId)) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        member.addRole(role);
    }

    @Transactional
    public void removeRoleFromMember(UUID wsId, UUID memberRecordId, UUID roleId, UUID requesterId) {
        requireModeratorOrAdmin(wsId, requesterId);
        WorkspaceMember member = memberRepository.findById(memberRecordId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        if (!member.getWorkspace().getId().equals(wsId)) {
            throw new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        WorkspaceRoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (!role.getWorkspace().getId().equals(wsId)) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        member.removeRole(role);
    }

    /** Adds the user to every workspace in the org (used after org self-join). */
    @Transactional
    public void joinAllInOrg(UUID orgId, UUID userId) {
        orgService.requireMembership(orgId, userId);
        for (Workspace ws : wsRepository.findAllByOrganizationIdOrderByNameAsc(orgId)) {
            if (!memberRepository.existsByWorkspaceIdAndUserId(ws.getId(), userId)) {
                joinWorkspace(orgId, ws.getId(), userId);
            }
        }
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

    @Transactional(readOnly = true)
    public String getCommunityGuidelines(UUID wsId, UUID userId) {
        requireMembership(wsId, userId);
        Workspace ws = getOrThrow(wsId);
        return ws.getCommunityGuidelines();
    }

    @Transactional
    public void updateCommunityGuidelines(UUID wsId, UUID userId, String guidelines) {
        requireModeratorOrAdmin(wsId, userId);
        getOrThrow(wsId).setCommunityGuidelines(guidelines);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Workspace getOrThrow(UUID wsId) {
        return wsRepository.findById(wsId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    public void requireMembership(UUID wsId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(wsId, userId)) {
            throw new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
    }

    /** Checks if the user holds Admin or Moderator role in the workspace. */
    public void requireModeratorOrAdmin(UUID wsId, UUID userId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(wsId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        boolean hasElevated = member.hasRole("Admin") || member.hasRole("Moderator");
        if (!hasElevated) {
            throw new AppException(ErrorCode.MODERATION_INSUFFICIENT_ROLE);
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private void seedDefaultRoles(Workspace ws, User creator) {
        for (String name : DEFAULT_ROLE_NAMES) {
            if (!roleRepository.existsByWorkspaceIdAndName(ws.getId(), name)) {
                roleRepository.save(
                        WorkspaceRoleEntity.builder()
                                .workspace(ws)
                                .name(name)
                                .isSystem(true)
                                .createdBy(creator)
                                .build()
                );
            }
        }
    }

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
