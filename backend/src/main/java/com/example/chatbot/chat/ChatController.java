package com.example.chatbot.chat;

import com.example.chatbot.CurrentUser;
import com.example.chatbot.chat.ChatDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService service;
    private final CurrentUser user;

    public ChatController(ChatService service, CurrentUser user) {
        this.service = service;
        this.user = user;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSummary create() {
        return service.createChat(user.id());
    }

    @GetMapping
    public List<ChatSummary> list() {
        return service.listChats(user.id());
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> messages(@PathVariable String id) {
        return service.messages(user.id(), id);
    }

    @PostMapping("/{id}/messages")
    public MessageDto send(@PathVariable String id, @Valid @RequestBody SendMessageRequest req) {
        return service.send(user.id(), id, req.content());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(user.id(), id);
    }
}