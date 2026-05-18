package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.Entity.Notebook;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NotebookMapper {
    // 插入新笔记本
    @Insert("INSERT INTO notebook (user_id, name) VALUES (#{userId}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Notebook notebook);

    // 查询某用户的所有笔记本，按创建时间倒序
    @Select("SELECT * FROM notebook WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Notebook> findByUserId(Long userId);

    // 重命名笔记本
    @Update("UPDATE notebook SET name = #{name} WHERE id = #{id} AND user_id = #{userId}")
    void updateName(@Param("id") Integer id, @Param("name") String name, @Param("userId") Long userId);

    // 删除笔记本
    @Delete("DELETE FROM notebook WHERE id = #{id} AND user_id = #{userId}")
    void deleteById(@Param("id") Integer id, @Param("userId") Long userId);


}