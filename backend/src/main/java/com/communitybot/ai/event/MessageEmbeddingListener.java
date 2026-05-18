package com.communitybot.ai.event;

import com.communitybot.message.service.MessageEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Indexes message bodies for workspace-wide semantic search (async, after commit).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageEmbeddingListener {

    private final MessageEmbeddingService messageEmbeddingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        try {
            messageEmbeddingService.indexMessage(event.messageId(), event.workspaceId());
        } catch (Exception e) {
            log.debug("Message embedding skipped for {}: {}", event.messageId(), e.getMessage());
        }
    }
}
