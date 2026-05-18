package com.zhh.taskmanager.service;

import com.zhh.taskmanager.Entity.ChatMessage;
import java.util.List;
import java.util.Map;

public interface ChatService {
    // 保留你原有的接口，兼容旧逻辑
    List<ChatMessage> getHistory(Integer notebookId);

    // 🌟 核心升级：为 processChat 新增 sessionId 参数
    Map<String, Object> processChat(Integer notebookId, Integer sessionId, String query);
}