package com.communitybot.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChannelRequest(
        @NotBlank @Size(min = 1, max = 100) String name,
        @Size(max = 300) String description
) {
}
