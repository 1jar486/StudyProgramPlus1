package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.model.ChatMessage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ChatMessageMapper {
    // 保存一条对话记录，补全 sources 字段
    @Insert("INSERT INTO chat_message (notebook_id, role, content, sources) VALUES (#{notebookId}, #{role}, #{content}, #{sources})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatMessage message);

    // 查出某个笔记本下的所有对话历史，按时间正序排列（保证对话一问一答的顺序）
    @Select("SELECT * FROM chat_message WHERE notebook_id = #{notebookId} ORDER BY created_time ASC")
    List<ChatMessage> findByNotebookId(Integer notebookId);

    // 清空某个笔记本的对话记录（用于删除笔记本时级联删除）
    @Delete("DELETE FROM chat_message WHERE notebook_id = #{notebookId}")
    void deleteByNotebookId(@Param("notebookId") Integer notebookId);

    @Select("""
        SELECT * FROM (
            SELECT * FROM chat_message 
            WHERE notebook_id = #{notebookId} 
            ORDER BY created_time DESC 
            LIMIT 10
        ) sub 
        ORDER BY created_time ASC
    """)
    List<ChatMessage> findRecentHistoryByNotebookId(@Param("notebookId") Integer notebookId);
}