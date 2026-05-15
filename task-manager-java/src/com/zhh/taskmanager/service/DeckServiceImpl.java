package com.zhh.taskmanager.service;

import com.zhh.taskmanager.Entity.Deck;
import com.zhh.taskmanager.mapper.DeckMapper;
import com.zhh.taskmanager.mapper.FlashcardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeckServiceImpl implements DeckService {

    @Autowired
    private DeckMapper deckMapper;

    // 把闪卡的 Mapper 注入到这里
    @Autowired
    private FlashcardMapper flashcardMapper;

    @Override
    public List<Deck> getUserDecks(Long userId) {
        return deckMapper.findByUserId(userId);
    }

    @Override
    public Deck createDeck(Deck deck) {
        deckMapper.insert(deck);
        return deck;
    }

    @Override
    public void updateDeck(Deck deck) {
        deckMapper.update(deck);
    }

    // 【修复】：这段带 @Transactional 的级联删除代码，必须放在 ServiceImpl 里！
    @Override
    @Transactional
    public void deleteDeck(Integer id) {
        // 1. 先彻底抹除该牌组下的所有闪卡
        flashcardMapper.deleteByDeckId(id);
        // 2. 再抹除牌组本身
        deckMapper.deleteById(id);
    }
}