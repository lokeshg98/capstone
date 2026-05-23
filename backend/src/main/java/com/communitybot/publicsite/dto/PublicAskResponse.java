package com.communitybot.publicsite.dto;

import java.util.List;

public record PublicAskResponse(
        String answer,
        List<String> steps
) {}
