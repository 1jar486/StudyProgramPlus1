package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.mapper.DocumentMapper;
import com.zhh.taskmanager.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin // 允许跨域，方便你的 Vue 前端调用
public class DocumentController {

    @Autowired
    private DocumentMapper documentMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    // 定义文件保存在本地的绝对路径（你可以根据需要修改盘符）
    private final String UPLOAD_DIR = "D:/StudyProgramPlus_Uploads/";

    // 解析 Token 获取用户 ID 的辅助方法 (复用你之前写任务看板的逻辑)
    private Long getUserIdFromToken(String token) {
        return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
    }

    // 1. 获取我的知识库列表
    @GetMapping
    public List<Document> getAllMyDocuments(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return documentMapper.findByUserId(userId);
    }

    // 2. 接收前端上传的文件
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {

        Long userId = getUserIdFromToken(token);

        // 如果文件为空，直接打回
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("不能上传空文件哦！");
        }

        try {
            // 确保本地上传文件夹存在，没有就自动创建
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 获取原始文件名并拼接保存路径
            String originalFilename = file.getOriginalFilename();
            // --- 【新增：重复文件拦截】 ---
            if (documentMapper.countByUserIdAndFileName(userId, originalFilename) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("文件已存在，请勿重复上传！");
            }
            // --------------------------
            String filePath = UPLOAD_DIR + System.currentTimeMillis() + "_" + originalFilename; // 加时间戳防止重名覆盖

            // 将文件真实保存到电脑硬盘上
            File destFile = new File(filePath);
            file.transferTo(destFile);

            // 构建 Document 对象，保存信息到 MySQL 数据库
            Document doc = new Document();
            doc.setUserId(userId);
            doc.setFileName(originalFilename);
            doc.setFilePath(filePath);
            doc.setStatus("PROCESSING"); // 状态设为解析中，等后面交给 Python 处理

            documentMapper.insert(doc);

            // 获取刚刚存入数据库的这条记录的 ID
            final Integer docId = doc.getId();

            // 【关键】开启一个新线程去后台调用 Python，让前端立刻显示“待解析”
            new Thread(() -> {
                String pythonUrl = "http://localhost:8000/api/ai/parse";
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("file_path", filePath);

                try {
                    // 等待 Python 慢慢解析...
                    restTemplate.postForEntity(pythonUrl, requestMap, String.class);
                    System.out.println("Python 解析完成，更新数据库: " + filePath);

                    // Python 解析成功后，把数据库状态改成 COMPLETED
                    documentMapper.updateStatus(docId, "COMPLETED");
                } catch (Exception e) {
                    System.err.println("解析失败: " + e.getMessage());
                    // 如果失败了，标为 FAILED
                    documentMapper.updateStatus(docId, "FAILED");
                }
            }).start();

            return ResponseEntity.ok("文件上传成功，AI 正在后台光速解析...");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败，服务器开小差了");
        }
    }

    // 3. 接收 Python 的回执，更新文件解析状态
    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestBody Map<String, String> payload) {
        String filePath = payload.get("file_path");
        // 这里简单演示逻辑，实际建议在 Mapper 增加根据路径修改状态的 SQL
        System.out.println("收到回执，文件解析完成: " + filePath);
        // documentMapper.updateStatusByPath(filePath, "COMPLETED");
        return ResponseEntity.ok("状态已同步");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        documentMapper.deleteById(id, userId);
        return ResponseEntity.ok("文件记录已清理");
    }
}