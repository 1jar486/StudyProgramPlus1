package com.zhh.taskmanager.service;

import com.zhh.taskmanager.mapper.ChatMessageMapper;
import com.zhh.taskmanager.mapper.ChatSessionMapper;
import com.zhh.taskmanager.Entity.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageMapper chatMessageMapper;

    // 🌟 1. 新增注入：用来修改会话标题
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public ChatServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    // 保留你原有的老方法
    @Override
    public List<ChatMessage> getHistory(Integer notebookId) {
        return chatMessageMapper.findByNotebookId(notebookId);
    }

    // 🌟 2. 升级核心方法：加上 sessionId
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> processChat(Integer notebookId, Integer sessionId, String query) {

        // 🌟 3. 利用"截断术"去取历史记录（只取当前会话的最新 10 条，大模型永远不会 OOM）
        List<ChatMessage> historyRecords = chatMessageMapper.findRecentHistoryBySessionId(sessionId);
        List<Map<String, String>> historyList = new ArrayList<>();

        // 🌟 4. 【Notion灵魂细节】如果是这个会话的第一句话，自动用用户问题作为标题！
        if (historyRecords == null || historyRecords.isEmpty()) {
            String newTitle = query.length() > 15 ? query.substring(0, 15) + "..." : query;
            chatSessionMapper.updateTitle(sessionId, newTitle);
        } else {
            // 装填有限的截断上下文
            for (ChatMessage record : historyRecords) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", record.getRole());
                msgMap.put("content", record.getContent());
                historyList.add(msgMap);
            }
        }

        // 5. 将当前用户的提问落库
        ChatMessage userMsg = new ChatMessage();
        userMsg.setNotebookId(notebookId);
        userMsg.setSessionId(sessionId); // 🌟 核心绑定：这条消息属于哪个历史面板
        userMsg.setRole("user");
        userMsg.setContent(query);
        // 使用刚刚在 Mapper 里新写的 insertMessage 方法（包含sessionId）
        chatMessageMapper.insertMessage(userMsg);
        // 【新增】更新当前会话的活跃时间，保证下拉列表排序正确
        chatSessionMapper.updateSessionTime(sessionId);
        // 6. 组装给 Python (Langchain) 的请求包
        Map<String, Object> pyRequest = new HashMap<>();
        pyRequest.put("query", query);
        pyRequest.put("notebook_id", notebookId);
        pyRequest.put("history", historyList); // 这个 history 已经被截断，安全送达！

        try {
            Map<String, Object> pyResponse = restTemplate.postForObject(
                    "http://localhost:8000/api/ai/chat", pyRequest, Map.class
            );

            if (pyResponse != null && pyResponse.containsKey("answer")) {
                String answer = (String) pyResponse.get("answer");
                List<Map<String, Object>> sources = (List<Map<String, Object>>) pyResponse.get("sources");

                ObjectMapper mapper = new ObjectMapper();
                String sourcesJson = mapper.writeValueAsString(sources);

                // AI 回复落库
                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setNotebookId(notebookId);
                aiMsg.setSessionId(sessionId); // 🌟 核心绑定
                aiMsg.setRole("ai");
                aiMsg.setContent(answer);
                aiMsg.setSources(sourcesJson);
                chatMessageMapper.insertMessage(aiMsg);

                Map<String, Object> finalRes = new HashMap<>();
                finalRes.put("answer", answer);
                finalRes.put("sources", sources);
                return finalRes;
            }
            return Map.of("answer", "❌ AI 引擎没有返回有效的数据。");
        } catch (Exception e) {
            e.printStackTrace(); // 建议加上报错打印，方便以后排查Python连接问题
            return Map.of("answer", "❌ 抱歉，大脑连接失败。");
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Cron表达式：每天凌晨 3:00:00 触发
    @Transactional // 开启事务：要么全删成功，要么都不删，保证数据绝对一致
    public void autoCleanOldChatRecords() {
        System.out.println("🧹 [自动清理] 启动深空探测：开始扫描 3 个月前的沉睡对话数据...");
        try {
            // 🚨 极其重要的顺序：必须先删子表（Message），再删主表（Session）！
            // 1. 抹除过期会话名下的所有聊天记录
            chatMessageMapper.deleteMessagesOfOldSessions();

            // 2. 抹除过期会话的标题外壳
            chatSessionMapper.deleteOldSessions();

            System.out.println("✅ [自动清理] 沉睡超过 3 个月的对话与记录已全部物理销毁！");
        } catch (Exception e) {
            System.err.println("❌ [自动清理] 销毁过期对话失败，错误日志：" + e.getMessage());
            // 由于加了 @Transactional，一旦这里报错，前面的删除会自动回滚，确保数据库安全
        }
    }
}