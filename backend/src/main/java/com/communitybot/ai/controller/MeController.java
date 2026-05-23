package com.communitybot.ai.controller;

import com.communitybot.ai.usage.LlmUsageService;
import com.communitybot.ai.usage.dto.LlmUsageSummaryResponse;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final LlmUsageService llmUsageService;

    @GetMapping("/llm-usage")
    public ResponseEntity<ApiResponse<LlmUsageSummaryResponse>> llmUsage(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(llmUsageService.summarizeForUserAndProject(userId)));
    }
}
