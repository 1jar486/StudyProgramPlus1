package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.model.Task;
import com.zhh.taskmanager.service.TaskService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    private Long getUserIdFromToken(String token) {
        return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
    }

    @GetMapping
    public List<Task> getAll(@RequestHeader("Authorization") String token) {
        return service.getTasksByUserId(getUserIdFromToken(token));
    }

    @PostMapping
    public void add(@RequestBody Task task, @RequestHeader("Authorization") String token) {
        task.setUserId(getUserIdFromToken(token));
        service.addTask(task);
    }

    @PutMapping("/{id}")
    public void toggle(@PathVariable int id, @RequestHeader("Authorization") String token) {
        service.completeTask(id, getUserIdFromToken(token));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id, @RequestHeader("Authorization") String token) {
        service.deleteTask(id, getUserIdFromToken(token));
    }

    @PutMapping("/{id}/memo")
    public void updateMemo(@PathVariable int id, @RequestBody Map<String, String> payload, @RequestHeader("Authorization") String token) {
        // 从 Token 解析出用户 ID
        Long userId = getUserIdFromToken(token);
        // 从请求体中提取 memo 字符串
        String memo = payload.get("memo");
        // 调用 Service 层业务逻辑
        service.updateTaskMemo(id, memo, userId);
    }

    // 新增：前端行内编辑专属接口
    @PutMapping("/{id}/core")
    public void updateCoreInfo(@PathVariable int id, @RequestBody Map<String, String> payload, @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        String title = payload.get("title");
        String priority = payload.get("priority");
        String tag = payload.get("tag"); // [新增] 获取前端传来的 tag
        service.updateTaskCore(id, title, priority, tag, userId);
    }
}
