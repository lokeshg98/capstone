package com.communitybot.ai.config;

import com.communitybot.ai.usage.RecordingChatModelListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LangChainModelConfig {

    @Bean
    ChatModel chatLanguageModel(OpenAiProperties props, RecordingChatModelListener recordingListener) {
        return OpenAiChatModel.builder()
                .apiKey(nullToEmpty(props.getApiKey()))
                .modelName(props.getChatModel())
                .maxTokens(props.getMaxTokens())
                .temperature(props.getTemperature())
                .listeners(List.of(recordingListener))
                .build();
    }

    @Bean
    StreamingChatModel streamingChatLanguageModel(OpenAiProperties props, RecordingChatModelListener recordingListener) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(nullToEmpty(props.getApiKey()))
                .modelName(props.getChatModel())
                .maxTokens(props.getMaxTokens())
                .temperature(props.getTemperature())
                .listeners(List.of(recordingListener))
                .build();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
