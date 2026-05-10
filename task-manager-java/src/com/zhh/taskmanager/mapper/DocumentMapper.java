package com.zhh.taskmanager.mapper;
import org.apache.ibatis.annotations.*;
import com.zhh.taskmanager.model.Document;

import java.util.List;

@Mapper
public interface DocumentMapper {
    // 1. 【修改插入方法】在 SQL 中加入 notebook_id
    @Insert("INSERT INTO document (user_id, notebook_id, file_name, file_path, status) " +
            "VALUES (#{userId}, #{notebookId}, #{fileName}, #{filePath}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Document document);

    // 2. 【修改查询方法】根据笔记本 ID 查询文档，而不是单凭用户 ID
    @Select("SELECT * FROM document WHERE user_id = #{userId} AND notebook_id = #{notebookId} ORDER BY upload_time DESC")
    List<Document> findByNotebookId(@Param("userId") Long userId, @Param("notebookId") Integer notebookId);

    // 3. 【新增方法】删除整个笔记本时，级联删除里面的所有文档记录
    @Delete("DELETE FROM document WHERE notebook_id = #{notebookId} AND user_id = #{userId}")
    void deleteByNotebookId(@Param("notebookId") Integer notebookId, @Param("userId") Long userId);

    // 根据 ID 删除记录
    @Delete("DELETE FROM document WHERE id = #{id} AND user_id = #{userId}")
    void deleteById(@Param("id") Integer id, @Param("userId") Long userId);

    // 新增：根据 ID 更新文件的解析状态
    @org.apache.ibatis.annotations.Update("UPDATE document SET status = #{status} WHERE id = #{id}")
    void updateStatus(@org.apache.ibatis.annotations.Param("id") Integer id, @org.apache.ibatis.annotations.Param("status") String status);

    // 查询某用户下特定文件名的数量
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM document WHERE user_id = #{userId} AND file_name = #{fileName}")
    int countByUserIdAndFileName(@org.apache.ibatis.annotations.Param("userId") Long userId, @org.apache.ibatis.annotations.Param("fileName") String fileName);

    // 在 DocumentMapper 接口里新增这个方法
    @Select("SELECT * FROM document WHERE id = #{id} AND user_id = #{userId}")
    Document findByIdAndUserId(@Param("id") Integer id, @Param("userId") Long userId);

    // 在 DocumentMapper.java 中添加
    @Update("UPDATE document SET status = #{status} WHERE file_path = #{filePath}")
    void updateStatusByPath(@Param("filePath") String filePath, @Param("status") String status);
}