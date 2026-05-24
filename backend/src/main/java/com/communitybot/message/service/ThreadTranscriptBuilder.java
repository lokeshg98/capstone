package com.communitybot.message.service;

import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import com.communitybot.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ThreadTranscriptBuilder {

    private static final List<MessageStatus> EXCLUDED = List.of(
            MessageStatus.DELETED,
            MessageStatus.HIDDEN,
            MessageStatus.FLAGGED
    );

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final MessageRepository messageRepository;

    public record Transcript(UUID rootId, UUID channelId, UUID workspaceId, String text, int messageCount) {}

    @Transactional(readOnly = true)
    public Transcript build(UUID threadRootId) {
        Message root = messageRepository.findById(threadRootId).orElse(null);
        if (root == null) {
            return null;
        }
        List<Message> replies = messageRepository.findThreadReplies(threadRootId, EXCLUDED);
        StringBuilder sb = new StringBuilder();
        appendLine(sb, root);
        for (Message reply : replies) {
            appendLine(sb, reply);
        }
        int count = 1 + replies.size();
        return new Transcript(
                root.getId(),
                root.getChannel().getId(),
                root.getWorkspaceId(),
                sb.toString().trim(),
                count
        );
    }

    private static void appendLine(StringBuilder sb, Message m) {
        String name = m.getAuthor().getDisplayName() != null
                ? m.getAuthor().getDisplayName()
                : m.getAuthor().getEmail();
        sb.append('[')
                .append(name)
                .append(' ')
                .append(TIME.format(m.getCreatedAt()))
                .append("] ")
                .append(m.getBody() == null ? "" : m.getBody().trim())
                .append('\n');
    }
}
