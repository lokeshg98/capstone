package com.communitybot.ai.dto;

public record AskResponse(
        String answer,
        int    sourceChunks   // number of FAQ chunks used as context
) {}
