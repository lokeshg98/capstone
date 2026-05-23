package com.communitybot.ai.event;

import com.communitybot.ai.agent.MultiAgentService;
import com.communitybot.ai.service.BotUserInitializer;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class BotMentionListener {

    private final MultiAgentService   multiAgentService;
    private final MessageService     messageService;
    private final UserRepository     userRepository;
    private final RealtimePublisher  realtimePublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        if (!event.body().toLowerCase().contains("@bot")) return;

        UUID botId = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL)
                .map(User::getId).orElse(null);
        if (botId != null && botId.equals(event.authorId())) return;

        log.info("Bot mention detected in channel={} message={}", event.channelId(), event.messageId());
        try {
            String question = event.body().replaceAll("(?i)@bot", "").strip();
            if (question.isBlank()) question = "What can you help me with?";

            String answer = multiAgentService
                    .answerSync(event.workspaceId(), event.authorId(), null, question, true)
                    .answer();

            if (botId == null) {
                botId = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL)
                        .orElseThrow(() -> new IllegalStateException("Bot user not found"))
                        .getId();
            }

            MessageResponse botReply = messageService.send(
                    event.channelId(),
                    botId,
                    answer,
                    event.messageId(),
                    null
            );

            realtimePublisher.publishToChannel(event.channelId(), WsOutboundEvent.messageCreated(botReply));
            log.info("Bot reply sent: msgId={}", botReply.id());

        } catch (Exception e) {
            log.error("Bot failed to respond to message {}: {}", event.messageId(), e.getMessage(), e);
        }
    }
}
