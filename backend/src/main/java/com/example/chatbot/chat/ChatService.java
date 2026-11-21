package com.example.chatbot.chat;

import com.example.chatbot.AiClient;
import com.example.chatbot.chat.ChatDtos.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private static final int HISTORY_LIMIT = 20;
    private static final String DEFAULT_TITLE = "New chat";

    private final Firestore db;
    private final AiClient ai;

    public ChatService(Firestore db, AiClient ai) {
        this.db = db;
        this.ai = ai;
    }

    public ChatSummary createChat(String uid) {
        DocumentReference ref = chats(uid).document();
        long now = System.currentTimeMillis();
        await(ref.set(Map.of("title", DEFAULT_TITLE, "createdAt", now, "updatedAt", now)));
        return new ChatSummary(ref.getId(), DEFAULT_TITLE, now);
    }

    public List<ChatSummary> listChats(String uid) {
        return await(chats(uid).orderBy("updatedAt", Query.Direction.DESCENDING).get())
                .getDocuments().stream()
                .map(d -> new ChatSummary(d.getId(), d.getString("title"), d.getLong("updatedAt")))
                .toList();
    }

    public List<MessageDto> messages(String uid, String chatId) {
        DocumentReference chat = requireChat(uid, chatId).getReference();
        return await(chat.collection("messages").orderBy("createdAt").get())
                .getDocuments().stream()
                .map(d -> new MessageDto(d.getId(), d.getString("role"),
                        d.getString("content"), d.getLong("createdAt")))
                .toList();
    }

    public MessageDto send(String uid, String chatId, String content) {
        DocumentSnapshot snap = requireChat(uid, chatId);
        DocumentReference chat = snap.getReference();
        CollectionReference msgs = chat.collection("messages");

        saveMessage(msgs, "user", content, System.currentTimeMillis());

        // Last N messages, newest first, then reversed into chronological order.
        List<QueryDocumentSnapshot> recent = await(
                msgs.orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(HISTORY_LIMIT).get()).getDocuments();
        List<AiClient.Msg> history = new ArrayList<>();
        for (int i = recent.size() - 1; i >= 0; i--) {
            QueryDocumentSnapshot d = recent.get(i);
            history.add(new AiClient.Msg(d.getString("role"), d.getString("content")));
        }

        String reply;
        try {
            reply = ai.generate(history);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service unavailable");
        }

        MessageDto saved = saveMessage(msgs, "assistant", reply, System.currentTimeMillis());

        Map<String, Object> updates = new HashMap<>();
        updates.put("updatedAt", saved.createdAt());
        if (DEFAULT_TITLE.equals(snap.getString("title"))) {
            updates.put("title", titleFrom(content));
        }
        await(chat.update(updates));
        return saved;
    }

    public void delete(String uid, String chatId) {
        DocumentReference chat = requireChat(uid, chatId).getReference();
        WriteBatch batch = db.batch();
        for (DocumentReference m : chat.collection("messages").listDocuments()) {
            batch.delete(m);
        }
        batch.delete(chat);
        await(batch.commit());
    }

    // ---------- helpers ----------

    private CollectionReference chats(String uid) {
        return db.collection("users").document(uid).collection("chats");
    }

    private DocumentSnapshot requireChat(String uid, String chatId) {
        DocumentSnapshot snap = await(chats(uid).document(chatId).get());
        if (!snap.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        }
        return snap;
    }

    private MessageDto saveMessage(CollectionReference msgs, String role, String content, long at) {
        DocumentReference ref = msgs.document();
        await(ref.set(Map.of("role", role, "content", content, "createdAt", at)));
        return new MessageDto(ref.getId(), role, content, at);
    }

    private String titleFrom(String text) {
        String t = text.strip();
        return t.length() <= 40 ? t : t.substring(0, 40) + "...";
    }

    private <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}