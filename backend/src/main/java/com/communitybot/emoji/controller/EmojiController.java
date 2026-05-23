package com.communitybot.emoji.controller;

import com.communitybot.emoji.config.JoyPixelsProperties;
import com.communitybot.emoji.dto.EmojiSearchResult;
import com.communitybot.emoji.service.JoyPixelsEmojiService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/emoji")
@RequiredArgsConstructor
public class EmojiController {

    private final JoyPixelsEmojiService service;
    private final JoyPixelsProperties   properties;

    /**
     * JoyPixels-powered emoji search (keyword + optional AI semantic matching).
     * Example: {@code GET /api/emoji/search?q=celebration&limit=20}
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EmojiSearchResult>>> search(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "0") int limit,
            @CurrentUser UUID userId
    ) {
        if (!properties.isEnabled()) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        int effectiveLimit = limit > 0 ? limit : properties.getDefaultLimit();
        return ResponseEntity.ok(ApiResponse.ok(service.search(q, effectiveLimit, userId)));
    }
}
