package com.atcsm.java.ai.langchain4j.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class MCPClient {

    private static final Logger log = LoggerFactory.getLogger(MCPClient.class);

    @Value("${mcp.server.url}")
    private String mcpServerUrl;

    private final WebClient webClient;

    public MCPClient() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Map<String, Object> call(String tool, Map<String, Object> parameters, Map<String, String> credentials) {
        Map<String, Object> request = Map.of(
                "tool", tool,
                "parameters", parameters,
                "credentials", credentials
        );

        try {
            return webClient.post()
                    .uri(mcpServerUrl + "/mcp/call")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("MCP调用失败: tool={}, error={}", tool, e.getMessage());
            throw new RuntimeException("MCP调用失败: " + e.getMessage());
        }
    }
}
