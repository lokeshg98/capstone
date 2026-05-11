package com.communitybot.moderation.controller;

import com.communitybot.moderation.dto.ModerationFlagResponse;
import com.communitybot.moderation.service.ModerationService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import com.communitybot.workspace.service.BanService;
import com.communitybot.workspace.service.BanService.BanResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Moderation endpoints — all require the calling user to be
 * a workspace MODERATOR or ADMIN (enforced in service layer).
 */
@RestController
@RequestMapping("/api/workspaces/{wsId}/moderation")
@RequiredArgsConstructor
@Validated
public class ModerationController {

    private final ModerationService moderationService;
    private final BanService        banService;

    // ── Flagged messages ──────────────────────────────────────────────────────

    /**
     * Lists flagged messages.
     * @param status Optional filter: PENDING | APPROVED | REMOVED (omit for all)
     */
    @GetMapping("/flags")
    public ResponseEntity<ApiResponse<List<ModerationFlagResponse>>> listFlags(
            @PathVariable UUID wsId,
            @RequestParam(required = false) String status,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                moderationService.listFlags(wsId, status, userId)));
    }

    /** Restores the message and marks the flag as APPROVED (false positive). */
    @PostMapping("/flags/{flagId}/approve")
    public ResponseEntity<ApiResponse<ModerationFlagResponse>> approve(
            @PathVariable UUID wsId,
            @PathVariable UUID flagId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.approve(flagId, userId)));
    }

    /** Hides the message permanently and marks the flag as REMOVED (confirmed violation). */
    @PostMapping("/flags/{flagId}/remove")
    public ResponseEntity<ApiResponse<ModerationFlagResponse>> remove(
            @PathVariable UUID wsId,
            @PathVariable UUID flagId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.remove(flagId, userId)));
    }

    // ── Bans ──────────────────────────────────────────────────────────────────

    @GetMapping("/bans")
    public ResponseEntity<ApiResponse<List<BanResponse>>> listBans(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(banService.listActiveBans(wsId, userId)));
    }

    @PostMapping("/bans")
    public ResponseEntity<ApiResponse<BanResponse>> ban(
            @PathVariable UUID wsId,
            @RequestBody BanRequest req,
            @CurrentUser UUID userId
    ) {
        BanResponse response = banService.ban(wsId, req.userId(), req.reason(), req.expiresAt(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/bans/{targetUserId}")
    public ResponseEntity<Void> liftBan(
            @PathVariable UUID wsId,
            @PathVariable UUID targetUserId,
            @CurrentUser UUID userId
    ) {
        banService.liftBan(wsId, targetUserId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Request DTO ───────────────────────────────────────────────────────────

    public record BanRequest(
            UUID    userId,
            @NotBlank String reason,
            Instant expiresAt   // null = permanent
    ) {}
}
