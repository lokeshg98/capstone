package com.communitybot.message.service;

import com.communitybot.ai.event.MessageSentEvent;
import com.communitybot.attachment.domain.Attachment;
import com.communitybot.attachment.repository.AttachmentRepository;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.service.ChannelService;
import com.communitybot.workspace.service.BanService;
import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import com.communitybot.message.domain.Reaction;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.dto.SendMessageRequest;
import com.communitybot.message.repository.MessageRepository;
import com.communitybot.message.repository.ReactionRepository;
import com.communitybot.shared.dto.PageResponse;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final List<MessageStatus> HIDDEN_STATUSES =
            List.of(MessageStatus.DELETED, MessageStatus.HIDDEN);

    private final MessageRepository         messageRepository;
    private final ReactionRepository        reactionRepository;
    private final AttachmentRepository      attachmentRepository;
    private final ChannelService            channelService;
    private final UserService               userService;
    private final BanService                banService;
    private final ApplicationEventPublisher eventPublisher;

    /** Sends a new message and returns the persisted DTO (used by both HTTP and WS paths). */
    @Transactional
    public MessageResponse send(UUID channelId, UUID authorId, String body, UUID threadRootId, UUID attachmentId) {
        channelService.requireChannelMember(channelId, authorId);
        Channel channel = channelService.getOrThrow(channelId);
        banService.requireNotBanned(channel.getWorkspace().getId(), authorId);
        User    author  = userService.getOrThrow(authorId);

        Message threadRoot = null;
        if (threadRootId != null) {
            threadRoot = messageRepository.findById(threadRootId)
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        }

        Attachment attachment = null;
        if (attachmentId != null) {
            attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        }

        Message saved = messageRepository.save(
                Message.builder()
                        .channel(channel)
                        .workspaceId(channel.getWorkspace().getId())
                        .author(author)
                        .body(body)
                        .threadRoot(threadRoot)
                        .attachment(attachment)
                        .build()
        );

        // Publish event for bot listener + moderation classifier (both run async after commit)
        eventPublisher.publishEvent(
                new MessageSentEvent(saved.getId(), channelId, channel.getWorkspace().getId(), body, authorId));

        return MessageResponse.from(saved, List.of(), authorId);
    }

    /** Paginated top-level messages (oldest first, page 0 = oldest page). */
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> loadPage(UUID channelId, UUID requesterId, int page, int size) {
        channelService.requireChannelMember(channelId, requesterId);

        Page<Message> msgPage = messageRepository.findTopLevel(
                channelId, HIDDEN_STATUSES, PageRequest.of(page, size));

        List<UUID> ids = msgPage.map(Message::getId).toList();
        Map<UUID, List<Reaction>> byMsg = reactionRepository.findAllByMessageIdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getMessage().getId()));

        Page<MessageResponse> responsePage = msgPage.map(
                m -> MessageResponse.from(m, byMsg.getOrDefault(m.getId(), List.of()), requesterId)
        );
        return PageResponse.from(responsePage);
    }

    /** Soft-delete a message (author or workspace moderator). */
    @Transactional
    public void delete(UUID channelId, UUID messageId, UUID requesterId) {
        channelService.requireChannelMember(channelId, requesterId);
        Message msg = getOrThrow(messageId);
        if (!msg.getAuthor().getId().equals(requesterId)) {
            // TODO: also allow workspace moderators once roles are propagated to this layer
            throw new AppException(ErrorCode.MESSAGE_FORBIDDEN);
        }
        msg.delete();
    }

    @Transactional
    public MessageResponse addReaction(UUID messageId, String emoji, UUID userId) {
        Message msg = getOrThrow(messageId);
        channelService.requireChannelMember(msg.getChannel().getId(), userId);

        if (!reactionRepository.existsByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)) {
            User user = userService.getOrThrow(userId);
            reactionRepository.save(
                    Reaction.builder().message(msg).user(user).emoji(emoji).build()
            );
        }

        List<Reaction> reactions = reactionRepository.findAllByMessageIdIn(List.of(messageId));
        return MessageResponse.from(msg, reactions, userId);
    }

    @Transactional
    public MessageResponse removeReaction(UUID messageId, String emoji, UUID userId) {
        Message msg = getOrThrow(messageId);
        channelService.requireChannelMember(msg.getChannel().getId(), userId);
        reactionRepository.deleteByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        List<Reaction> reactions = reactionRepository.findAllByMessageIdIn(List.of(messageId));
        return MessageResponse.from(msg, reactions, userId);
    }

    // -------------------------------------------------------------------------

    public Message getOrThrow(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
    }
}
