package com.zhh.taskmanager.service;

import com.zhh.taskmanager.mapper.ChatMessageMapper;
import com.zhh.taskmanager.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ChatServiceImpl implements ChatService {
    private final ChatMessageMapper chatMessageMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public ChatServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<ChatMessage> getHistory(Integer notebookId) {
        return chatMessageMapper.findByNotebookId(notebookId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> processChat(Integer notebookId, String query) {
        List<ChatMessage> historyRecords = chatMessageMapper.findRecentHistoryByNotebookId(notebookId);
        List<Map<String, String>> historyList = new ArrayList<>();
        if (historyRecords != null) {
            for (ChatMessage record : historyRecords) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", record.getRole());
                msgMap.put("content", record.getContent());
                historyList.add(msgMap);
            }
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setNotebookId(notebookId);
        userMsg.setRole("user");
        userMsg.setContent(query);
        chatMessageMapper.insert(userMsg);

        Map<String, Object> pyRequest = new HashMap<>();
        pyRequest.put("query", query);
        pyRequest.put("notebook_id", notebookId);
        pyRequest.put("history", historyList);

        try {
            Map<String, Object> pyResponse = restTemplate.postForObject(
                    "http://localhost:8000/api/ai/chat", pyRequest, Map.class
            );

            if (pyResponse != null && pyResponse.containsKey("answer")) {
                String answer = (String) pyResponse.get("answer");
                List<Map<String, Object>> sources = (List<Map<String, Object>>) pyResponse.get("sources");

                // 将 sources 转为 JSON 字符串存入数据库
                ObjectMapper mapper = new ObjectMapper();
                String sourcesJson = mapper.writeValueAsString(sources);

                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setNotebookId(notebookId);
                aiMsg.setRole("ai");
                aiMsg.setContent(answer);
                aiMsg.setSources(sourcesJson); // 存入来源
                chatMessageMapper.insert(aiMsg);

                // 将双重数据返回给 Vue
                Map<String, Object> finalRes = new HashMap<>();
                finalRes.put("answer", answer);
                finalRes.put("sources", sources);
                return finalRes;
            }
            return Map.of("answer", "❌ AI 引擎没有返回有效的数据。");
        } catch (Exception e) {
            return Map.of("answer", "❌ 抱歉，大脑连接失败。");
        }
    }
}
