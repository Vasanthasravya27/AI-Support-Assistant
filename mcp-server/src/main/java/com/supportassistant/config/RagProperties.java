package com.supportassistant.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings for talking to the Python RAG service (prefix: rag.*). */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        @DefaultValue("http://localhost:8000") String baseUrl,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout,
        @DefaultValue("4") int defaultTopK,
        @DefaultValue("0.25") double minScore) {
}
