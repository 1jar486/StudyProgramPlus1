package com.zhh.taskmanager.service;

import com.zhh.taskmanager.Entity.Deck;
import java.util.List;

public interface DeckService {
    List<Deck> getUserDecks(Long userId);
    Deck createDeck(Deck deck);
    void updateDeck(Deck deck);
    void deleteDeck(Integer id);
}