package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.Entity.Flashcard;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FlashcardMapper {

    // ================== 基础 CRUD ==================
    @Insert("INSERT INTO flashcard (deck_id, front_content, back_content, status, next_review_time, interval_minutes, ease_factor, consecutive_correct) " +
            "VALUES (#{deckId}, #{frontContent}, #{backContent}, #{status}, #{nextReviewTime}, #{intervalMinutes}, #{easeFactor}, #{consecutiveCorrect})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Flashcard flashcard);

    // 安全删除：验证卡片归属
    @Delete("DELETE FROM flashcard WHERE id = #{id} AND deck_id IN (SELECT id FROM deck WHERE user_id = #{userId})")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // 安全更新：验证卡片归属 (注意这里加了 flashcard. 前缀)
    @Update("UPDATE flashcard SET front_content = #{flashcard.frontContent}, back_content = #{flashcard.backContent} " +
            "WHERE id = #{flashcard.id} AND deck_id IN (SELECT id FROM deck WHERE user_id = #{userId})")
    void updateContentSafe(@Param("flashcard") Flashcard flashcard, @Param("userId") Long userId);

    @Select("SELECT * FROM flashcard WHERE id = #{id}")
    Flashcard findById(Long id);

    // ================== 查询列表 ==================
    // 查询某牌组下的所有卡片 (用于列表展示、导出)
    @Select("SELECT * FROM flashcard WHERE deck_id = #{deckId} ORDER BY created_time DESC")
    List<Flashcard> findAllByDeckId(Integer deckId);

    // 查询某牌组下，当前时间之前需要复习的所有卡片 (用于沉浸式学习模式)
    @Select("SELECT * FROM flashcard WHERE deck_id = #{deckId} AND next_review_time <= NOW() ORDER BY next_review_time ASC")
    List<Flashcard> findDueCardsByDeckId(Integer deckId);

    // ================== 核心算法专用 ==================
    @Update("UPDATE flashcard SET status = #{status}, next_review_time = #{nextReviewTime}, " +
            "interval_minutes = #{intervalMinutes}, ease_factor = #{easeFactor}, " +
            "consecutive_correct = #{consecutiveCorrect} " +
            "WHERE id = #{id}")
    void updateReviewStats(Flashcard flashcard);

    @Delete("DELETE FROM flashcard WHERE deck_id = #{deckId}")
    void deleteByDeckId(Integer deckId);
}