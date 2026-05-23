package com.communitybot.message.event;

import com.communitybot.ai.event.MessageSentEvent;
import com.communitybot.ai.service.BotUserInitializer;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.message.config.ThreadSummaryProperties;
import com.communitybot.message.service.ThreadSummaryService;
import com.communitybot.message.service.ThreadTranscriptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * When a thread reaches 10+ messages, auto-generates an Otter.ai-style digest and posts it as a bot reply.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ThreadSummaryListener {

    private final ThreadSummaryService    threadSummaryService;
    private final ThreadTranscriptBuilder transcriptBuilder;
    private final ThreadSummaryProperties properties;
    private final UserRepository          userRepository;

    private volatile UUID botUserId;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        if (isBotUser(event.authorId())) {
            return;
        }

        UUID rootId = event.threadRootId();
        if (rootId == null) {
            return;
        }

        ThreadTranscriptBuilder.Transcript transcript = transcriptBuilder.build(rootId);
        if (transcript == null || transcript.messageCount() < properties.getMinMessages()) {
            return;
        }

        try {
            threadSummaryService.summarizeIfEligible(rootId);
        } catch (Exception e) {
            log.error("Thread summarization failed for root {}: {}", rootId, e.getMessage(), e);
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
