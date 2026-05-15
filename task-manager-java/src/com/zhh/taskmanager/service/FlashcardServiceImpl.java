package com.zhh.taskmanager.service;

import com.zhh.taskmanager.Entity.Flashcard;
import com.zhh.taskmanager.mapper.FlashcardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlashcardServiceImpl implements FlashcardService {

    @Autowired
    private FlashcardMapper flashcardMapper;

    @Override
    public List<Flashcard> getAllCards(Integer deckId) {
        return flashcardMapper.findAllByDeckId(deckId);
    }

    @Override
    public List<Flashcard> getDueCards(Integer deckId) {
        return flashcardMapper.findDueCardsByDeckId(deckId);
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

    @Override
    public void processReview(Long flashcardId, String grade) {
        Flashcard card = flashcardMapper.findById(flashcardId);
        if (card == null) return;

        LocalDateTime now = LocalDateTime.now();

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
    }
}