package com.communitybot.ai.controller;

import com.communitybot.ai.dto.AskRequest;
import com.communitybot.ai.dto.AskResponse;
import com.communitybot.ai.service.RagService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Synchronous Ask Bot endpoint (non-SSE). Prefer {@link AgentController} for streaming + tool UX.
 */
@RestController
@RequestMapping("/api/workspaces/{wsId}/ask")
@RequiredArgsConstructor
public class AskController {

    private final RagService ragService;

    @PostMapping
    public ResponseEntity<ApiResponse<AskResponse>> ask(
            @PathVariable UUID wsId,
            @Valid @RequestBody AskRequest req,
            @CurrentUser UUID userId
    ) {
        var ans = ragService.ask(wsId, userId, req.question(), req.conversationId());
        return ResponseEntity.ok(ApiResponse.ok(
                new AskResponse(ans.answer(), ans.sourceChunks(), ans.citations(), ans.steps())));
    }
}
