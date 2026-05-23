package com.communitybot.auth.controller;

import com.communitybot.auth.dto.UpdateUserProfileRequest;
import com.communitybot.auth.dto.UserProfileDetailResponse;
import com.communitybot.auth.service.UserProfileService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDetailResponse>> getProfile(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile(userId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDetailResponse>> updateProfile(
            @CurrentUser UUID userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.updateProfile(userId, request)));
    }
}
