package com.communitybot.message.dto;

import java.util.List;

public record ThreadViewResponse(
        MessageResponse root,
        List<MessageResponse> replies,
        ThreadSummaryResponse summary
) {}
