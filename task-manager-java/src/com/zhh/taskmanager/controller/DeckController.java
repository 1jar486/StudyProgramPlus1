package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.Entity.Deck;
import com.zhh.taskmanager.service.DeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
@CrossOrigin // 确保跨域开启
public class DeckController {

    @Autowired
    private DeckService deckService;

    // 解析 Token 的方法
    private Long getUserIdFromToken(String token) {
        return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
    }

    @GetMapping
    public List<Deck> listDecks(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return deckService.getUserDecks(userId);
    }

    @PostMapping
    public ResponseEntity<?> createDeck(@RequestBody Deck deck, @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        deck.setUserId(userId);
        return ResponseEntity.ok(deckService.createDeck(deck));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeck(@PathVariable Integer id, @RequestBody Deck deck, @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token); // 校验身份
        deck.setId(id);
        deck.setUserId(userId);
        deckService.updateDeck(deck);
        return ResponseEntity.ok().build();
    }

    // 【修复】：这里恢复成 Controller 该有的样子，接收 HTTP DELETE 请求
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeck(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token); // 校验身份
        deckService.deleteDeck(id); // 呼叫 Service 去干活
        return ResponseEntity.ok().build();
    }
}