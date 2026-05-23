package com.communitybot.auth.service;

import com.communitybot.organization.domain.OrgMember;
import com.communitybot.organization.domain.OrgRole;
import com.communitybot.organization.repository.OrgMemberRepository;
import com.communitybot.organization.service.OrganizationService;
import com.communitybot.organization.dto.CreateOrgRequest;
import com.communitybot.auth.service.UserService;
import com.communitybot.auth.domain.User;
import com.communitybot.workspace.domain.WorkspaceMember;
import com.communitybot.workspace.domain.WorkspaceRole;
import com.communitybot.workspace.dto.CreateWorkspaceRequest;
import com.communitybot.workspace.repository.WorkspaceMemberRepository;
import com.communitybot.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Seeds a small set of local-login accounts and a demo workspace in the dev profile.
 */
@Component
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class LocalDevAccountSeeder implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "admin@communitybot.local";
    public static final String MOD_EMAIL   = "mod@communitybot.local";
    public static final String USER_EMAIL  = "user@communitybot.local";

    private static final String DEMO_ORG_NAME    = "Community Bot Demo";
    private static final String DEMO_WS_NAME      = "General";

    private final LocalAuthService localAuthService;
    private final OrganizationService organizationService;
    private final WorkspaceService workspaceService;
    private final UserService userService;
    private final OrgMemberRepository orgMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var admin = localAuthService.seedAccount(ADMIN_EMAIL, "Admin123!", "Local Admin");
        var mod   = localAuthService.seedAccount(MOD_EMAIL, "Mod123!", "Local Moderator");
        var user  = localAuthService.seedAccount(USER_EMAIL, "User123!", "Local User");

        UUID orgId = organizationService.listForUser(admin.getId()).stream()
                .filter(org -> DEMO_ORG_NAME.equals(org.name()))
                .map(org -> org.id())
                .findFirst()
                .orElseGet(() -> organizationService.create(new CreateOrgRequest(DEMO_ORG_NAME), admin.getId()).id());

        UUID workspaceId = workspaceService.listForUser(orgId, admin.getId()).stream()
                .filter(ws -> DEMO_WS_NAME.equals(ws.name()))
                .map(ws -> ws.id())
                .findFirst()
                .orElseGet(() -> workspaceService.create(orgId, new CreateWorkspaceRequest(DEMO_WS_NAME, "Default workspace for local auth testing"), admin.getId()).id());

        ensureOrgMember(orgId, mod.getId(), OrgRole.MEMBER);
        ensureOrgMember(orgId, user.getId(), OrgRole.MEMBER);

        ensureWorkspaceRole(orgId, workspaceId, mod.getId(), WorkspaceRole.MODERATOR);
        ensureWorkspaceRole(orgId, workspaceId, user.getId(), WorkspaceRole.MEMBER);

        log.info("Seeded local auth accounts: {}, {}, {}", ADMIN_EMAIL, MOD_EMAIL, USER_EMAIL);
    }

    private void ensureOrgMember(UUID orgId, UUID userId, OrgRole role) {
        orgMemberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .ifPresentOrElse(member -> {
                    if (member.getRole() != role && member.getRole() == OrgRole.MEMBER) {
                        member.changeRole(role);
                    }
                }, () -> orgMemberRepository.save(
                        OrgMember.builder()
                                .organization(organizationService.getOrThrow(orgId))
                                .user(userService.getOrThrow(userId))
                                .role(role)
                                .build()
                ));
    }

    private void ensureWorkspaceRole(UUID orgId, UUID workspaceId, UUID userId, WorkspaceRole role) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            workspaceService.joinWorkspace(orgId, workspaceId, userId);
        }

        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .ifPresent(member -> {
                    if (member.getRole() != role) {
                        member.changeRole(role);
                    }
                });
    }
}
