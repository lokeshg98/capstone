package com.communitybot.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AskCitation(
        String documentTitle,
        String chunkText,
        int chunkIndex
) {}
