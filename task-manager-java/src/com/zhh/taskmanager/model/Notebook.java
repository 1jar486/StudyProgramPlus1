package com.zhh.taskmanager.model;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Notebook {
    private Integer id;
    private Long userId;
    private String name;
    private LocalDateTime createdTime;

    // 无参构造
    public Notebook() {}
}