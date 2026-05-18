package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.Entity.ChatSession;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ChatSessionMapper {

    // 插入新会话，并回填自增 ID 给前端
    @Insert("INSERT INTO chat_session (notebook_id, title) VALUES (#{notebookId}, #{title})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatSession session);

    // 修改原有的 findByNotebookId 查询语句 [cite: 215]
    @Select("SELECT * FROM chat_session WHERE notebook_id = #{notebookId} ORDER BY is_pinned DESC, updated_at DESC")
    List<ChatSession> findByNotebookId(Integer notebookId);

    // 第一句话触发时，动态更新会话标题（Notion的灵魂细节）
    @Update("UPDATE chat_session SET title = #{title} WHERE id = #{id}")
    void updateTitle(@Param("id") Integer id, @Param("title") String title);

    @Delete("DELETE FROM chat_session WHERE notebook_id = #{notebookId}")
    void deleteByNotebookId(Integer notebookId);

    @Update("UPDATE chat_session SET updated_at = NOW() WHERE id = #{id}")
    void updateSessionTime(Integer id);

    // ================== 【新增：定期清理专属方法 (完美版)】 ==================
    @Delete("DELETE FROM chat_session " +
            "WHERE IFNULL(updated_at, created_at) < DATE_SUB(NOW(), INTERVAL 3 MONTH)")
    void deleteOldSessions();

    // ================== 【本次新增：置顶与删除】 ==================
    // 更新置顶状态
    @Update("UPDATE chat_session SET is_pinned = #{isPinned} WHERE id = #{id}")
    void updatePinned(@Param("id") Integer id, @Param("isPinned") Integer isPinned);

    // 删除会话本体
    @Delete("DELETE FROM chat_session WHERE id = #{id}")
    void deleteById(Integer id);
}