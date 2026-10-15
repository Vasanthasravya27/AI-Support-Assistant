package com.supportassistant.rag;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.supportassistant.config.RagProperties;

/** Java-side client for the Python RAG service (retrieval + indexing). */
@Component
public class RagClient {

    private static final int MAX_TOP_K = 10;

    private final RestClient http;
    private final RagProperties props;

    public RagClient(@Qualifier("ragRestClient") RestClient http, RagProperties props) {
        this.http = http;
        this.props = props;
    }

    /** Semantic search. {@code topK} may be null to use the configured default. */
    public List<SearchHit> search(String query, Integer topK) {
        int k = topK == null ? props.defaultTopK() : Math.max(1, Math.min(topK, MAX_TOP_K));
        try {
            SearchResponse response = http.post()
                    .uri("/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new SearchBody(query, k, props.minScore()))
                    .retrieve()
                    .body(SearchResponse.class);
            return response == null || response.hits() == null ? List.of() : response.hits();
        } catch (RestClientException e) {
            throw new RagUnavailableException("RAG search failed: " + e.getMessage(), e);
        }
    }

    /** Asks the RAG service to re-index the knowledge-base folder. */
    public IngestResult reindex() {
        try {
            return http.post().uri("/ingest").retrieve().body(IngestResult.class);
        } catch (RestClientException e) {
            throw new RagUnavailableException("RAG re-index failed: " + e.getMessage(), e);
        }
    }

    record SearchBody(
            String query,
            @JsonProperty("top_k") int topK,
            @JsonProperty("min_score") double minScore) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(List<SearchHit> hits) {
    }
}
