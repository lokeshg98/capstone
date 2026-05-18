package com.communitybot.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentStreamRequest(
        @NotBlank @Size(max = 1000) String question,
        @Size(max = 64) String conversationId
) {}
