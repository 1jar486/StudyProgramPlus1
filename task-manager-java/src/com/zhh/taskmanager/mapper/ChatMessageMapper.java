package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.Entity.ChatMessage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ChatMessageMapper {

    // ================== 【保留你原有的旧方法】 ==================
    @Insert("INSERT INTO chat_message (notebook_id, role, content, sources) VALUES (#{notebookId}, #{role}, #{content}, #{sources})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatMessage message);

    @Select("SELECT * FROM chat_message WHERE notebook_id = #{notebookId} ORDER BY id ASC")
    List<ChatMessage> findByNotebookId(Integer notebookId);

    @Select("SELECT * FROM chat_message WHERE notebook_id = #{notebookId} ORDER BY id DESC LIMIT 10")
    List<ChatMessage> findRecentHistoryByNotebookId(Integer notebookId);

    // 👇 🌟 被我误删的级联删除方法，现在给它请回来了！
    @Delete("DELETE FROM chat_message WHERE notebook_id = #{notebookId}")
    void deleteByNotebookId(@Param("notebookId") Integer notebookId);


    // ================== 【本次新增：支持 Notion 多会话的核心方法】 ==================

    // 1. 插入一条带有 sessionId 的新消息
    @Insert("INSERT INTO chat_message (notebook_id, session_id, role, content, sources) VALUES (#{notebookId}, #{sessionId}, #{role}, #{content}, #{sources})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMessage(ChatMessage message);

    // 2. 截断术：只查当前会话下的最新 10 条（防大模型爆仓报错）
    @Select("SELECT * FROM (" +
            "  SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY id DESC LIMIT 10" +
            ") sub ORDER BY id ASC")
    List<ChatMessage> findRecentHistoryBySessionId(Integer sessionId);

    // 3. 查出某个历史会话的所有完整聊天记录（用于前端点开历史面板时展示全量数据）
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY id ASC")
    List<ChatMessage> findAllBySessionId(Integer sessionId);

    // ================== 【新增：定期清理专属方法 (完美版)】 ==================
    @Delete("DELETE cm FROM chat_message cm " +
            "INNER JOIN chat_session cs ON cm.session_id = cs.id " +
            "WHERE IFNULL(cs.updated_at, cs.created_at) < DATE_SUB(NOW(), INTERVAL 3 MONTH)")
    void deleteMessagesOfOldSessions();

    // ================== 【本次新增：级联删除消息】 ==================
    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    void deleteBySessionId(Integer sessionId);
}