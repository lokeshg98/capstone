package com.communitybot.publicsite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicAskRequest(
        @NotBlank @Size(max = 2000) String question
) {}
