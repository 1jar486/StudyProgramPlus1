package com.zhh.taskmanager.Entity;
import lombok.Data;
/**
 * 用户实体类
 * 对应数据库中的 user 表
 */
@Data
public class User {
    private Long id; // 用户唯一ID
    private String username; // 用户名
    private String password; // 设置密码

}
