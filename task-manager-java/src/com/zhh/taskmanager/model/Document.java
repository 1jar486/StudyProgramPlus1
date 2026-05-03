package com.zhh.taskmanager.model;

import java.time.LocalDateTime;

/**
 * 备考资料实体类
 */
public class Document {
    private Integer id;
    private Long userId;
    private String fileName;
    private String filePath;
    private String status; // PROCESSING, COMPLETED, FAILED
    private LocalDateTime uploadTime;

    // 无参构造方法（必须有）
    public Document() {}

    // 下面是所有的 Getter 和 Setter 方法
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
}