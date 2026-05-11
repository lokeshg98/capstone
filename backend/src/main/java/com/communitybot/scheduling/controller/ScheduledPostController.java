package com.communitybot.scheduling.controller;

import com.communitybot.scheduling.dto.CreateScheduledPostRequest;
import com.communitybot.scheduling.dto.ScheduledPostResponse;
import com.communitybot.scheduling.service.ScheduledPostService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{wsId}/scheduled-posts")
@RequiredArgsConstructor
public class ScheduledPostController {

    private final ScheduledPostService scheduledPostService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledPostResponse>> create(
            @PathVariable UUID wsId,
            @Valid @RequestBody CreateScheduledPostRequest request,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(scheduledPostService.create(wsId, request, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledPostResponse>>> list(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(scheduledPostService.list(wsId, userId)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID wsId,
            @PathVariable UUID postId,
            @CurrentUser UUID userId
    ) {
        scheduledPostService.cancel(wsId, postId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
