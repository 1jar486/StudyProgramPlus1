package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.mapper.DocumentMapper;
import com.zhh.taskmanager.Entity.Document;
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

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

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

    // 1. 获取特定笔记本下的知识库列表
    @GetMapping
    public List<Document> getAllMyDocuments(
            @RequestParam Integer notebookId, // 【新增】必须传入笔记本ID
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        // 【修改】调用 DocumentMapper 中新加的按笔记本查询的方法
        return documentMapper.findByNotebookId(userId, notebookId);
    }

    // 2. 接收前端上传的文件
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("notebookId") Integer notebookId, // 【新增】接收前端传来的笔记本ID
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

            // --- 【重复文件拦截】 ---
            if (documentMapper.countByUserIdAndFileName(userId, originalFilename) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("文件已存在，请勿重复上传！");
            }

            String filePath = UPLOAD_DIR + System.currentTimeMillis() + "_" + originalFilename;

            // 将文件真实保存到电脑硬盘上
            File destFile = new File(filePath);
            file.transferTo(destFile);

            // 构建 Document 对象，保存信息到 MySQL 数据库
            Document doc = new Document();
            doc.setUserId(userId);
            doc.setNotebookId(notebookId); // 【新增】将文档绑定到具体的笔记本
            doc.setFileName(originalFilename);
            doc.setFilePath(filePath);
            doc.setStatus("PROCESSING");
            documentMapper.insert(doc);

            // 【关键修改】开启新线程去后台调用 Python，并带上 notebook_id
            new Thread(() -> {
                // 组装发给 Python FastAPI 的 JSON 数据
                Map<String, Object> pyRequest = new HashMap<>();
                pyRequest.put("file_path", filePath);
                pyRequest.put("notebook_id", notebookId); // 让 Python 知道该存入哪个向量集合

                try {
                    restTemplate.postForObject("http://localhost:8000/api/ai/parse", pyRequest, String.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return ResponseEntity.ok("文件上传成功，专属知识库正在光速解析...");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败，服务器开小差了");
        }
    }

    // 替换 DocumentController.java 中的 updateStatus 方法
    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestBody Map<String, String> payload) {
        // Python 传过来的是 {"file_path": "D:/...", "status": "COMPLETED"}
        String filePath = payload.get("file_path");

        // 我们假设回执默认就是成功，也可以让 Python 传具体的 status
        String status = payload.getOrDefault("status", "COMPLETED");

        System.out.println("收到 AI 引擎回执，更新文件状态为完成: " + filePath);

        // 调用 Mapper 更新数据库，这样前端轮询时就能查到 COMPLETED 了
        documentMapper.updateStatusByPath(filePath, status);

        return ResponseEntity.ok("状态已同步");
    }

    // 3. 删除文件（实现 MySQL 与 ChromaDB 的完美双向销毁）
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        // 🌟 承接前端 request.js 翻译后的老格式 Token，这里绝对不会再报错
        Long userId = getUserIdFromToken(token);

        // 第一步：从数据库把这个文件的信息查出来，获取绝对路径和所在的笔记本ID
        // 保持你原有的 Mapper 命名，确保 0 编译错误
        Document doc = documentMapper.findByIdAndUserId(id, userId);

        if (doc != null) {
            // 第二步：异步通知 Python AI 引擎，进行“物理级全面销毁”（不阻塞前端UI，体验极佳）
            new Thread(() -> {
                Map<String, Object> pyRequest = new HashMap<>();
                pyRequest.put("file_path", doc.getFilePath()); // 核心锚点：利用文件路径去删向量
                pyRequest.put("notebook_id", doc.getNotebookId());

                try {
                    // 1. 发送 HTTP 请求，让 Python 端同时融毁 Chroma 向量和磁盘上的物理文件
                    restTemplate.postForObject("http://localhost:8000/api/ai/delete", pyRequest, String.class);

                    // 2. 🌟【关键移除】：这里不再由 Java 亲自执行 physicalFile.delete()
                    // 物理文件清除的大任已经交由更安全的 Python 引擎闭环处理，彻底避免双端抢夺资源的冲突
                    System.out.println("🚀 星云全链路垃圾回收：已成功给 Python 下达融毁指令，文件路径: " + doc.getFilePath());

                } catch (Exception e) {
                    System.err.println("❌ 调用 Python 销毁向量失败：" + e.getMessage());
                }
            }).start();

            // 第三步：在 MySQL 数据库中同步抹除它的存在
            documentMapper.deleteById(id, userId);

            return ResponseEntity.ok("文件及底层关联向量已彻底清除！");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到该文件");
        }
    }

    // 4. 安全预览文件流接口
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewDocument(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);

        // 校验文件归属权
        Document doc = documentMapper.findByIdAndUserId(id, userId);
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        File file = new File(doc.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 将物理文件包装为资源
        Resource resource = new FileSystemResource(file);

        // 判断文件类型，通知浏览器如何渲染
        String contentType = doc.getFileName().toLowerCase().endsWith(".pdf")
                ? "application/pdf"
                : "text/plain;charset=UTF-8";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                // inline 表示让浏览器尝试直接预览，而不是下载
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }


}