package com.zhh.taskmanager.model;
/**
 * 用户实体类
 * 对应数据库中的 user 表
 */
public class User {
    private Long id; // 用户唯一ID
    private String username; // 用户名
    private String password; // 设置密码

    // set 和 get 方法
    public void setId(Long id) {
        this.id = id;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {return id;}
    public String getPassword(){
        return password;
    }
    public String getUsername() {
        return username;
    }
}
