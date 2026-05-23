package com.communitybot.ai.controller;

import com.communitybot.ai.agent.MultiAgentService;
import com.communitybot.ai.agent.ProposedActionService;
import com.communitybot.ai.dto.AgentStreamRequest;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{wsId}/agent")
@RequiredArgsConstructor
public class AgentController {

    private final MultiAgentService    multiAgentService;
    private final ProposedActionService proposedActionService;

    @PostMapping(path = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId,
            @Valid @RequestBody AgentStreamRequest req
    ) {
        SseEmitter emitter = new SseEmitter(300_000L);
        var securityContext = SecurityContextHolder.getContext();
        Thread.ofVirtual().start(new DelegatingSecurityContextRunnable(
                () -> multiAgentService.streamAnswer(
                        emitter, wsId, userId, req.conversationId(), req.question()),
                securityContext
        ));
        return emitter;
    }

    @PostMapping("/actions/{actionId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(
            @PathVariable UUID wsId,
            @PathVariable UUID actionId,
            @CurrentUser UUID userId
    ) {
        proposedActionService.confirmScheduledPost(wsId, userId, actionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/actions/{actionId}/decline")
    public ResponseEntity<ApiResponse<Void>> decline(
            @PathVariable UUID wsId,
            @PathVariable UUID actionId,
            @CurrentUser UUID userId
    ) {
        proposedActionService.decline(wsId, userId, actionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
