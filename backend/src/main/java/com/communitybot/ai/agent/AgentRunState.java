package com.communitybot.ai.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Collects structured citations and step log for one agent run. */
public final class AgentRunState {

    private final List<AgentStreamEvent.CitationPayload> citations = new ArrayList<>();
    private final List<AgentStreamEvent.StepPayload>   steps     = new ArrayList<>();
    private       AgentStreamEvent.ProposalPayload   proposal;

    public void addCitation(String documentTitle, String chunkText, int chunkIndex) {
        citations.add(new AgentStreamEvent.CitationPayload(documentTitle, chunkText, chunkIndex));
    }

    public void addStep(String kind, String detail) {
        steps.add(new AgentStreamEvent.StepPayload(kind, detail));
    }

    public void setProposal(UUID id, String actionType, String summary) {
        this.proposal = new AgentStreamEvent.ProposalPayload(id, actionType, summary);
    }

    public List<AgentStreamEvent.CitationPayload> citations() {
        return List.copyOf(citations);
    }

    public List<AgentStreamEvent.StepPayload> steps() {
        return List.copyOf(steps);
    }

    public AgentStreamEvent.ProposalPayload proposal() {
        return proposal;
    }

    public int sourceChunkCount() {
        return citations.size();
    }
}
