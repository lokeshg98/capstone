package com.communitybot.attachment.dto;

import java.util.List;

public record AttachmentLimitsResponse(
        long maxSizeBytes,
        List<String> allowedExtensions
) {}
