package com.communitybot.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AskStep(
        String kind,
        String detail
) {}
