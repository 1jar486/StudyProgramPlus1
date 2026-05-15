package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.Entity.Task;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TaskMapper {
    @Select("SELECT * FROM task WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Task> findByUserId(Long userId);

    // 2. 插入时增加 created_time 字段
    @Insert("INSERT INTO task (user_id, title, completed, priority, tag, memo, created_time) " +
            "VALUES (#{userId}, #{title}, #{completed}, #{priority}, #{tag}, #{memo}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Task task);

    // 3. 新增：专门更新备忘录的方法
    @Update("UPDATE task SET memo = #{memo} WHERE id = #{id} AND user_id = #{userId}")
    void updateMemo(@Param("id") int id, @Param("memo") String memo, @Param("userId") Long userId);

    // 3. 智能切换状态：修正了 MySQL SET 子句从左到右取新值的逻辑陷阱
    @Update("UPDATE task SET completed = NOT completed, " +
            "completed_time = CASE WHEN completed THEN NOW() ELSE NULL END " +
            "WHERE id = #{id} AND user_id = #{userId}")
    void toggleStatus(@Param("id") int id, @Param("userId") Long userId);

    @Delete("DELETE FROM task WHERE id = #{id} AND user_id = #{userId}")
    void deleteById(@Param("id") int id, @Param("userId") Long userId);

    // 4. 新增：前端无感行内编辑的核心接口（同时更新标题、优先级和标签）
    @Update("UPDATE task SET title = #{title}, priority = #{priority}, tag = #{tag} WHERE id = #{id} AND user_id = #{userId}")
    void updateCoreInfo(@Param("id") int id, @Param("title") String title, @Param("priority") String priority, @Param("tag") String tag, @Param("userId") Long userId);
}