package com.communitybot.emoji.dto;

public record EmojiSearchResult(
        String unicode,
        String name,
        String shortname,
        String category,
        String imageUrl
) {}
