package com.communitybot.ai.agent;

import com.communitybot.ai.service.EmbeddingService;
import com.communitybot.document.repository.DocumentChunkRepository;
import com.communitybot.document.repository.FaqSearchChunkRow;
import com.communitybot.document.service.DocumentIngestionService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

/**
 * Tool set for the CHANNEL agent (@bot mentions).
 * Only exposes {@code searchFaqDocuments} so the LLM cannot call other tools (e.g. recallPastConversations).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelAgentTools {

    private final EmbeddingService         embeddingService;
    private final DocumentChunkRepository  documentChunkRepository;

    @Tool("Search ingested FAQ documents for this workspace. Returns the top matching document chunks. This is the ONLY tool available — use it to answer the user's question and do NOT request any other tools.")
    public String searchFaqDocuments(String query) {
        AgentContext ctx = AgentContextHolder.get();
        if (ctx == null) return "No workspace context available.";

        try {
            float[] emb = embeddingService.embed(query);
            String vec = DocumentIngestionService.floatArrayToVectorString(emb);
            List<FaqSearchChunkRow> rows = documentChunkRepository.findTopFaqChunksWithTitles(ctx.workspaceId(), vec);
            log.info("ChannelAgentTools.searchFaqDocuments: query='{}' workspaceId={} rowsFound={}",
                    query, ctx.workspaceId(), rows.size());

            if (rows.isEmpty()) {
                return "No FAQ documents found for this query.";
            }

            if (!rows.isEmpty()) {
                String first = rows.get(0).getChunkText();
                log.debug("searchFaqDocuments first chunk: doc='{}' text='{}...'",
                        rows.get(0).getDocumentTitle(),
                        first.length() > 200 ? first.substring(0, 200) : first);
            }

            AgentRunState state = AgentRunStateHolder.get();
            StringJoiner joiner = new StringJoiner("\n\n---\n\n");
            joiner.add("Answer the user's question using ONLY the following document excerpts:");
            for (FaqSearchChunkRow row : rows) {
                if (state != null) {
                    state.addCitation(row.getDocumentTitle(), row.getChunkText(), row.getChunkIndex());
                }
                joiner.add("Document: " + row.getDocumentTitle() + "\n" + row.getChunkText());
            }
            return joiner.toString();
        } catch (Exception e) {
            log.warn("Channel FAQ search failed: {}", e.getMessage());
            return "FAQ search failed: " + e.getMessage();
        }
    }
}
