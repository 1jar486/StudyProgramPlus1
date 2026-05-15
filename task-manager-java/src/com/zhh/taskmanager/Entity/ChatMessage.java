package com.zhh.taskmanager.Entity;
import java.time.LocalDateTime;
import lombok.Data; // Lombok库自动生成get和set方法，还默认写了toString()方法,方便在控制台打印日志查看对象内容

@Data
public class ChatMessage {
    private Integer id;
    private Integer notebookId;
    private String role; // "user" 或 "ai"
    private String content;
    private LocalDateTime createdTime;
    private String sources; // 答案来源

    public ChatMessage() {}

}