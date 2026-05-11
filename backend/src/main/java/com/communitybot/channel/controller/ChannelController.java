package com.communitybot.channel.controller;

import com.communitybot.channel.dto.ChannelResponse;
import com.communitybot.channel.dto.CreateChannelRequest;
import com.communitybot.channel.service.ChannelService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Channels are nested under workspaces: /api/workspaces/{wsId}/channels
 */
@RestController
@RequestMapping("/api/workspaces/{wsId}/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService   channelService;
    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChannelResponse>> create(
            @PathVariable UUID wsId,
            @Valid @RequestBody CreateChannelRequest req,
            @CurrentUser UUID userId
    ) {
        Workspace ws = workspaceService.getOrThrow(wsId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(channelService.create(wsId, ws, req, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> listMine(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(channelService.listForUser(wsId, userId)));
    }

    /** Self-service join for public channels. */
    @PostMapping("/{channelId}/join")
    public ResponseEntity<ApiResponse<ChannelResponse>> join(
            @PathVariable UUID wsId,
            @PathVariable UUID channelId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(channelService.join(wsId, channelId, userId)));
    }
}
