package com.communitybot.message.service;

import com.communitybot.ai.config.OpenAiProperties;
import com.communitybot.ai.service.BotUserInitializer;
import com.communitybot.ai.usage.LlmUsageCategory;
import com.communitybot.ai.usage.LlmUsageService;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.channel.service.ChannelService;
import com.communitybot.message.domain.ThreadSummary;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.repository.ThreadSummaryRepository;
import com.communitybot.realtime.dto.WsOutboundEvent;
import com.communitybot.realtime.service.RealtimePublisher;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Otter.ai-style thread digests: auto-generated when threads reach {@code minMessages}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ThreadSummaryService {

    private static final String OTTER_PROMPT = """
            You are an AI meeting assistant like Otter.ai. Summarize the following chat thread
            into a digestible overview for busy community members.

            Use exactly these markdown sections (keep each section concise):
            ## Overview
            (2–3 sentence TL;DR of the whole discussion)

            ## Key points
            - (bullet list of important topics raised)

            ## Decisions made
            - (bullet list, or a single bullet "None noted")

            ## Action items
            - (bullet list with owner names when clear, or "None")

            ## Open questions
            - (bullet list, or "None")

            Attribute statements to speaker names from the transcript when helpful.
            Be faithful to the transcript only — do not invent facts.

            Transcript:
            """;

    private final ThreadTranscriptBuilder  transcriptBuilder;
    private final ThreadSummaryRepository  summaryRepository;
    private final MessageService           messageService;
    private final UserRepository           userRepository;
    private final RealtimePublisher        realtimePublisher;
    private final ChatModel                chatModel;
    private final LlmUsageService          llmUsageService;
    private final OpenAiProperties         openAiProperties;
    private final ChannelService           channelService;
    private final com.communitybot.message.config.ThreadSummaryProperties properties;

    @Transactional(readOnly = true)
    public Optional<ThreadSummaryView> getSummary(UUID threadRootId) {
        return summaryRepository.findByThreadRootId(threadRootId)
                .map(s -> new ThreadSummaryView(
                        s.getSummaryBody(),
                        s.getMessageCount(),
                        s.getCreatedAt(),
                        s.getBotMessageId()
                ));
    }

    /**
     * Generates and posts a digest if the thread is long enough and not yet summarized.
     * Safe to call concurrently — only one summary row is persisted per thread root.
     */
    @Transactional
    public void summarizeIfEligible(UUID threadRootId) {
        if (!properties.isEnabled()) {
            return;
        }
        if (summaryRepository.existsByThreadRootId(threadRootId)) {
            return;
        }

        ThreadTranscriptBuilder.Transcript transcript = transcriptBuilder.build(threadRootId);
        if (transcript == null || transcript.messageCount() < properties.getMinMessages()) {
            return;
        }

        User bot = userRepository.findByEmail(BotUserInitializer.BOT_EMAIL).orElse(null);
        if (bot == null) {
            log.warn("Cannot summarize thread {} — bot user missing", threadRootId);
            return;
        }

        String digest = generateDigest(transcript.text(), bot.getId());
        String postedBody = formatBotPost(digest, transcript.messageCount());

        channelService.ensureBotInChannel(transcript.channelId());

        MessageResponse botMsg = messageService.send(
                transcript.channelId(),
                bot.getId(),
                postedBody,
                threadRootId,
                null
        );
        realtimePublisher.publishToChannel(transcript.channelId(), WsOutboundEvent.messageCreated(botMsg));

        try {
            summaryRepository.save(ThreadSummary.builder()
                    .threadRootId(threadRootId)
                    .workspaceId(transcript.workspaceId())
                    .channelId(transcript.channelId())
                    .messageCount(transcript.messageCount())
                    .summaryBody(digest)
                    .botMessageId(botMsg.id())
                    .createdAt(Instant.now())
                    .build());
            log.info("Thread digest posted: root={} messages={}", threadRootId, transcript.messageCount());
        } catch (DataIntegrityViolationException e) {
            log.debug("Thread {} already summarized by another worker", threadRootId);
        }
    }

    /** On-demand summarization for the agent tool (does not persist). */
    public String summarizeOnDemand(UUID threadRootId, UUID attributingUserId) {
        ThreadTranscriptBuilder.Transcript transcript = transcriptBuilder.build(threadRootId);
        if (transcript == null) {
            return "Thread root message not found.";
        }
        if (transcript.messageCount() < 2) {
            return "Thread is too short to summarize meaningfully.";
        }
        return generateDigest(transcript.text(), attributingUserId);
    }

    private String generateDigest(String transcriptText, UUID attributingUserId) {
        ChatResponse response = chatModel.chat(UserMessage.from(OTTER_PROMPT + transcriptText));
        if (attributingUserId != null && response.tokenUsage() != null) {
            llmUsageService.record(
                    attributingUserId,
                    LlmUsageCategory.THREAD_SUMMARY,
                    openAiProperties.getChatModel(),
                    nullSafe(response.tokenUsage().inputTokenCount()),
                    nullSafe(response.tokenUsage().outputTokenCount())
            );
        }
        return response.aiMessage().text();
    }

    private static String formatBotPost(String digest, int messageCount) {
        return """
                📋 **Thread digest** _(Otter-style overview · %d messages)_

                %s
                """.formatted(messageCount, digest).trim();
    }

    private static int nullSafe(Integer n) {
        return n == null ? 0 : n;
    }

    public record ThreadSummaryView(
            String summaryBody,
            int messageCount,
            Instant createdAt,
            UUID botMessageId
    ) {}
}
