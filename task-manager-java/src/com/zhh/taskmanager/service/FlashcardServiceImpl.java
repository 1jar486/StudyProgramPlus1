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
    public List<Flashcard> getDueCards(Integer deckId, int newLimit, int reviewLimit, Long userId, String mode) {
        // 1. 查询今天已经消耗的配额
        int newLearnedToday = reviewLogMapper.countNewLearnedToday(userId);
        int oldReviewedToday = reviewLogMapper.countOldReviewedToday(userId);

        int effectiveNewLimit = 0;
        int effectiveReviewLimit = 0;

        // 2. 🌟 核心策略分发：根据不同模式动态计算额度
        if ("rapid".equals(mode)) {
            // 【快速突破模式】：考前突击专用！无视今天已经学过的防沉迷配额，强制按设定的上限输出，并把新卡获取量翻倍！
            effectiveNewLimit = newLimit * 2;
            effectiveReviewLimit = reviewLimit * 2;
        } else {
            // 【标准模式】与【重难点模式】：严格遵守 SM-2 算法的每日配额，扣除今天已学数量，防止过度复习
            effectiveNewLimit = Math.max(0, newLimit - newLearnedToday);
            effectiveReviewLimit = Math.max(0, reviewLimit - oldReviewedToday);
        }

        // 3. 去数据库捞卡片 (重难点模式下，我们故意扩大复习卡的捞取范围，以便后续在内存中进行"困难度提取")
        int fetchReviewLimit = "difficult".equals(mode) ? effectiveReviewLimit * 3 : effectiveReviewLimit;
        List<Flashcard> reviewCards = flashcardMapper.findReviewCards(deckId, fetchReviewLimit);
        List<Flashcard> newCards = flashcardMapper.findNewCards(deckId, effectiveNewLimit);

        // 4. 🌟 核心算法：【重难点轰炸模式】的内存级重排
        if ("difficult".equals(mode) && !reviewCards.isEmpty()) {
            // a. 针对捞出来的大批复习卡，按“痛苦程度”进行排序
            reviewCards.sort((a, b) -> {
                // 第一梯队：把正在 LEARNING（生疏/遗忘）状态的卡片强制置顶
                boolean aIsLearning = "LEARNING".equals(a.getStatus());
                boolean bIsLearning = "LEARNING".equals(b.getStatus());
                if (aIsLearning && !bIsLearning) return -1;
                if (!aIsLearning && bIsLearning) return 1;

                // 第二梯队：按 easeFactor (掌握难易度) 升序排，分数越低说明越难，越要排在前面挨揍
                Double easeA = a.getEaseFactor() != null ? a.getEaseFactor() : 2.5;
                Double easeB = b.getEaseFactor() != null ? b.getEaseFactor() : 2.5;
                return Double.compare(easeA, easeB);
            });

            // b. 截断到原本的复习限制数量
            if (reviewCards.size() > effectiveReviewLimit) {
                reviewCards = reviewCards.subList(0, effectiveReviewLimit);
            }
            // c. 重难点模式下，屏蔽新卡，集中精力攻克难关
            newCards.clear();
        }

        // 5. 合并并返回最终结果
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