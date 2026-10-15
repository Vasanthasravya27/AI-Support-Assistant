package com.supportassistant.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.supportassistant.mcp.KnowledgeBaseTools;
import com.supportassistant.mcp.TicketTools;

@Configuration
public class AppConfig {

    /** HTTP client pointed at the Python RAG service, with timeouts. */
    @Bean
    RestClient ragRestClient(RagProperties props,
                             @Value("${RAG_API_TOKEN:}") String ragApiToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory);
        if (!ragApiToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + ragApiToken);
        }
        return builder.build();
    }

    /**
     * Every @Tool method on these objects is published by the MCP server.
     * To expose a new tool: add a @Tool method and register its class here.
     */
    @Bean
    ToolCallbackProvider supportTools(KnowledgeBaseTools knowledgeBaseTools, TicketTools ticketTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(knowledgeBaseTools, ticketTools)
                .build();
    }
}
