package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.Entity.ChatSession;
import com.zhh.taskmanager.Entity.ChatMessage;
import com.zhh.taskmanager.mapper.ChatSessionMapper;
import com.zhh.taskmanager.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map; // 通常已经导入过了
import java.util.List;

@RestController
@RequestMapping("/api/chat-sessions")
@CrossOrigin // 🌟 极其重要：允许 Vue 跨域调用
public class ChatSessionController {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    // ================= 【解析 Token 的辅助方法】 =================
    private Long getUserIdFromToken(String token) {
        try {
            return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
        } catch (Exception e) {
            throw new RuntimeException("身份验证失败，请重新登录");
        }
    }
    // ==========================================================

    // 1. 获取左侧下拉框的历史列表 (GET)
    @GetMapping("/notebook/{notebookId}")
    public ResponseEntity<List<ChatSession>> getSessions(
            @PathVariable Integer notebookId,
            @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);
        return ResponseEntity.ok(chatSessionMapper.findByNotebookId(notebookId));
    }

    // 2. 点击“开启新对话”时，创建一个新 Session (POST)
    @PostMapping("/notebook/{notebookId}")
    public ResponseEntity<ChatSession> createSession(
            @PathVariable Integer notebookId,
            @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);
        ChatSession session = new ChatSession();
        session.setNotebookId(notebookId);
        session.setTitle("新的探讨");
        chatSessionMapper.insert(session);
        return ResponseEntity.ok(session); // 返回包含生成 ID 的完整实体
    }

    // 3. 点击某个历史标题时，拉取它的所有完整聊天记录 (GET)
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getSessionMessages(
            @PathVariable Integer sessionId,
            @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);
        return ResponseEntity.ok(chatMessageMapper.findAllBySessionId(sessionId));
    }

    // ================== 【本次新增：三个控制台接口】 ==================

    // 4. 重命名会话 (PUT)
    @PutMapping("/{sessionId}/rename")
    public ResponseEntity<?> renameSession(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, String> payload,
            @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token); // 简单鉴权 [cite: 74]
        String newTitle = payload.get("title");
        chatSessionMapper.updateTitle(sessionId, newTitle); // 复用你原有的方法 [cite: 216]
        return ResponseEntity.ok().build();
    }

    // 5. 置顶/取消置顶 (PUT)
    @PutMapping("/{sessionId}/pin")
    public ResponseEntity<?> togglePinSession(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, Integer> payload,
            @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);
        Integer isPinned = payload.get("isPinned"); // 接收前端传来的 0 或 1
        chatSessionMapper.updatePinned(sessionId, isPinned);
        return ResponseEntity.ok().build();
    }

    // 6. 物理销毁会话及记录 (DELETE)
    @DeleteMapping("/{sessionId}")
    @Transactional // 开启事务，保证消息和会话壳子同时被销毁
    public ResponseEntity<?> deleteSession(
            @PathVariable Integer sessionId,
            @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);

        // 🚨 顺序极其重要：必须先删消息子表，再删会话主表
        chatMessageMapper.deleteBySessionId(sessionId);
        chatSessionMapper.deleteById(sessionId);

        return ResponseEntity.ok().build();
    }
}