package com.zhh.taskmanager.service;

import com.zhh.taskmanager.model.User;
import com.zhh.taskmanager.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder; // 注入我们在 SecurityConfig 中配置的加密器

    @Override
    public boolean register(User user) {
        // 1. 检查用户名是否已存在
        if (userMapper.findByUsername(user.getUsername()) != null) {
            return false; // 用户名已存在，注册失败
        }

        // 2. 将明文密码加密为 BCrypt 密文
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword); // 替换为密文

        // 3. 存入数据库
        userMapper.insert(user);
        return true;
    }

    @Override
    public User login(String username, String rawPassword) {
        // 1. 根据用户名从数据库查出该用户
        User user = userMapper.findByUsername(username);

        // 2. 如果用户存在，验证密码
        if (user != null) {
            // 使用 matches 方法：将前端传来的明文(rawPassword)与数据库里的密文(user.getPassword())进行比对
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                return user; // 密码正确，返回用户信息
            }
        }
        return null; // 用户名不存在或密码错误
    }
}