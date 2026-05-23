package com.communitybot.ai.event;

import java.util.UUID;

/**
 * Published by {@link com.communitybot.message.service.MessageService} after a message
 * is saved and committed.
 * Consumed by {@link BotMentionListener} (RAG reply) and
 * {@link com.communitybot.moderation.event.MessageFlagListener} (content moderation).
 */
public record MessageSentEvent(
        UUID   messageId,
        UUID   channelId,
        UUID   workspaceId,
        String body,
        UUID   authorId,
        UUID   threadRootId
) {}
