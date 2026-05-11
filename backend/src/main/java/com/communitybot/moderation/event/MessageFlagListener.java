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
 * Asynchronous listener that runs a content classification on every new message
 * after the transaction commits. Creates a {@link com.communitybot.moderation.domain.ModerationFlag}
 * when the classifier scores the message above the configured confidence threshold.
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
        if (isBotUser(event.authorId()))   return;
        if (event.body().length() < 10)    return;   // too short to classify meaningfully

        try {
            ClassificationResult result = classifier.classify(event.body());
            log.debug("Classification: msgId={} safe={} reason={} confidence={:.2f}",
                    event.messageId(), result.safe(), result.flagReason(), result.confidence());

            if (!result.safe() && result.confidence() >= confidenceThreshold) {
                moderationService.flagMessage(event.messageId(), event.workspaceId(), result);
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
