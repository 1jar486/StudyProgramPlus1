package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.Entity.ChatMessage;
import com.zhh.taskmanager.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin // 允许跨域
public class ChatController {

    @Autowired
    private ChatService chatService;

    // ================= 【核心修复：引入 Token 解析方法】 =================
    private Long getUserIdFromToken(String token) {
        try {
            return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
        } catch (Exception e) {
            throw new RuntimeException("身份验证失败，请重新登录");
        }
    }
    // =================================================================

    /**
     * [cite_start]拉取历史记录 - 增加 Token 校验拦截[span_0](end_span)
     */
    @GetMapping("/{notebookId}")
    public List<ChatMessage> getHistory(
            @PathVariable Integer notebookId,
            @RequestHeader("Authorization") String token) { // 👈 强制要求请求头包含 Token

        getUserIdFromToken(token); // 验证用户是否登录

        return chatService.getHistory(notebookId);
    }

    /**
     * [span_1](start_span)[span_2](start_span)发起 AI 对话 - 增加 Token 校验拦截 [cite: 43-44]
     */
    @PostMapping
    public Map<String, Object> chat(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String token) { // 👈 强制要求请求头包含 Token

        getUserIdFromToken(token); // 验证用户是否登录

        Integer notebookId = (Integer) payload.get("notebookId");
        String query = (String) payload.get("query");

        return chatService.processChat(notebookId, query);
    }
}