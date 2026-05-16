package com.zhh.taskmanager.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewLogMapper {
    @Insert("INSERT INTO review_log (user_id, flashcard_id, grade, review_date) VALUES (#{userId}, #{flashcardId}, #{grade}, NOW())")
    void insertLog(@Param("userId") Long userId, @Param("flashcardId") Long flashcardId, @Param("grade") String grade);

    @Select("SELECT COUNT(*) FROM review_log WHERE user_id = #{userId} AND DATE(review_date) = CURDATE()")
    int countTodayByUserId(Long userId);

    @Delete("DELETE FROM review_log WHERE user_id = #{userId} AND flashcard_id = #{flashcardId} ORDER BY review_date DESC LIMIT 1")
    void deleteLastLog(@Param("userId") Long userId, @Param("flashcardId") Long flashcardId);

    // 【新增】获取用户的全量复习热力数据
    @Select("SELECT DATE_FORMAT(review_date, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM review_log WHERE user_id = #{userId} " +
            "GROUP BY DATE_FORMAT(review_date, '%Y-%m-%d') " +
            "ORDER BY date ASC")
    List<Map<String, Object>> getHeatmapStats(Long userId);

    // 巧妙的 SQL：查询今天复习过的卡片中，【历史第一次复习时间就是今天】的卡片数量（即：今天新学的卡片）
    @Select("SELECT COUNT(DISTINCT flashcard_id) FROM review_log " +
            "WHERE user_id = #{userId} AND DATE(review_date) = CURDATE() " +
            "AND flashcard_id IN (SELECT flashcard_id FROM review_log GROUP BY flashcard_id HAVING DATE(MIN(review_date)) = CURDATE())")
    int countNewLearnedToday(@Param("userId") Long userId);

    // 巧妙的 SQL：查询今天复习过的卡片中，【历史第一次复习时间早于今天】的卡片数量（即：今天复习的旧卡片）
    @Select("SELECT COUNT(DISTINCT flashcard_id) FROM review_log " +
            "WHERE user_id = #{userId} AND DATE(review_date) = CURDATE() " +
            "AND flashcard_id IN (SELECT flashcard_id FROM review_log GROUP BY flashcard_id HAVING DATE(MIN(review_date)) < CURDATE())")
    int countOldReviewedToday(@Param("userId") Long userId);
}