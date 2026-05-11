package com.communitybot.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrgRequest(
        @NotBlank @Size(min = 2, max = 100) String name
) {
}
