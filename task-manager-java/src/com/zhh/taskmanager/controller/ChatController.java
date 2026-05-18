package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.Entity.ChatMessage;
import com.zhh.taskmanager.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin // 🌟 必须保留，确保前端 Vue 能跨域访问
public class ChatController {

    @Autowired
    private ChatService chatService;

    // ================= 【解析 Token 的辅助方法】 =================
    private Long getUserIdFromToken(String token) {
        try {
            return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
        } catch (Exception e) {
            throw new RuntimeException("身份验证失败，请重新登录");
        }
    }
    // ==========================================================

    /**
     * 拉取旧版历史记录 (GET 请求)
     */
    @GetMapping("/{notebookId}")
    public List<ChatMessage> getHistory(
            @PathVariable Integer notebookId,
            @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token); // 验证登录
        return chatService.getHistory(notebookId);
    }

    /**
     * 🌟 发起 AI 对话 (POST 请求) - 已接入多线程 Session 机制
     */
    @PostMapping
    public Map<String, Object> chat(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String token) {

        getUserIdFromToken(token); // 验证登录

        Integer notebookId = (Integer) payload.get("notebookId");
        // 🌟 核心：接收前端传来的 sessionId (如果是第一次对话，前端会先去创建 Session 然后把 ID 传过来)
        Integer sessionId = (Integer) payload.get("sessionId");
        String query = (String) payload.get("query");

        // 调用 Service 层的 processChat
        return chatService.processChat(notebookId, sessionId, query);
    }
}