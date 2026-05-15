package com.zhh.taskmanager.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Flashcard {
    private Long id;
    private Integer deckId;
    private String frontContent;
    private String backContent;

    // 记忆核心状态
    private String status; // NEW, LEARNING, REVIEW
    private LocalDateTime nextReviewTime;
    private Integer intervalMinutes;
    private Double easeFactor;
    private Integer consecutiveCorrect;

    private LocalDateTime createdTime;

}