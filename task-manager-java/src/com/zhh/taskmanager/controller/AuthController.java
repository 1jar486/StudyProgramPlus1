package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.Entity.User;
import com.zhh.taskmanager.service.UserService;
import com.zhh.taskmanager.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginDto) {
        String username = loginDto.get("username");
        String password = loginDto.get("password");

        User user = userService.login(username, password);
        Map<String, Object> response = new HashMap<>();

        if (user != null) {
            String accessToken = jwtUtils.generateAccessToken(username, user.getId());
            String refreshToken = jwtUtils.generateRefreshToken(username, user.getId());

            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("userId", user.getId()); // 🌟 核心新增：直接把用户ID顺便带给前端
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // 2. 注册方法：修正了调用逻辑
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("注册失败：用户名不能为空");
        }

        // 核心修正点：确保这里传入的是整个对象，且与 UserService 接口对齐
        boolean isSuccess = userService.register(user);

        if (isSuccess) {
            return ResponseEntity.ok("注册成功");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("注册失败：用户名已存在");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> refreshDto) {
        String refreshToken = refreshDto.get("refreshToken");
        Map<String, Object> response = new HashMap<>();

        if (refreshToken == null || refreshToken.isEmpty()) {
            response.put("message", "缺少 Refresh Token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            String username = jwtUtils.getUsernameFromToken(refreshToken);
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);

            String newAccessToken = jwtUtils.generateAccessToken(username, userId);
            String newRefreshToken = jwtUtils.generateRefreshToken(username, userId);

            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("userId", userId); // 🌟 核心新增：刷新时也将用户ID同步带回
            return ResponseEntity.ok(response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            response.put("message", "会话已过期，请重新登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("message", "无效的验证凭证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
