package com.zhh.taskmanager.service;
import com.zhh.taskmanager.model.ChatMessage;
import java.util.List;
import java.util.Map;

public interface ChatService {
    List<ChatMessage> getHistory(Integer notebookId);
    Map<String, Object>processChat(Integer notebookId, String query);
}