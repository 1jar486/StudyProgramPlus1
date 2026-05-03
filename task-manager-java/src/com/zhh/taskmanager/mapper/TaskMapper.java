package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.model.Task;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TaskMapper {
    // 1. 修改查询：SQL 自动映射到 Task 对象的 memo 属性
    @Select("SELECT * FROM task WHERE user_id = #{userId}")
    List<Task> findByUserId(Long userId);

    // 2. 修改插入：增加 memo 字段
    @Insert("INSERT INTO task (user_id, title, completed, priority, tag, memo) " +
            "VALUES (#{userId}, #{title}, #{completed}, #{priority}, #{tag}, #{memo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Task task);

    // 3. 新增：专门更新备忘录的方法
    @Update("UPDATE task SET memo = #{memo} WHERE id = #{id} AND user_id = #{userId}")
    void updateMemo(@Param("id") int id, @Param("memo") String memo, @Param("userId") Long userId);

    @Update("UPDATE task SET completed = NOT completed WHERE id = #{id} AND user_id = #{userId}")
    void toggleStatus(@Param("id") int id, @Param("userId") Long userId);

    @Delete("DELETE FROM task WHERE id = #{id} AND user_id = #{userId}")
    void deleteById(@Param("id") int id, @Param("userId") Long userId);
}