package com.zhh.taskmanager.service;

import com.zhh.taskmanager.Entity.Flashcard;
import java.util.List;

public interface FlashcardService {

    List<Flashcard> getAllCards(Integer deckId);

    List<Flashcard> getDueCards(Integer deckId);

    void createFlashcard(Flashcard card);

    // 【修改点】加入了 Long userId 参数
    void updateFlashcardContent(Long id, String front, String back, Long userId);

    // 【修改点】加入了 Long userId 参数
    void deleteFlashcard(Long id, Long userId);

    void processReview(Long flashcardId, String grade);
}