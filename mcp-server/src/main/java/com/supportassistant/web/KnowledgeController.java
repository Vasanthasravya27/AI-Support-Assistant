package com.supportassistant.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.supportassistant.rag.IngestResult;
import com.supportassistant.rag.RagClient;
import com.supportassistant.rag.SearchHit;

/** Admin / debugging endpoints for the knowledge base. */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final RagClient ragClient;

    public KnowledgeController(RagClient ragClient) {
        this.ragClient = ragClient;
    }

    /** Re-index the knowledge-base folder via the Python service. */
    @PostMapping("/reindex")
    public IngestResult reindex() {
        return ragClient.reindex();
    }

    /** Raw retrieval results, useful for tuning chunking and score thresholds. */
    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam("q") String query,
                                  @RequestParam(value = "topK", required = false) Integer topK) {
        return ragClient.search(query, topK);
    }
}
