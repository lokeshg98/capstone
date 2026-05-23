package com.communitybot.moderation.service;

import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.repository.WorkspaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
public class CommunityGuidelinesService {

    private final WorkspaceRepository workspaceRepository;
    private final String defaultGuidelines;

    public CommunityGuidelinesService(WorkspaceRepository workspaceRepository, ResourceLoader resourceLoader) {
        this.workspaceRepository = workspaceRepository;
        this.defaultGuidelines   = loadDefault(resourceLoader);
    }

    public String resolveForWorkspace(UUID workspaceId) {
        if (workspaceId == null) {
            return defaultGuidelines;
        }
        return workspaceRepository.findById(workspaceId)
                .map(Workspace::getCommunityGuidelines)
                .filter(g -> g != null && !g.isBlank())
                .orElse(defaultGuidelines);
    }

    private static String loadDefault(ResourceLoader loader) {
        try {
            Resource resource = loader.getResource("classpath:community-guidelines.md");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Could not load community-guidelines.md: {}", e.getMessage());
            return "Be respectful. No harassment, hate speech, threats, spam, or phishing.";
        }
    }
}
