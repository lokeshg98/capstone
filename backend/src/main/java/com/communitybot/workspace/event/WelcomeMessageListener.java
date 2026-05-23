package com.communitybot.workspace.event;

import com.communitybot.ai.service.BotUserInitializer;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.channel.repository.ChannelRepository;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.service.MessageService;
import com.communitybot.realtime.dto.WsOutboundEvent;
import com.communitybot.realtime.service.RealtimePublisher;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.repository.WorkspaceRepository;
import com.communitybot.workspace.service.WelcomeMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class WelcomeMessageListener {

    private final WorkspaceRepository   wsRepository;
    private final UserRepository        userRepository;
    private final ChannelRepository     channelRepository;
    private final MessageService        messageService;
    private final WelcomeMessageService welcomeMessageService;
    private final RealtimePublisher     realtimePublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserJoined(UserJoinedWorkspaceEvent event) {
        Workspace ws = wsRepository.findById(event.workspaceId()).orElse(null);
        if (ws == null) return;

        User newMember = userRepository.findById(event.userId()).orElse(null);
        if (newMember == null) return;

        User bot = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL).orElse(null);
        if (bot == null) {
            log.warn("Bot user missing — cannot send welcome for {}", event.userId());
            return;
        }

        String body = welcomeMessageService.buildWelcomeMessage(ws, newMember, bot.getId());
        if (body == null || body.isBlank()) return;

        channelRepository.findByWorkspaceIdAndSlug(event.workspaceId(), "general").ifPresent(general -> {
            try {
                MessageResponse msg = messageService.send(general.getId(), bot.getId(), body, null, null);
                realtimePublisher.publishToChannel(general.getId(), WsOutboundEvent.messageCreated(msg));
                log.info("Welcome message sent for user {} in workspace {}", event.userId(), event.workspaceId());
            } catch (Exception e) {
                log.warn("Failed to send welcome message for user {} in workspace {}: {}",
                        event.userId(), event.workspaceId(), e.getMessage());
            }
        });
    }
}
