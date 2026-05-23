package com.communitybot.moderation.service;

import com.communitybot.message.domain.Message;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.repository.MessageRepository;
import com.communitybot.message.repository.ReactionRepository;
import com.communitybot.moderation.domain.FlagStatus;
import com.communitybot.moderation.domain.ModerationFlag;
import com.communitybot.moderation.dto.ModerationFlagResponse;
import com.communitybot.moderation.repository.ModerationFlagRepository;
import com.communitybot.realtime.dto.WsOutboundEvent;
import com.communitybot.realtime.service.RealtimePublisher;
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
    private final ReactionRepository       reactionRepository;
    private final WorkspaceService         workspaceService;
    private final RealtimePublisher        realtimePublisher;

    /**
     * Flags a message, hides it from all channel members, and broadcasts the update.
     */
    @Transactional
    public void flagMessage(UUID messageId, UUID workspaceId, ContentClassifierService.ClassificationResult result) {
        if (flagRepository.existsByMessageId(messageId)) return;

        Message msg = messageRepository.findById(messageId).orElse(null);
        if (msg == null) return;

        msg.hide();

        flagRepository.save(
                ModerationFlag.builder()
                        .message(msg)
                        .workspaceId(workspaceId)
                        .llmReason(result.flagReason())
                        .llmExplanation(result.explanation())
                        .llmConfidence(result.confidence())
                        .build()
        );

        publishMessageUpdate(msg);
        log.info("Message {} hidden and flagged: reason={} confidence={:.2f} source={}",
                messageId, result.flagReason(), result.confidence(), result.source());
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
        publishMessageUpdate(flag.getMessage());
        return ModerationFlagResponse.from(flag);
    }

    @Transactional
    public ModerationFlagResponse remove(UUID flagId, UUID reviewerId) {
        ModerationFlag flag = getOrThrow(flagId);
        workspaceService.requireModeratorOrAdmin(flag.getWorkspaceId(), reviewerId);
        flag.remove(reviewerId);
        publishMessageUpdate(flag.getMessage());
        return ModerationFlagResponse.from(flag);
    }

    private void publishMessageUpdate(Message msg) {
        List<com.communitybot.message.domain.Reaction> reactions =
                reactionRepository.findAllByMessageIdIn(List.of(msg.getId()));
        MessageResponse response = MessageResponse.from(msg, reactions, null);
        realtimePublisher.publishToChannel(msg.getChannel().getId(), WsOutboundEvent.messageUpdated(response));
    }

    private ModerationFlag getOrThrow(UUID flagId) {
        return flagRepository.findById(flagId)
                .orElseThrow(() -> new AppException(ErrorCode.MODERATION_FLAG_NOT_FOUND));
    }
}
