package com.communitybot.scheduling.controller;

import com.communitybot.scheduling.config.N8nProperties;
import com.communitybot.scheduling.dto.N8nIntegrationInfoResponse;
import com.communitybot.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/n8n")
@RequiredArgsConstructor
public class N8nIntegrationController {

    private final N8nProperties n8nProperties;

    @Value("${server.port:8080}")
    private int serverPort;

    @GetMapping("/integration")
    public ResponseEntity<ApiResponse<N8nIntegrationInfoResponse>> integrationInfo() {
        boolean configured = n8nProperties.getApiKey() != null && !n8nProperties.getApiKey().isBlank();
        return ResponseEntity.ok(ApiResponse.ok(new N8nIntegrationInfoResponse(
                "http://localhost:" + serverPort + "/api/internal/n8n/weekly-digest",
                "X-N8n-Api-Key",
                configured,
                "0 0 17 * * FRI",
                "Every Friday at 5:00 PM (server timezone) — use n8n Cron node"
        )));
    }
}
