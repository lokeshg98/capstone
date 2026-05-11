package com.communitybot.message.dto;

import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import com.communitybot.message.domain.Reaction;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record MessageResponse(
        UUID              id,
        UUID              channelId,
        UUID              workspaceId,
        AuthorInfo        author,
        String            body,
        UUID              threadRootId,
        MessageStatus     status,
        boolean           edited,
        Instant           editedAt,
        Instant           createdAt,
        List<ReactionSummary> reactions,
        AttachmentInfo    attachment     // null for text-only messages
) {
    public record AuthorInfo(UUID id, String displayName, String avatarUrl) {}

    public record ReactionSummary(String emoji, int count, boolean reactedByMe) {}

    public record AttachmentInfo(UUID id, String filename, String mimeType, String kind, long sizeBytes) {}

    /** Builds a response for a single message with pre-loaded reactions. */
    public static MessageResponse from(Message msg, List<Reaction> reactions, UUID requesterId) {
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        List<ReactionSummary> summaries = grouped.entrySet().stream()
                .map(e -> new ReactionSummary(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().anyMatch(r -> r.getUser().getId().equals(requesterId))
                ))
                .toList();

        AttachmentInfo attachmentInfo = null;
        if (msg.getAttachment() != null) {
            var a = msg.getAttachment();
            attachmentInfo = new AttachmentInfo(
                    a.getId(), a.getFilename(), a.getMimeType(), a.getKind().name(), a.getSizeBytes());
        }

        return new MessageResponse(
                msg.getId(),
                msg.getChannel().getId(),
                msg.getWorkspaceId(),
                new AuthorInfo(
                        msg.getAuthor().getId(),
                        msg.getAuthor().getDisplayName(),
                        msg.getAuthor().getAvatarUrl()
                ),
                msg.getBody(),
                msg.getThreadRoot() != null ? msg.getThreadRoot().getId() : null,
                msg.getStatus(),
                msg.getEditedAt() != null,
                msg.getEditedAt(),
                msg.getCreatedAt(),
                summaries,
                attachmentInfo
        );
    }
}
