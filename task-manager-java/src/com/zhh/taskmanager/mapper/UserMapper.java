package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    // 根据用户名查找用户
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    // 插入新用户，并自动返回生成的 ID
    @Insert("INSERT INTO user (username, password) VALUES (#{username}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
}