package com.zhh.taskmanager.service;

import com.zhh.taskmanager.Entity.Flashcard;
import com.zhh.taskmanager.mapper.FlashcardMapper;
import com.zhh.taskmanager.mapper.ReviewLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlashcardServiceImpl implements FlashcardService {

    @Autowired
    private FlashcardMapper flashcardMapper;

    @Autowired
    private ReviewLogMapper reviewLogMapper; // 注入日志记录器

    @Override
    public List<Flashcard> getAllCards(Integer deckId) {
        return flashcardMapper.findAllByDeckId(deckId);
    }

    @Override
    public List<Flashcard> getDueCards(Integer deckId, int newLimit, int reviewLimit, Long userId) {
        // 1. 查询今天已经消耗的额度
        int newLearnedToday = reviewLogMapper.countNewLearnedToday(userId);
        int oldReviewedToday = reviewLogMapper.countOldReviewedToday(userId);

        // 2. 动态计算真实剩余额度 (防止出现负数)
        int effectiveNewLimit = Math.max(0, newLimit - newLearnedToday);
        int effectiveReviewLimit = Math.max(0, reviewLimit - oldReviewedToday);

        // 3. 根据真实剩余额度去数据库捞卡片
        List<Flashcard> reviewCards = flashcardMapper.findReviewCards(deckId, effectiveReviewLimit);
        List<Flashcard> newCards = flashcardMapper.findNewCards(deckId, effectiveNewLimit);

        List<Flashcard> combined = new java.util.ArrayList<>(reviewCards);
        combined.addAll(newCards);
        return combined;
    }

    @Override
    public void createFlashcard(Flashcard card) {
        card.setStatus("NEW");
        card.setIntervalMinutes(0);
        card.setEaseFactor(2.5);
        card.setConsecutiveCorrect(0);
        card.setNextReviewTime(LocalDateTime.now());
        flashcardMapper.insert(card);
    }

    // 【修改点】接收 userId，并调用 Mapper 的 updateContentSafe
    @Override
    public void updateFlashcardContent(Long id, String front, String back, Long userId) {
        Flashcard card = new Flashcard();
        card.setId(id);
        card.setFrontContent(front);
        card.setBackContent(back);
        flashcardMapper.updateContentSafe(card, userId);
    }

    // 【修改点】接收 userId，并调用 Mapper 的 deleteByIdAndUserId
    @Override
    public void deleteFlashcard(Long id, Long userId) {
        flashcardMapper.deleteByIdAndUserId(id, userId);
    }

    // 修改此方法，保存时同时插入日志
    @Override
    public void processReview(Long flashcardId, String grade, Long userId) {
        Flashcard card = flashcardMapper.findById(flashcardId);
        if (card == null) return;

        LocalDateTime now = LocalDateTime.now();

        // SM-2 核心算法逻辑保持不变
        switch (grade.toUpperCase()) {
            case "HARD":
                card.setStatus("LEARNING");
                card.setConsecutiveCorrect(0);
                card.setEaseFactor(Math.max(1.3, card.getEaseFactor() - 0.2));
                card.setIntervalMinutes(1);
                break;
            case "GOOD":
                card.setConsecutiveCorrect(card.getConsecutiveCorrect() + 1);
                if ("REVIEW".equals(card.getStatus())) {
                    card.setIntervalMinutes((int)(card.getIntervalMinutes() * card.getEaseFactor()));
                } else {
                    if (card.getConsecutiveCorrect() >= 2) {
                        card.setStatus("REVIEW");
                        card.setIntervalMinutes(1440);
                    } else {
                        card.setIntervalMinutes(10);
                    }
                }
                break;
            case "EASY":
                card.setStatus("REVIEW");
                card.setConsecutiveCorrect(card.getConsecutiveCorrect() + 1);
                card.setEaseFactor(card.getEaseFactor() + 0.15);
                if (card.getIntervalMinutes() < 1440) {
                    card.setIntervalMinutes(1440 * 4);
                } else {
                    card.setIntervalMinutes((int)(card.getIntervalMinutes() * card.getEaseFactor() * 1.3));
                }
                break;
        }

        card.setNextReviewTime(now.plusMinutes(card.getIntervalMinutes()));
        flashcardMapper.updateReviewStats(card);

        // 【新增】记录此次复习日志，用于热力图统计与状态回滚
        reviewLogMapper.insertLog(userId, flashcardId, grade);
    }

    // 【新增】执行回滚逻辑
    @Override
    public void rollbackReview(Long id, Flashcard oldState, Long userId) {
        oldState.setId(id);
        flashcardMapper.rollbackStats(oldState);          // 还原卡片状态
        reviewLogMapper.deleteLastLog(userId, id);        // 抹除统计日志
    }
}