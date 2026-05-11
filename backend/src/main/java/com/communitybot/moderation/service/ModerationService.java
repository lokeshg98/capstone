package com.communitybot.moderation.service;

import com.communitybot.message.domain.Message;
import com.communitybot.message.repository.MessageRepository;
import com.communitybot.moderation.domain.FlagStatus;
import com.communitybot.moderation.domain.ModerationFlag;
import com.communitybot.moderation.dto.ModerationFlagResponse;
import com.communitybot.moderation.repository.ModerationFlagRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationFlagRepository flagRepository;
    private final MessageRepository        messageRepository;
    private final WorkspaceService         workspaceService;

    /** Creates a PENDING flag for a message. Called by {@link com.communitybot.moderation.event.MessageFlagListener}. */
    @Transactional
    public void flagMessage(UUID messageId, UUID workspaceId, ContentClassifierService.ClassificationResult result) {
        // Guard: don't double-flag the same message
        if (flagRepository.existsByMessageId(messageId)) return;

        Message msg = messageRepository.findById(messageId).orElse(null);
        if (msg == null) return;

        msg.flag();

        flagRepository.save(
                ModerationFlag.builder()
                        .message(msg)
                        .workspaceId(workspaceId)
                        .llmReason(result.flagReason())
                        .llmExplanation(result.explanation())
                        .llmConfidence(result.confidence())
                        .build()
        );
        log.info("Message {} flagged: reason={} confidence={:.2f}", messageId, result.flagReason(), result.confidence());
    }

    @Transactional(readOnly = true)
    public List<ModerationFlagResponse> listFlags(UUID wsId, String statusFilter, UUID requesterId) {
        workspaceService.requireModeratorOrAdmin(wsId, requesterId);
        List<ModerationFlag> flags = (statusFilter != null)
                ? flagRepository.findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(wsId, FlagStatus.valueOf(statusFilter))
                : flagRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(wsId);
        return flags.stream().map(ModerationFlagResponse::from).toList();
    }

    @Transactional
    public ModerationFlagResponse approve(UUID flagId, UUID reviewerId) {
        ModerationFlag flag = getOrThrow(flagId);
        workspaceService.requireModeratorOrAdmin(flag.getWorkspaceId(), reviewerId);
        flag.approve(reviewerId);
        return ModerationFlagResponse.from(flag);
    }

    @Transactional
    public ModerationFlagResponse remove(UUID flagId, UUID reviewerId) {
        ModerationFlag flag = getOrThrow(flagId);
        workspaceService.requireModeratorOrAdmin(flag.getWorkspaceId(), reviewerId);
        flag.remove(reviewerId);
        return ModerationFlagResponse.from(flag);
    }

    // -------------------------------------------------------------------------

    private ModerationFlag getOrThrow(UUID flagId) {
        return flagRepository.findById(flagId)
                .orElseThrow(() -> new AppException(ErrorCode.MODERATION_FLAG_NOT_FOUND));
    }
}
