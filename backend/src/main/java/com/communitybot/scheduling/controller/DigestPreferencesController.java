package com.communitybot.scheduling.controller;

import com.communitybot.scheduling.domain.UserDigestPreferences;
import com.communitybot.scheduling.dto.UpdateDigestPreferencesRequest;
import com.communitybot.scheduling.dto.UserDigestPreferencesResponse;
import com.communitybot.scheduling.service.WeeklyDigestService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DigestPreferencesController {

    private final WeeklyDigestService weeklyDigestService;

    @GetMapping("/api/me/digest-preferences")
    public ResponseEntity<ApiResponse<UserDigestPreferencesResponse>> getPreferences(@CurrentUser UUID userId) {
        UserDigestPreferences prefs = weeklyDigestService.getOrCreatePreferences(userId);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(prefs)));
    }

    @PutMapping("/api/me/digest-preferences")
    public ResponseEntity<ApiResponse<UserDigestPreferencesResponse>> updatePreferences(
            @CurrentUser UUID userId,
            @Valid @RequestBody UpdateDigestPreferencesRequest req
    ) {
        UserDigestPreferences prefs =
                weeklyDigestService.updatePreferences(userId, req.weeklyDigestEnabled());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(prefs)));
    }

    private static UserDigestPreferencesResponse toResponse(UserDigestPreferences prefs) {
        return new UserDigestPreferencesResponse(
                prefs.getUserId(),
                prefs.isWeeklyDigestEnabled(),
                prefs.getUpdatedAt()
        );
    }
}
