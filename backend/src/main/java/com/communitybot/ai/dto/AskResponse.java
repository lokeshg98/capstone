package com.communitybot.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AskResponse(
        String answer,
        int sourceChunks,
        List<AskCitation> citations,
        List<AskStep> steps
) {
    public AskResponse(String answer, int sourceChunks) {
        this(answer, sourceChunks, List.of(), List.of());
    }
}
