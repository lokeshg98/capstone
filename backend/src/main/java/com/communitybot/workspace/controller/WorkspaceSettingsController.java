package com.communitybot.workspace.controller;

import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import com.communitybot.workspace.service.WorkspaceService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Workspace-scoped settings that don't need an orgId — consumed by the
 * workspace page where only wsId is in the URL.
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
        wsService.updateWelcomeTemplate(wsId, userId, body.get("template"));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
