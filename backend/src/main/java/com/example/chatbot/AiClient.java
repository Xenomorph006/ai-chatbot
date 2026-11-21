package com.example.chatbot;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiClient {

    public record Msg(String role, String content) {}
    public record GenerateRequest(List<Msg> messages) {}
    public record GenerateResponse(String reply) {}

    private final RestClient http;

    public AiClient(@Value("${ai.service.url}") String url,
                    @Value("${ai.service.key}") String key) {
        this.http = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("x-service-key", key)
                .build();
    }

    public String generate(List<Msg> messages) {
        GenerateResponse res = http.post()
                .uri("/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GenerateRequest(messages))
                .retrieve()
                .body(GenerateResponse.class);
        return res.reply();
    }
}