package com.communitybot.scheduling.dto;

public record N8nIntegrationInfoResponse(
        String webhookUrl,
        String apiKeyHeader,
        boolean configured,
        String cronExample,
        String cronDescription
) {}
