package com.communitybot.ai.event;

import com.communitybot.ai.service.BotUserInitializer;
import com.communitybot.ai.service.RagService;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.service.MessageService;
import com.communitybot.realtime.dto.WsOutboundEvent;
import com.communitybot.realtime.service.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Listens for {@link MessageSentEvent} after a transaction commits and,
 * when the message body contains {@code @bot}, triggers the RAG pipeline
 * and posts the answer as a thread reply authored by the Bot user.
 *
 * <p>{@code @Async} ensures the bot's call to OpenAI does not block
 * the user's original HTTP/WS response.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BotMentionListener {

    private final RagService         ragService;
    private final MessageService     messageService;
    private final UserRepository     userRepository;
    private final RealtimePublisher  realtimePublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        if (!event.body().toLowerCase().contains("@bot")) return;

        // Prevent the bot from triggering itself in an infinite loop
        UUID botId = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL)
                .map(User::getId).orElse(null);
        if (botId != null && botId.equals(event.authorId())) return;

        log.info("Bot mention detected in channel={} message={}", event.channelId(), event.messageId());
        try {
            String question = event.body().replaceAll("(?i)@bot", "").strip();
            if (question.isBlank()) question = "What can you help me with?";

            String answer = ragService.ask(event.workspaceId(), question).answer();

            if (botId == null) {
                botId = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL)
                        .orElseThrow(() -> new IllegalStateException("Bot user not found"))
                        .getId();
            }

            MessageResponse botReply = messageService.send(
                    event.channelId(),
                    botId,
                    answer,
                    event.messageId(),   // reply in-thread
                    null                 // no attachment
            );

            realtimePublisher.publishToChannel(event.channelId(), WsOutboundEvent.messageCreated(botReply));
            log.info("Bot reply sent: msgId={}", botReply.id());

        } catch (Exception e) {
            log.error("Bot failed to respond to message {}: {}", event.messageId(), e.getMessage(), e);
        }
    }
}
