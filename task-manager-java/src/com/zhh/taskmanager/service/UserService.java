package com.zhh.taskmanager.service;

import com.zhh.taskmanager.Entity.User;

public interface UserService {

    /**
     * 注册方法
     * @return true 代表注册成功，false 代表用户名已存在
     */
    boolean register(User user);

    /**
     * 登录方法
     * @return 验证成功返回完整的 User 对象（包含数据库 ID），失败返回 null
     */
    User login(String username, String password);

    // 返回值类型为什么是Boolean和user?

}
