package com.zhh.taskmanager.model; // 在这个软件包下
public class Task {
    private Integer id; // 任务ID
    private Long userId; // 用户ID
    private String title; // 任务标题
    private boolean completed; // 任务完成状态
    // 【新增】任务优先级，例如："高", "中", "低"
    private String priority;
    // 【新增】任务标签，例如："学习", "生活", "兼职"
    private String tag;

    private String memo; // 详情备忘
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

    // --- 下面是 Getter 和 Setter 方法（必须有，否则报错） ---
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // 【新增】获取优先级
    public String getPriority() { return priority; }
    // 【新增】设置优先级
    public void setPriority(String priority) { this.priority = priority; }

    // 【新增】获取标签
    public String getTag() { return tag; }
    // 【新增】设置标签
    public void setTag(String tag) { this.tag = tag; }

    // 手动添加这个方法，Controller 才能调用它
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // 建议顺便把获取的方法也加上，以后肯定会用到
    public Long getUserId() {
        return userId;
    }

    public String getMemo() {return memo;}

    public void setMemo(String memo) {this.memo = memo;}
}