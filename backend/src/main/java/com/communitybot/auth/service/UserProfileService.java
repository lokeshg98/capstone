package com.communitybot.auth.service;

import com.communitybot.auth.domain.NotificationMode;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.domain.UserProfile;
import com.communitybot.auth.dto.UpdateUserProfileRequest;
import com.communitybot.auth.dto.UserProfileDetailResponse;
import com.communitybot.auth.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserService            userService;
    private final UserProfileRepository  profileRepository;

    @Transactional(readOnly = true)
    public UserProfileDetailResponse getProfile(UUID userId) {
        User user = userService.getOrThrow(userId);
        UserProfile profile = profileRepository.findById(userId).orElse(emptyProfile(userId));
        return UserProfileDetailResponse.from(user, profile);
    }

    @Transactional
    public UserProfileDetailResponse updateProfile(UUID userId, UpdateUserProfileRequest req) {
        User user = userService.getOrThrow(userId);
        UserProfile profile = getOrCreateProfile(userId);

        String interestsCsv = req.interests() == null
                ? profile.getInterests()
                : req.interests().stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(","));

        profile.update(
                req.statusMessage() != null ? req.statusMessage() : profile.getStatusMessage(),
                req.aboutMe() != null ? req.aboutMe() : profile.getAboutMe(),
                req.phone() != null ? req.phone() : profile.getPhone(),
                req.showEmail() != null ? req.showEmail() : profile.isShowEmail(),
                req.showPhone() != null ? req.showPhone() : profile.isShowPhone(),
                interestsCsv,
                req.notificationMode() != null ? req.notificationMode() : profile.getNotificationMode()
        );
        profileRepository.save(profile);
        return UserProfileDetailResponse.from(user, profile);
    }

    @Transactional
    public UserProfile getOrCreateProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(
                        UserProfile.builder()
                                .userId(userId)
                                .showEmail(false)
                                .showPhone(false)
                                .notificationMode(NotificationMode.ALL)
                                .updatedAt(Instant.now())
                                .build()
                ));
    }

    private static UserProfile emptyProfile(UUID userId) {
        return UserProfile.builder()
                .userId(userId)
                .showEmail(false)
                .showPhone(false)
                .notificationMode(NotificationMode.ALL)
                .updatedAt(Instant.now())
                .build();
    }
}
