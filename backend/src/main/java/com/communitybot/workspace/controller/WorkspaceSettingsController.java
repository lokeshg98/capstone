package com.communitybot.workspace.controller;

import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.shared.security.CurrentUser;
import com.communitybot.workspace.dto.MemberRolesResponse;
import com.communitybot.workspace.dto.WorkspaceRoleResponse;
import com.communitybot.workspace.service.WorkspaceService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Workspace-scoped settings and role management — consumed by the workspace page.
 */
@RestController
@RequestMapping("/api/workspaces/{wsId}")
@RequiredArgsConstructor
public class WorkspaceSettingsController {

    private final WorkspaceService wsService;

    @GetMapping("/welcome-message")
    public ResponseEntity<ApiResponse<String>> getWelcomeTemplate(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(wsService.getWelcomeTemplate(wsId, userId)));
    }

    @PutMapping("/welcome-message")
    public ResponseEntity<ApiResponse<Void>> updateWelcomeTemplate(
            @PathVariable UUID wsId,
            @RequestBody Map<String, @Size(max = 2000) String> body,
            @CurrentUser UUID userId
    ) {
        String template = body.get("template");
        if (template == null) throw new AppException(ErrorCode.VALIDATION_ERROR, "template is required");
        wsService.updateWelcomeTemplate(wsId, userId, template);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Roles ──────────────────────────────────────────────────────────────────

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<WorkspaceRoleResponse>>> listRoles(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(wsService.listRoles(wsId, userId)));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<WorkspaceRoleResponse>> createRole(
            @PathVariable UUID wsId,
            @RequestBody Map<String, @NotBlank @Size(max = 50) String> body,
            @CurrentUser UUID userId
    ) {
        String name = body.get("name");
        if (name == null) throw new AppException(ErrorCode.VALIDATION_ERROR, "name is required");
        WorkspaceRoleResponse role = wsService.createRole(wsId, name, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(role));
    }

    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID wsId,
            @PathVariable UUID roleId,
            @CurrentUser UUID userId
    ) {
        wsService.deleteRole(wsId, roleId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Member roles ───────────────────────────────────────────────────────────

    @GetMapping("/members/roles")
    public ResponseEntity<ApiResponse<List<MemberRolesResponse>>> listMembersWithRoles(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(wsService.listMembersWithRoles(wsId, userId)));
    }

    @PostMapping("/members/{memberId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> assignRoleToMember(
            @PathVariable UUID wsId,
            @PathVariable UUID memberId,
            @PathVariable UUID roleId,
            @CurrentUser UUID userId
    ) {
        wsService.assignRoleToMember(wsId, memberId, roleId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/members/{memberId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> removeRoleFromMember(
            @PathVariable UUID wsId,
            @PathVariable UUID memberId,
            @PathVariable UUID roleId,
            @CurrentUser UUID userId
    ) {
        wsService.removeRoleFromMember(wsId, memberId, roleId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/community-guidelines")
    public ResponseEntity<ApiResponse<String>> getCommunityGuidelines(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(wsService.getCommunityGuidelines(wsId, userId)));
    }

    @PutMapping("/community-guidelines")
    public ResponseEntity<ApiResponse<Void>> updateCommunityGuidelines(
            @PathVariable UUID wsId,
            @RequestBody Map<String, @Size(max = 10000) String> body,
            @CurrentUser UUID userId
    ) {
        wsService.updateCommunityGuidelines(wsId, userId, body.get("guidelines"));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
