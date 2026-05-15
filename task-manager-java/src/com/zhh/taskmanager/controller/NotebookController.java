package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.Entity.Notebook;
import com.zhh.taskmanager.service.NotebookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notebooks")
@CrossOrigin
public class NotebookController {
    @Autowired private NotebookService notebookService;

    private Long getUserIdFromToken(String token) {
        return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
    }

    @GetMapping
    public List<Notebook> getAll(@RequestHeader("Authorization") String token) {
        return notebookService.getNotebooks(getUserIdFromToken(token));
    }

    @PostMapping
    public Notebook create(@RequestBody Map<String, String> payload, @RequestHeader("Authorization") String token) {
        return notebookService.createNotebook(getUserIdFromToken(token), payload.get("name"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> rename(@PathVariable Integer id, @RequestBody Map<String, String> payload, @RequestHeader("Authorization") String token) {
        notebookService.renameNotebook(id, payload.get("name"), getUserIdFromToken(token));
        return ResponseEntity.ok("重命名成功");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        notebookService.deleteNotebook(id, getUserIdFromToken(token));
        return ResponseEntity.ok("笔记本及关联数据已删除");
    }
}