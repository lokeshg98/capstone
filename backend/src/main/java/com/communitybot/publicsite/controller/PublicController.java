package com.communitybot.publicsite.controller;

import com.communitybot.ai.agent.AgentContext;
import com.communitybot.ai.agent.AgentContextHolder;
import com.communitybot.ai.agent.AgentRunState;
import com.communitybot.ai.agent.AgentRunStateHolder;
import com.communitybot.ai.agent.CommunityAgentGraphService;
import com.communitybot.ai.agent.UserFacingResponseSanitizer;
import com.communitybot.publicsite.dto.*;
import com.communitybot.publicsite.service.NewsletterService;
import com.communitybot.publicsite.service.PublicFaqService;
import com.communitybot.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicFaqService            publicFaqService;
    private final NewsletterService           newsletterService;
    private final CommunityAgentGraphService  agentGraphService;
    private final UserFacingResponseSanitizer responseSanitizer;

    @GetMapping("/platform")
    public ResponseEntity<ApiResponse<PlatformInfoResponse>> platform() {
        return ResponseEntity.ok(ApiResponse.ok(new PlatformInfoResponse(
                "Community Bot",
                "AI-powered community platform for teams",
                """
                Community Bot combines Slack-style real-time chat with an intelligent assistant that \
                answers FAQ questions, moderates content, summarizes threads, welcomes new members, \
                and schedules automated posts — all in one workspace.
                """.trim(),
                List.of(
                        new PlatformInfoResponse.FeatureCard(
                                "Real-time chat",
                                "Channels, threads, reactions, and file attachments with real-time delivery.",
                                "messages"
                        ),
                        new PlatformInfoResponse.FeatureCard(
                                "AI assistant (RAG)",
                                "Ask Bot searches your uploaded FAQ documents and channel history for grounded answers.",
                                "bot"
                        ),
                        new PlatformInfoResponse.FeatureCard(
                                "Smart moderation",
                                "LLM content classification flags toxic, spam, and harassment for moderator review.",
                                "shield"
                        ),
                        new PlatformInfoResponse.FeatureCard(
                                "Thread summaries",
                                "Long discussions distilled into TL;DR bullets with decisions and open questions.",
                                "summary"
                        ),
                        new PlatformInfoResponse.FeatureCard(
                                "Scheduled posts",
                                "Cron or one-shot announcements — with human confirmation for agent proposals.",
                                "calendar"
                        ),
                        new PlatformInfoResponse.FeatureCard(
                                "Role-based flows",
                                "Member, moderator, and admin capabilities are applied automatically based on your role.",
                                "roles"
                        )
                )
        )));
    }

    @GetMapping("/faq")
    public ResponseEntity<ApiResponse<List<PublicFaqEntry>>> faq(
            @RequestParam(defaultValue = "8") int limit
    ) {
        int capped = Math.clamp(limit, 1, 20);
        List<PublicFaqEntry> entries = publicFaqService.loadEntries(capped).stream()
                .map(e -> new PublicFaqEntry(e.question(), e.answer()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @PostMapping("/newsletter")
    public ResponseEntity<ApiResponse<Void>> newsletter(@Valid @RequestBody NewsletterSubscribeRequest req) {
        newsletterService.subscribe(req.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    /** Public demo bot — no login; searches platform FAQ only. */
    @PostMapping("/agent/ask")
    public ResponseEntity<ApiResponse<PublicAskResponse>> publicAsk(@Valid @RequestBody PublicAskRequest req) {
        String sessionId = "public-" + UUID.randomUUID();

        AgentRunStateHolder.init();
        AgentContextHolder.set(AgentContext.publicAsk());
        try {
            String answer;
            List<String> steps;
            try {
                answer = agentGraphService.runSync(req.question(), sessionId);
                AgentRunState st = AgentRunStateHolder.get();
                steps = st == null ? List.of()
                        : st.steps().stream().map(s -> s.kind() + ": " + s.detail()).toList();
            } catch (Exception agentErr) {
                answer = publicFaqService.answerForUser(req.question());
                var best = publicFaqService.findBestMatch(req.question());
                steps = best != null
                        ? List.of("public_faq: " + best.question())
                        : List.of("public_faq: no strong match");
            }
            return ResponseEntity.ok(ApiResponse.ok(
                    new PublicAskResponse(responseSanitizer.sanitize(answer), steps)));
        } finally {
            AgentContextHolder.clear();
            AgentRunStateHolder.clear();
        }
    }
}
