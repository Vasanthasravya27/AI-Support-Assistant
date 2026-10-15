package com.supportassistant.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestResult(int documents, int chunks) {
}
