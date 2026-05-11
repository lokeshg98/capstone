package com.communitybot.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String body,
        UUID threadRootId,    // null → top-level message
        UUID attachmentId     // null → text-only message
) {
}
