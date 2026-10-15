package com.supportassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.supportassistant.config.RagProperties;

class RagClientTest {

    private MockRestServiceServer server;
    private RagClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://rag.test");
        server = MockRestServiceServer.bindTo(builder).build();
        RagProperties props = new RagProperties(
                "http://rag.test", Duration.ofSeconds(1), Duration.ofSeconds(1), 4, 0.25);
        client = new RagClient(builder.build(), props);
    }

    @Test
    void searchSendsSnakeCaseBodyAndParsesHits() {
        server.expect(requestTo("http://rag.test/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.query").value("reset password"))
                .andExpect(jsonPath("$.top_k").value(4))
                .andRespond(withSuccess("""
                        {"query":"reset password","hits":[
                          {"source":"password-reset.md","title":"How to Reset Your Password",
                           "chunk_index":0,"score":0.83,"text":"Click Forgot password"}]}
                        """, MediaType.APPLICATION_JSON));

        List<SearchHit> hits = client.search("reset password", null);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).source()).isEqualTo("password-reset.md");
        assertThat(hits.get(0).score()).isEqualTo(0.83);
        server.verify();
    }

    @Test
    void searchClampsTopK() {
        server.expect(requestTo("http://rag.test/search"))
                .andExpect(jsonPath("$.top_k").value(10))
                .andRespond(withSuccess("{\"hits\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.search("anything", 500)).isEmpty();
        server.verify();
    }

    @Test
    void serverErrorBecomesRagUnavailable() {
        server.expect(requestTo("http://rag.test/search")).andRespond(withServerError());

        assertThatThrownBy(() -> client.search("x", null)).isInstanceOf(RagUnavailableException.class);
    }
}
