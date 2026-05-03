package com.zhh.taskmanager.mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import com.zhh.taskmanager.model.Document;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentMapper {

    // 插入文件记录到数据库
    @Insert("INSERT INTO document (user_id, file_name, file_path, status) " +
            "VALUES (#{userId}, #{fileName}, #{filePath}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Document document);

    // 查询某个用户上传的所有资料
    @Select("SELECT * FROM document WHERE user_id = #{userId} ORDER BY upload_time DESC")
    List<Document> findByUserId(Long userId);

    // 根据 ID 删除记录
    @Delete("DELETE FROM document WHERE id = #{id} AND user_id = #{userId}")
    void deleteById(@Param("id") Integer id, @Param("userId") Long userId);

    // 新增：根据 ID 更新文件的解析状态
    @org.apache.ibatis.annotations.Update("UPDATE document SET status = #{status} WHERE id = #{id}")
    void updateStatus(@org.apache.ibatis.annotations.Param("id") Integer id, @org.apache.ibatis.annotations.Param("status") String status);

    // 查询某用户下特定文件名的数量
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM document WHERE user_id = #{userId} AND file_name = #{fileName}")
    int countByUserIdAndFileName(@org.apache.ibatis.annotations.Param("userId") Long userId, @org.apache.ibatis.annotations.Param("fileName") String fileName);
}