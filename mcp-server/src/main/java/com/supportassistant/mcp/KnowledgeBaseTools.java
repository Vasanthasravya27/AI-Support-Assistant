package com.supportassistant.mcp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.supportassistant.rag.RagClient;
import com.supportassistant.rag.RagUnavailableException;

/** MCP tools that give the agent access to the RAG knowledge base. */
@Component
public class KnowledgeBaseTools {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseTools.class);

    private final RagClient ragClient;

    public KnowledgeBaseTools(RagClient ragClient) {
        this.ragClient = ragClient;
    }

    @Tool(name = "search_knowledge_base",
            description = """
                    Searches the support knowledge base (help articles, policies, troubleshooting guides) \
                    and returns the most relevant text snippets with their source document and a similarity score. \
                    Call this BEFORE answering any product, billing, account or policy question. \
                    Answer only from the returned snippets; if none are relevant, say so and offer to create a ticket.""")
    public Result searchKnowledgeBase(
            @ToolParam(description = "The customer's question or a concise keyword query") String query,
            @ToolParam(description = "Maximum number of snippets to return (1-10). Defaults to 4.",
                    required = false) Integer maxResults) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be empty");
        }
        try {
            List<Snippet> snippets = ragClient.search(query, maxResults).stream()
                    .map(hit -> new Snippet(hit.source(), hit.title(), hit.score(), hit.text()))
                    .toList();
            String message = snippets.isEmpty()
                    ? "No relevant articles found. Do not guess an answer; offer to create a support ticket."
                    : snippets.size() + " relevant snippet(s) found.";
            return new Result(true, message, snippets);
        } catch (RagUnavailableException e) {
            log.warn("Knowledge base unavailable: {}", e.getMessage());
            return new Result(false,
                    "The knowledge base is temporarily unavailable. Offer to create a support ticket instead.",
                    List.of());
        }
    }

    public record Result(boolean available, String message, List<Snippet> snippets) {
    }

    public record Snippet(String source, String title, double score, String text) {
    }
}
