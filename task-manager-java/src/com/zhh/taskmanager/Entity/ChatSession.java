package com.zhh.taskmanager.Entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatSession {
    private Integer id;
    private Integer notebookId;
    private Integer sessionId;
    private String title;
    private Date createdAt;
    private Date updatedAt;
    // 🌟 新增：简单的置顶标记（0为否，1为是）
    private Integer isPinned;
}