package com.communitybot.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinOrgRequest(
        @NotBlank @Size(min = 2, max = 100) String slug
) {}
