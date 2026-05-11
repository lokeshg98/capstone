package com.communitybot.workspace.service;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.domain.WorkspaceBan;
import com.communitybot.workspace.repository.WorkspaceBanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages workspace-scoped user bans.
 * All permission checks (moderator/admin role) are delegated to {@link WorkspaceService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BanService {

    private final WorkspaceBanRepository banRepository;
    private final WorkspaceService       workspaceService;
    private final UserService            userService;

    /** Throws {@link ErrorCode#USER_BANNED} if the user has an active ban in this workspace. */
    public void requireNotBanned(UUID wsId, UUID userId) {
        if (banRepository.isUserBanned(wsId, userId)) {
            throw new AppException(ErrorCode.USER_BANNED);
        }
    }

    @Transactional
    public BanResponse ban(UUID wsId, UUID targetUserId, String reason, Instant expiresAt, UUID requesterId) {
        workspaceService.requireModeratorOrAdmin(wsId, requesterId);
        Workspace workspace = workspaceService.getOrThrow(wsId);
        User      target    = userService.getOrThrow(targetUserId);

        // Lift any existing active ban first to maintain the uniqueness constraint
        banRepository.findActiveBan(wsId, targetUserId).ifPresent(b -> { b.lift(); banRepository.save(b); });

        WorkspaceBan ban = banRepository.save(
                WorkspaceBan.builder()
                        .workspace(workspace)
                        .user(target)
                        .bannedBy(requesterId)
                        .reason(reason)
                        .expiresAt(expiresAt)
                        .build()
        );
        log.info("User {} banned from workspace {} by {} (expires={})", targetUserId, wsId, requesterId, expiresAt);
        return BanResponse.from(ban);
    }

    @Transactional
    public void liftBan(UUID wsId, UUID targetUserId, UUID requesterId) {
        workspaceService.requireModeratorOrAdmin(wsId, requesterId);
        WorkspaceBan ban = banRepository.findActiveBan(wsId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.BAN_NOT_FOUND));
        ban.lift();
        log.info("Ban lifted for user {} in workspace {} by {}", targetUserId, wsId, requesterId);
    }

    @Transactional(readOnly = true)
    public List<BanResponse> listActiveBans(UUID wsId, UUID requesterId) {
        workspaceService.requireModeratorOrAdmin(wsId, requesterId);
        return banRepository.findAllByWorkspaceIdAndActiveTrueOrderByCreatedAtDesc(wsId)
                .stream()
                .filter(WorkspaceBan::isEffectivelyActive)
                .map(BanResponse::from)
                .toList();
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public record BanResponse(
            UUID    id,
            UUID    workspaceId,
            UUID    userId,
            String  userDisplayName,
            String  reason,
            Instant expiresAt,
            Instant bannedAt
    ) {
        public static BanResponse from(WorkspaceBan b) {
            return new BanResponse(
                    b.getId(),
                    b.getWorkspace().getId(),
                    b.getUser().getId(),
                    b.getUser().getDisplayName(),
                    b.getReason(),
                    b.getExpiresAt(),
                    b.getCreatedAt()
            );
        }
    }
}
