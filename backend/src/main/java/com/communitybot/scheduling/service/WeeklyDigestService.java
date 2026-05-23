package com.communitybot.scheduling.service;

import com.communitybot.ai.config.OpenAiProperties;
import com.communitybot.ai.service.BotUserInitializer;
import com.communitybot.ai.usage.LlmUsageCategory;
import com.communitybot.ai.usage.LlmUsageService;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.domain.ChannelMember;
import com.communitybot.channel.domain.ChannelType;
import com.communitybot.channel.repository.ChannelMemberRepository;
import com.communitybot.channel.repository.ChannelRepository;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.service.MessageService;
import com.communitybot.realtime.dto.WsOutboundEvent;
import com.communitybot.realtime.service.RealtimePublisher;
import com.communitybot.scheduling.config.N8nProperties;
import com.communitybot.scheduling.domain.UserDigestPreferences;
import com.communitybot.scheduling.domain.WeeklyDigestRun;
import com.communitybot.scheduling.dto.WeeklyDigestRunResponse;
import com.communitybot.scheduling.repository.UserDigestPreferencesRepository;
import com.communitybot.scheduling.repository.WeeklyDigestRunRepository;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.domain.WorkspaceMember;
import com.communitybot.workspace.repository.WorkspaceMemberRepository;
import com.communitybot.workspace.repository.WorkspaceRepository;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates personalized weekly digests for workspace members across all channels
 * they are subscribed to ({@code channel_members}). Triggered by n8n on a cron such as
 * "every Friday at 5pm".
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyDigestService {

    private static final String DIGEST_PROMPT = """
            You are a community assistant like Otter.ai. Create a concise weekly digest
            for a member summarizing activity in the channels they follow.

            Use markdown with these sections:
            ## Highlights
            (2–4 bullets of the most important cross-channel themes)

            ## By channel
            (For each channel with activity, 2–3 bullets of key discussions)

            ## Action items & follow-ups
            (Bullets with owners when clear, or "None this week")

            ## Open questions
            (Bullets or "None")

            Be faithful to the transcript only. Keep the whole digest under 400 words.

            Transcript:
            """;

    public static final String WEEKLY_DIGEST_SLUG = "weekly-digest";

    private static final DateTimeFormatter RANGE =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    private final WorkspaceRepository              workspaceRepository;
    private final WorkspaceMemberRepository        workspaceMemberRepository;
    private final ChannelRepository                channelRepository;
    private final ChannelMemberRepository          channelMemberRepository;
    private final UserRepository                   userRepository;
    private final UserDigestPreferencesRepository  digestPreferencesRepository;
    private final WeeklyDigestRunRepository      digestRunRepository;
    private final WeeklyDigestTranscriptBuilder  transcriptBuilder;
    private final MessageService                   messageService;
    private final RealtimePublisher                realtimePublisher;
    private final ChatModel                        chatModel;
    private final LlmUsageService                  llmUsageService;
    private final OpenAiProperties                 openAiProperties;
    private final N8nProperties                    n8nProperties;

    /**
     * Runs weekly digests for one workspace or all workspaces.
     * Idempotent per user + period window.
     */
    @Transactional
    public WeeklyDigestRunResponse runWeeklyDigests(UUID workspaceId, int periodDays, boolean dryRun) {
        Instant periodEnd   = Instant.now();
        Instant periodStart = periodEnd.minusSeconds((long) periodDays * 86400L);

        List<Workspace> workspaces = workspaceId == null
                ? workspaceRepository.findAll()
                : List.of(workspaceRepository.findById(workspaceId)
                        .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId)));

        int usersProcessed = 0;
        int digestsSent    = 0;
        int skipped        = 0;
        List<String> errors = new ArrayList<>();

        User bot = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL).orElse(null);
        if (bot == null) {
            return new WeeklyDigestRunResponse(0, 0, 0, List.of("Bot user missing — restart backend"));
        }

        for (Workspace workspace : workspaces) {
            List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspace.getId());
            Channel digestChannel = dryRun ? null : getOrCreateWeeklyDigestChannel(workspace, bot);

            for (WorkspaceMember member : members) {
                usersProcessed++;
                User user = member.getUser();
                if (user.getEmail().equals(BotUserInitializer.BOT_EMAIL)) {
                    skipped++;
                    continue;
                }
                if (!isDigestEnabled(user.getId())) {
                    skipped++;
                    continue;
                }

                if (digestRunRepository.existsByWorkspaceIdAndUserIdAndPeriodStartAndPeriodEnd(
                        workspace.getId(), user.getId(), periodStart, periodEnd)) {
                    skipped++;
                    continue;
                }

                List<ChannelMember> memberships =
                        channelMemberRepository.findActiveMemberships(user.getId(), workspace.getId());
                if (memberships.isEmpty()) {
                    skipped++;
                    continue;
                }

                String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
                WeeklyDigestTranscriptBuilder.UserDigestInput input = transcriptBuilder.build(
                        user.getId(), displayName, memberships, periodStart, periodEnd);
                if (input == null) {
                    skipped++;
                    continue;
                }

                try {
                    String llmBody = generateDigestBody(input, periodStart, periodEnd, bot.getId());
                    String postedBody = formatPostedMessage(displayName, periodStart, periodEnd, llmBody, input);

                    if (dryRun) {
                        digestsSent++;
                        continue;
                    }

                    ensureChannelMember(digestChannel, user);
                    MessageResponse msg = messageService.send(
                            digestChannel.getId(),
                            bot.getId(),
                            postedBody,
                            null,
                            null
                    );
                    realtimePublisher.publishToChannel(
                            digestChannel.getId(), WsOutboundEvent.messageCreated(msg));

                    digestRunRepository.save(WeeklyDigestRun.builder()
                            .workspaceId(workspace.getId())
                            .userId(user.getId())
                            .periodStart(periodStart)
                            .periodEnd(periodEnd)
                            .channelId(digestChannel.getId())
                            .messageId(msg.id())
                            .createdAt(Instant.now())
                            .build());
                    digestsSent++;
                    log.info("Weekly digest posted for user {} in workspace {}", user.getId(), workspace.getId());
                } catch (DataIntegrityViolationException e) {
                    skipped++;
                } catch (Exception e) {
                    log.error("Weekly digest failed for user {} workspace {}: {}",
                            user.getId(), workspace.getId(), e.getMessage(), e);
                    errors.add(user.getId() + ": " + e.getMessage());
                }
            }
        }

        return new WeeklyDigestRunResponse(usersProcessed, digestsSent, skipped, errors);
    }

    @Transactional(readOnly = true)
    public boolean isDigestEnabled(UUID userId) {
        return digestPreferencesRepository.findById(userId)
                .map(UserDigestPreferences::isWeeklyDigestEnabled)
                .orElse(true);
    }

    @Transactional
    public UserDigestPreferences getOrCreatePreferences(UUID userId) {
        return digestPreferencesRepository.findById(userId)
                .orElseGet(() -> digestPreferencesRepository.save(
                        UserDigestPreferences.builder()
                                .userId(userId)
                                .weeklyDigestEnabled(true)
                                .updatedAt(Instant.now())
                                .build()
                ));
    }

    @Transactional
    public UserDigestPreferences updatePreferences(UUID userId, boolean weeklyDigestEnabled) {
        UserDigestPreferences prefs = getOrCreatePreferences(userId);
        prefs.setWeeklyDigestEnabled(weeklyDigestEnabled);
        return digestPreferencesRepository.save(prefs);
    }

    private String generateDigestBody(
            WeeklyDigestTranscriptBuilder.UserDigestInput input,
            Instant periodStart,
            Instant periodEnd,
            UUID botUserId
    ) {
        String transcript = transcriptBuilder.formatForLlm(input, periodStart, periodEnd);
        ChatResponse response = chatModel.chat(UserMessage.from(DIGEST_PROMPT + transcript));
        if (response.tokenUsage() != null) {
            llmUsageService.record(
                    botUserId,
                    LlmUsageCategory.WEEKLY_DIGEST,
                    openAiProperties.getChatModel(),
                    nullSafe(response.tokenUsage().inputTokenCount()),
                    nullSafe(response.tokenUsage().outputTokenCount())
            );
        }
        return response.aiMessage().text();
    }

    private static String formatPostedMessage(
            String displayName,
            Instant periodStart,
            Instant periodEnd,
            String digestBody,
            WeeklyDigestTranscriptBuilder.UserDigestInput input
    ) {
        String channelList = input.channels().stream()
                .map(a -> "#" + a.channel().getName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return """
                📅 **Weekly Digest** for **%s**
                _%s – %s · %d messages across %s_

                %s
                """.formatted(
                displayName,
                RANGE.format(periodStart),
                RANGE.format(periodEnd),
                input.totalMessages(),
                channelList,
                digestBody
        ).trim();
    }

    private Channel getOrCreateWeeklyDigestChannel(Workspace workspace, User bot) {
        return channelRepository.findByWorkspaceIdAndSlug(workspace.getId(), WEEKLY_DIGEST_SLUG)
                .orElseGet(() -> {
                    Channel channel = channelRepository.save(
                            Channel.builder()
                                    .workspace(workspace)
                                    .name("weekly-digest")
                                    .slug(WEEKLY_DIGEST_SLUG)
                                    .type(ChannelType.PUBLIC)
                                    .description("Automated weekly digests from n8n (Friday 5pm)")
                                    .createdBy(bot)
                                    .build()
                    );
                    channelMemberRepository.save(ChannelMember.builder().channel(channel).user(bot).build());
                    return channel;
                });
    }

    private void ensureChannelMember(Channel channel, User user) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), user.getId())) {
            channelMemberRepository.save(ChannelMember.builder().channel(channel).user(user).build());
        }
    }

    private static int nullSafe(Integer n) {
        return n == null ? 0 : n;
    }
}
