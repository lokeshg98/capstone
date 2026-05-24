package com.communitybot.ai.event;

import com.communitybot.ai.agent.MultiAgentService;
import com.communitybot.ai.service.BotUserInitializer;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.channel.service.ChannelService;
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
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class BotMentionListener {

    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "(?i)@bot\\b|@" + Pattern.quote(normalizeName(BotUserInitializer.BOT_DISPLAY_NAME)) + "[\\p{So}]*\\s*"
    );

    private final MultiAgentService   multiAgentService;
    private final MessageService     messageService;
    private final UserRepository     userRepository;
    private final RealtimePublisher  realtimePublisher;
    private final ChannelService     channelService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        log.debug("BotMentionListener triggered: body='{}' threadRootId={}", event.body(), event.threadRootId());
        if (!MENTION_PATTERN.matcher(event.body()).find()) return;

        UUID botId = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL)
                .map(User::getId).orElse(null);
        if (botId != null && botId.equals(event.authorId())) return;

        log.info("Bot mention detected in channel={} message={}", event.channelId(), event.messageId());
        try {
            String question = MENTION_PATTERN.matcher(event.body()).replaceAll("").strip();
            if (question.isBlank()) question = "What can you help me with?";

            String answer = multiAgentService
                    .answerSync(event.workspaceId(), event.authorId(), null, question, true)
                    .answer();

            if (botId == null) {
                botId = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL)
                        .orElseThrow(() -> new IllegalStateException("Bot user not found"))
                        .getId();
            }

            channelService.ensureBotInChannel(event.channelId());

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

    private static String normalizeName(String displayName) {
        return displayName.replaceAll("[^\\p{ASCII}]", "").strip();
    }
}
