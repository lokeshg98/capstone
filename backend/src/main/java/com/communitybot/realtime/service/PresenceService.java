package com.communitybot.realtime.service;

import com.communitybot.realtime.dto.WsOutboundEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users are online in which workspaces, and which STOMP sessions
 * belong to which users. Uses in-memory maps — presence is ephemeral.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PresenceService {

    /** sessionId → userId */
    private final Map<String, UUID> sessionUsers = new ConcurrentHashMap<>();
    /** userId → set of workspaceIds the user is online in */
    private final Map<UUID, Set<UUID>> userWorkspaces = new ConcurrentHashMap<>();

    private final RealtimePublisher publisher;

    public void sessionAuthenticated(String sessionId, UUID userId) {
        sessionUsers.put(sessionId, userId);
    }

    public void sessionDisconnected(String sessionId) {
        UUID userId = sessionUsers.remove(sessionId);
        if (userId == null) return;
        Set<UUID> wsIds = userWorkspaces.remove(userId);
        if (wsIds == null) return;
        for (UUID wsId : wsIds) {
            publisher.publishPresenceUpdate(wsId, userId, false);
        }
    }

    public void userJoinedWorkspace(UUID wsId, UUID userId) {
        userWorkspaces.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(wsId);
        publisher.publishPresenceUpdate(wsId, userId, true);
    }

    public void userLeftWorkspace(UUID wsId, UUID userId) {
        Set<UUID> wsIds = userWorkspaces.get(userId);
        if (wsIds != null) {
            wsIds.remove(wsId);
            if (wsIds.isEmpty()) userWorkspaces.remove(userId);
        }
        publisher.publishPresenceUpdate(wsId, userId, false);
    }

    public boolean isOnline(UUID userId) {
        return userWorkspaces.containsKey(userId);
    }

    public Set<UUID> onlineInWorkspace(UUID wsId) {
        return userWorkspaces.entrySet().stream()
                .filter(e -> e.getValue().contains(wsId))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }
}
