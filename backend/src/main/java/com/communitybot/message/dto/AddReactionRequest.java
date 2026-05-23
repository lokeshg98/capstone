package com.communitybot.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddReactionRequest(
        @NotBlank @Size(max = 32) String emoji
) {
}
