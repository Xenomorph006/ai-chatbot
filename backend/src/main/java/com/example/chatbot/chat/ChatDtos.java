package com.example.chatbot.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ChatDtos {
    private ChatDtos() {}

    public record ChatSummary(String id, String title, long updatedAt) {}

    public record MessageDto(String id, String role, String content, long createdAt) {}

    public record SendMessageRequest(@NotBlank @Size(max = 4000) String content) {}
}