package com.communitybot.scheduling.service;

import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.domain.ChannelMember;
import com.communitybot.channel.domain.ChannelType;
import com.communitybot.channel.repository.ChannelMemberRepository;
import com.communitybot.channel.repository.ChannelRepository;
import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import com.communitybot.message.repository.MessageRepository;
import com.communitybot.scheduling.config.N8nProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WeeklyDigestTranscriptBuilder {

    private static final List<MessageStatus> VISIBLE = List.of(
            MessageStatus.ACTIVE,
            MessageStatus.EDITED,
            MessageStatus.FLAGGED
    );

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

    private final MessageRepository messageRepository;
    private final N8nProperties     n8nProperties;

    public record ChannelActivity(Channel channel, String transcript, int messageCount) {}

    public record UserDigestInput(
            UUID userId,
            String displayName,
            List<ChannelActivity> channels,
            int totalMessages
    ) {}

    public UserDigestInput build(UUID userId, String displayName, List<ChannelMember> memberships,
                                 Instant periodStart, Instant periodEnd) {
        StringBuilder full = new StringBuilder();
        int total = 0;
        java.util.ArrayList<ChannelActivity> activities = new java.util.ArrayList<>();

        for (ChannelMember membership : memberships) {
            Channel channel = membership.getChannel();
            List<Message> messages = messageRepository.findByChannelIdAndCreatedAtBetween(
                    channel.getId(),
                    periodStart,
                    periodEnd,
                    VISIBLE,
                    PageRequest.of(0, n8nProperties.getMaxMessagesPerChannel())
            );
            if (messages.isEmpty()) {
                continue;
            }
            StringBuilder channelSb = new StringBuilder();
            for (Message m : messages) {
                appendLine(channelSb, m);
            }
            total += messages.size();
            activities.add(new ChannelActivity(channel, channelSb.toString().trim(), messages.size()));
            full.append("## #").append(channel.getName()).append(" (").append(messages.size()).append(" messages)\n");
            full.append(channelSb).append('\n');
        }

        if (activities.isEmpty()) {
            return null;
        }
        return new UserDigestInput(userId, displayName, activities, total);
    }

    public String formatForLlm(UserDigestInput input, Instant periodStart, Instant periodEnd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Member: ").append(input.displayName()).append('\n');
        sb.append("Period: ")
                .append(DATE.format(periodStart))
                .append(" – ")
                .append(DATE.format(periodEnd))
                .append('\n');
        sb.append("Subscribed channels with activity: ").append(input.channels().size()).append('\n');
        sb.append("Total messages: ").append(input.totalMessages()).append("\n\n");
        for (ChannelActivity activity : input.channels()) {
            sb.append("## #").append(activity.channel().getName())
                    .append(" (").append(activity.messageCount()).append(" messages)\n");
            sb.append(activity.transcript()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static void appendLine(StringBuilder sb, Message m) {
        String name = m.getAuthor().getDisplayName() != null
                ? m.getAuthor().getDisplayName()
                : m.getAuthor().getEmail();
        sb.append('-')
                .append(' ')
                .append(name)
                .append(" [")
                .append(DATE.format(m.getCreatedAt()))
                .append("]: ")
                .append(m.getBody() == null ? "" : m.getBody().trim().replace('\n', ' '))
                .append('\n');
    }
}
