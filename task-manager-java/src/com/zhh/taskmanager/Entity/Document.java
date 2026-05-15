package com.zhh.taskmanager.Entity;

import java.time.LocalDateTime;
import lombok.Data;
/**
 * 备考资料实体类
 */
@Data
public class Document {
    private Integer id;
    private Long userId;
    private String fileName;
    private String filePath;
    private String status; // PROCESSING, COMPLETED, FAILED
    private LocalDateTime uploadTime;
    private Integer notebookId; // 区分笔记本
    // 无参构造方法（必须有）
    public Document() {}

}