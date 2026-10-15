package com.supportassistant.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One chunk returned by the Python RAG service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchHit(
        String source,
        String title,
        @JsonProperty("chunk_index") int chunkIndex,
        double score,
        String text) {
}
