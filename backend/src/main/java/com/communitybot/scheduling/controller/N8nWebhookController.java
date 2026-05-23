package com.communitybot.scheduling.controller;

import com.communitybot.scheduling.config.N8nProperties;
import com.communitybot.scheduling.dto.N8nWeeklyDigestRequest;
import com.communitybot.scheduling.dto.WeeklyDigestRunResponse;
import com.communitybot.scheduling.service.WeeklyDigestService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook endpoints invoked by n8n cron workflows (e.g. weekly digest every Friday 5pm).
 * Authenticated via {@code X-N8n-Api-Key} — see {@link com.communitybot.scheduling.config.N8nApiKeyAuthFilter}.
 */
@RestController
@RequestMapping("/api/internal/n8n")
@RequiredArgsConstructor
public class N8nWebhookController {

    private final WeeklyDigestService weeklyDigestService;
    private final N8nProperties       n8nProperties;

    /**
     * Generates personalized weekly digests for every workspace member who opted in,
     * summarizing activity across all channels they are subscribed to.
     */
    @PostMapping("/weekly-digest")
    public ResponseEntity<ApiResponse<WeeklyDigestRunResponse>> runWeeklyDigest(
            @RequestBody(required = false) N8nWeeklyDigestRequest request
    ) {
        if (!n8nProperties.isEnabled()) {
            throw new AppException(ErrorCode.WORKSPACE_ACCESS_DENIED, "n8n integration is disabled");
        }

        int periodDays = request != null && request.periodDays() != null
                ? request.periodDays()
                : n8nProperties.getDefaultPeriodDays();
        boolean dryRun = request != null && Boolean.TRUE.equals(request.dryRun());
        var workspaceId = request != null ? request.workspaceId() : null;

        WeeklyDigestRunResponse result =
                weeklyDigestService.runWeeklyDigests(workspaceId, periodDays, dryRun);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
