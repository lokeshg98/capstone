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
 * Synchronous RAG endpoint for the "Ask Bot" panel in the workspace sidebar.
 * This path does not post a message to any channel — the answer is returned
 * directly to the requesting client.
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
        RagService.RagResult result = ragService.ask(wsId, req.question());
        return ResponseEntity.ok(ApiResponse.ok(
                new AskResponse(result.answer(), result.sourceChunks())));
    }
}
