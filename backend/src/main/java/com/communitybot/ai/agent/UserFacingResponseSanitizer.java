package com.communitybot.ai.agent;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Strips deployment, env-var, and infrastructure language from text shown to users.
 */
@Component
public class UserFacingResponseSanitizer {

    private static final Pattern ENV_VAR = Pattern.compile(
            "`?(?:MODERATION_ENABLED|OPENAI_API_KEY|TAVILY_API_KEY|N8N_API_KEY|JWT_SECRET|"
                    + "FRONTEND_URL|CLAMAV_ENABLED|N8N_BLOCK_ENV_ACCESS_IN_NODE)`?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern INFRA_PATH = Pattern.compile(
            "infra/\\.env|application-dev\\.yml|application\\.yml|docker-compose|gradlew|:bootRun|RUNBOOK\\.md",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXPORT_LINE = Pattern.compile(
            "^\\s*export\\s+[A-Z0-9_]+=.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    /** Product names / stack terms that must not appear in user-facing bot or FAQ answers. */
    private static final Pattern TECH_STACK = Pattern.compile(
            "\\b(?:MinIO|S3|pgvector|PostgreSQL|Postgres|Redis|Spring Boot|LangChain4?j|LangGraph4?j|"
                    + "WebSocket(?:s)?|STOMP|SSE|Tavily|ClamAV|n8n|Flyway|JPA|Hibernate|Docker|Kubernetes|"
                    + "Tomcat|Gradle|SockJS|OAuth2|JWT|OpenAI API|GPT-4|text-embedding|JoyPixels|CDN)\\b",
            Pattern.CASE_INSENSITIVE);

    public String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String out = text;
        out = out.replaceAll("(?i)When\\s+`?MODERATION_ENABLED=true`?,",
                "When moderation is enabled,");
        out = out.replaceAll("(?i)set\\s+`?MODERATION_ENABLED=true`?\\s+before starting the backend",
                "ensure moderation is enabled for your workspace");
        out = out.replaceAll("(?i)Files are stored securely in the backend object store \\(MinIO\\)\\.",
                "Files are scanned and stored securely.");
        out = out.replaceAll("(?i)stored securely in the backend object store \\(MinIO\\)",
                "stored securely");
        out = out.replaceAll("(?i)The platform uses WebSockets\\. ",
                "Messages are delivered in real time. ");
        out = out.replaceAll("(?i)run \\*\\*web search\\*\\* when enabled \\(Tavily; typically \\*\\*admins only\\*\\*\\)",
                "run **web search** when enabled (typically **admins only**)");
        out = out.replaceAll("(?i)The Spring Boot backend is running on port 8080",
                "The server is running");
        out = out.replaceAll("(?i)Ensure MinIO is running and the `community-bot` bucket exists\\.[^\\n]*",
                "If uploads fail, contact your workspace admin.");
        out = out.replaceAll("(?i)The WebSocket connection to `ws://localhost:8080/ws` may have failed\\.[^\\n]*",
                "Real-time delivery may be interrupted. Try refreshing the page.");
        out = ENV_VAR.matcher(out).replaceAll("");
        out = INFRA_PATH.matcher(out).replaceAll("");
        out = TECH_STACK.matcher(out).replaceAll("");
        out = EXPORT_LINE.matcher(out).replaceAll("");
        out = out.replaceAll("(?i)and exported before starting the backend", "");
        out = out.replaceAll("(?i)Without a valid OpenAI key, RAG queries will fail with a 500 error\\.",
                "If the assistant is temporarily unavailable, try again or contact your workspace admin.");
        out = out.replaceAll("(?m)^\\s*```bash\\s*\\R[\\s\\S]*?```\\s*", "");
        out = out.replaceAll(" \\.", ".");
        out = out.replaceAll(" {2,}", " ");
        out = out.replaceAll("\\n{3,}", "\n\n");
        return out.trim();
    }
}
