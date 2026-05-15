package com.zhh.taskmanager.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Deck {
    private Integer id;
    private Long userId;
    private String name;
    private String description;
    private LocalDateTime createdTime;

}