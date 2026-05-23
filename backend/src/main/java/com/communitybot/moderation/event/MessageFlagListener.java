package com.communitybot.moderation.event;

import com.communitybot.ai.event.MessageSentEvent;
import com.communitybot.ai.service.BotUserInitializer;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.moderation.service.ContentClassifierService;
import com.communitybot.moderation.service.ContentClassifierService.ClassificationResult;
import com.communitybot.moderation.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Runs the two-layer moderation pipeline after each message is committed:
 * OpenAI Moderation API + custom community guidelines. Flagged messages are
 * hidden from all users and queued for moderator review.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageFlagListener {

    private final ContentClassifierService classifier;
    private final ModerationService        moderationService;
    private final UserRepository           userRepository;

    @Value("${app.moderation.classification-enabled:true}")
    private boolean classificationEnabled;

    @Value("${app.moderation.confidence-threshold:0.7}")
    private double confidenceThreshold;

    /** Cached bot user ID to avoid a DB lookup per message. */
    private volatile UUID botUserId;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        if (!classificationEnabled) return;
        if (isBotUser(event.authorId())) return;

        try {
            ClassificationResult result = classifier.classify(
                    event.body(), event.workspaceId(), event.authorId());

            log.debug("Classification: msgId={} safe={} reason={} confidence={} source={}",
                    event.messageId(), result.safe(), result.flagReason(),
                    result.confidence(), result.source());

            if (!result.safe()) {
                boolean openAiHit = result.source() == ContentClassifierService.ModerationSource.OPENAI;
                if (openAiHit || result.confidence() >= confidenceThreshold) {
                    moderationService.flagMessage(event.messageId(), event.workspaceId(), result);
                }
            }
        } catch (Exception e) {
            log.error("Content classification failed for message {}: {}", event.messageId(), e.getMessage());
        }
    }

    private boolean isBotUser(UUID authorId) {
        if (authorId == null) return false;
        if (botUserId == null) {
            botUserId = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL)
                    .map(User::getId).orElse(null);
        }
        return authorId.equals(botUserId);
    }
}
