package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.model.ChatMessage;
import com.zhh.taskmanager.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {
    @Autowired private ChatService chatService;

    // 拉取历史记录
    @GetMapping("/{notebookId}")
    public List<ChatMessage> getHistory(@PathVariable Integer notebookId) {
        return chatService.getHistory(notebookId);
    }

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, Object> payload) {
        Integer notebookId = (Integer) payload.get("notebookId");
        String query = (String) payload.get("query");
        return chatService.processChat(notebookId, query);
    }
}