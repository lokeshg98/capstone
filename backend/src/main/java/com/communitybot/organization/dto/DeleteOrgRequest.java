package com.communitybot.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteOrgRequest(
        @NotBlank @Size(max = 100) String confirmSlug
) {}
