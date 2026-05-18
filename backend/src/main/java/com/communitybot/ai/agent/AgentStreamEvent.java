package com.communitybot.ai.agent;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Server-sent event payload for streaming agent runs (Ask panel).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentStreamEvent(
        String type,
        String message,
        String toolName,
        String toolArgsSummary,
        String toolResultSummary,
        String token,
        AgentAnswerPayload payload
) {
    public static AgentStreamEvent thinking(String message) {
        return new AgentStreamEvent("thinking", message, null, null, null, null, null);
    }

    public static AgentStreamEvent toolStart(String toolName, String argsSummary) {
        return new AgentStreamEvent("tool_start", null, toolName, argsSummary, null, null, null);
    }

    public static AgentStreamEvent toolResult(String toolName, String resultSummary) {
        return new AgentStreamEvent("tool_result", null, toolName, null, resultSummary, null, null);
    }

    public static AgentStreamEvent tokenDelta(String token) {
        return new AgentStreamEvent("token", null, null, null, null, token, null);
    }

    public static AgentStreamEvent actionProposal(AgentAnswerPayload p) {
        return new AgentStreamEvent("action_proposal", null, null, null, null, null, p);
    }

    public static AgentStreamEvent done(AgentAnswerPayload p) {
        return new AgentStreamEvent("final", null, null, null, null, null, p);
    }

    public record AgentAnswerPayload(
            String answer,
            int    sourceChunks,
            List<CitationPayload> citations,
            List<StepPayload> steps,
            ProposalPayload proposedAction
    ) {}

    public record CitationPayload(String documentTitle, String chunkText, int chunkIndex) {}

    public record StepPayload(String kind, String detail) {}

    public record ProposalPayload(
            java.util.UUID id,
            String actionType,
            String summary
    ) {}
}
