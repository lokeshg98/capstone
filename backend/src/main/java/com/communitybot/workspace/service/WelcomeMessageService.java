package com.communitybot.workspace.service;

import com.communitybot.ai.config.OpenAiProperties;
import com.communitybot.ai.usage.LlmUsageCategory;
import com.communitybot.ai.usage.LlmUsageService;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.domain.UserProfile;
import com.communitybot.auth.service.UserProfileService;
import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.domain.ChannelType;
import com.communitybot.channel.repository.ChannelRepository;
import com.communitybot.workspace.config.WelcomeProperties;
import com.communitybot.workspace.domain.Workspace;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WelcomeMessageService {

    private static final String PERSONALIZE_PROMPT = """
            You are the Community Bot welcoming a new member to a workspace chat (#general).
            Write ONE friendly onboarding message (2–4 sentences, markdown OK, light emoji OK).

            Rules:
            - Greet them by name
            - If they listed interests, mention 1–2 relevant discussion topics or channel types they might enjoy
            - If they wrote an "about me", acknowledge it naturally (do not quote verbatim)
            - Encourage them to explore channels and introduce themselves
            - Do NOT invent interests or background not provided below
            - Keep tone warm and inclusive

            Workspace: %s
            Template hint (optional tone guide): %s

            New member profile:
            Name: %s
            About: %s
            Interests: %s
            Public channels: %s
            """;

    private final WelcomeProperties    welcomeProperties;
    private final UserProfileService userProfileService;
    private final ChannelRepository  channelRepository;
    private final ChatModel          chatModel;
    private final OpenAiProperties   openAiProperties;
    private final LlmUsageService    llmUsageService;

    public String buildWelcomeMessage(Workspace workspace, User user, UUID botUserId) {
        if (!welcomeProperties.isEnabled()) {
            return null;
        }

        UserProfile profile = userProfileService.getOrCreateProfile(user.getId());
        String template = resolveTemplate(workspace);

        if (welcomeProperties.isPersonalizationEnabled() && hasPersonalizationSignal(profile)) {
            try {
                return personalizeWithLlm(workspace, user, profile, template, botUserId);
            } catch (Exception e) {
                log.warn("Welcome personalization failed for user {}: {}", user.getId(), e.getMessage());
            }
        }

        return applyPlaceholders(template, user, profile);
    }

    private String personalizeWithLlm(
            Workspace workspace,
            User user,
            UserProfile profile,
            String templateHint,
            UUID botUserId
    ) {
        List<Channel> channels = channelRepository.findAllByWorkspaceIdAndTypeOrderByNameAsc(
                workspace.getId(), ChannelType.PUBLIC);

        String channelNames = channels.stream()
                .map(c -> "#" + c.getName())
                .collect(Collectors.joining(", "));

        String name = displayName(user);
        String about = profile.getAboutMe() != null && !profile.getAboutMe().isBlank()
                ? profile.getAboutMe() : "(not provided)";
        String interests = profile.interestList().isEmpty()
                ? "(not provided)"
                : String.join(", ", profile.interestList());

        String prompt = PERSONALIZE_PROMPT.formatted(
                workspace.getName(),
                templateHint,
                name,
                about,
                interests,
                channelNames.isBlank() ? "#general" : channelNames
        );

        ChatResponse response = chatModel.chat(UserMessage.from(prompt));
        if (response.tokenUsage() != null && botUserId != null) {
            llmUsageService.record(
                    botUserId,
                    LlmUsageCategory.WELCOME_MESSAGE,
                    openAiProperties.getChatModel(),
                    nullSafe(response.tokenUsage().inputTokenCount()),
                    nullSafe(response.tokenUsage().outputTokenCount())
            );
        }
        return response.aiMessage().text().trim();
    }

    private static boolean hasPersonalizationSignal(UserProfile profile) {
        return (profile.getAboutMe() != null && !profile.getAboutMe().isBlank())
                || !profile.interestList().isEmpty();
    }

    private String resolveTemplate(Workspace workspace) {
        String template = workspace.getWelcomeMessageTemplate();
        if (template == null || template.isBlank()) {
            return welcomeProperties.getDefaultTemplate();
        }
        return template;
    }

    static String applyPlaceholders(String template, User user, UserProfile profile) {
        String name = displayName(user);
        String interests = profile.interestList().isEmpty()
                ? "exploring the community"
                : String.join(", ", profile.interestList());
        String about = profile.getAboutMe() != null && !profile.getAboutMe().isBlank()
                ? profile.getAboutMe()
                : "getting started";

        return template
                .replace("{name}", name)
                .replace("{interests}", interests)
                .replace("{about}", about);
    }

    private static String displayName(User user) {
        return user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
    }

    private static int nullSafe(Integer n) {
        return n == null ? 0 : n;
    }
}
