package com.communitybot.realtime.config;

import com.communitybot.auth.service.JwtService;
import com.communitybot.auth.service.UserService;
import com.communitybot.realtime.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Intercepts STOMP CONNECT frames to authenticate the WebSocket session.
 * Clients must send the access token in the {@code Authorization} header:
 * <pre>
 *   CONNECT
 *   Authorization: Bearer &lt;token&gt;
 * </pre>
 * On success, the authenticated {@link java.security.Principal} is attached to the
 * STOMP session and available in {@link com.communitybot.realtime.controller.ChatWebSocketController}
 * via {@code SimpMessageHeaderAccessor.getUser()}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService  jwtService;
    private final UserService userService;
    private final PresenceService presenceService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("WS CONNECT without auth header — anonymous session");
            return message;
        }

        String token = authHeader.substring(7);
        if (jwtService.isValid(token) && jwtService.isAccessToken(token)) {
            UUID        userId = jwtService.extractUserId(token);
            UserDetails ud     = userService.loadUserById(userId);
            var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            accessor.setUser(auth);
            presenceService.sessionAuthenticated(accessor.getSessionId(), userId);
            log.debug("WS authenticated: userId={}", userId);
        } else {
            log.debug("WS CONNECT with invalid token — rejecting");
        }

        return message;
    }
}
