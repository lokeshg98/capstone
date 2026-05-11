package com.communitybot.workspace.controller;

import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import com.communitybot.workspace.dto.CreateWorkspaceRequest;
import com.communitybot.workspace.dto.WorkspaceResponse;
import com.communitybot.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Workspaces are nested under organisations: /api/organizations/{orgId}/workspaces
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService wsService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateWorkspaceRequest request,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(wsService.create(orgId, request, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listMine(
            @PathVariable UUID orgId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(wsService.listForUser(orgId, userId)));
    }

    @GetMapping("/{wsId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getOne(
            @PathVariable UUID orgId,
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(wsService.getById(orgId, wsId, userId)));
    }

    /** Allows an org member who is not yet in the workspace to join it. */
    @PostMapping("/{wsId}/join")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> join(
            @PathVariable UUID orgId,
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(wsService.joinWorkspace(orgId, wsId, userId)));
    }
}
