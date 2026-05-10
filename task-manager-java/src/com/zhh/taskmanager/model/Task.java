package com.zhh.taskmanager.model; // 在这个软件包下
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
@Data
public class Task {
    private Integer id; // 任务ID
    private Long userId; // 用户ID
    private String title; // 任务标题
    private boolean completed; // 任务完成状态
    private String priority; // 【新增】任务优先级，例如："高", "中", "低"
    private String tag; // 【新增】任务标签，例如："学习", "生活", "兼职"
    private String memo; // 详情备忘
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;

    // --- 必须有这个：无参数构造方法（数据库需要） ---
    public Task() {
    }
    // 包含所有字段的构造方法（方便手动创建对象）
    public Task(String title, boolean completed, String priority, String tag) {
        this.title = title;
        this.completed = completed;
        this.priority = priority;
        this.tag = tag;
    }


}