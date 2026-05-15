package com.zhh.taskmanager.controller;

import com.zhh.taskmanager.Entity.Flashcard;
import com.zhh.taskmanager.service.FlashcardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flashcards")
@CrossOrigin // 确保跨域开启
public class FlashcardController {

    @Autowired
    private FlashcardService flashcardService;

    // 【新增】解析 Token
    private Long getUserIdFromToken(String token) {
        return Long.parseLong(token.replace("SUCCESS_TOKEN_FOR_", ""));
    }

    @GetMapping("/deck/{deckId}")
    public List<Flashcard> getCards(@PathVariable Integer deckId, @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token); // 拦截未登录请求
        return flashcardService.getAllCards(deckId);
    }

    @GetMapping("/deck/{deckId}/due")
    public List<Flashcard> getDueCards(@PathVariable Integer deckId, @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token); // 拦截未登录请求
        return flashcardService.getDueCards(deckId);
    }

    @PostMapping
    public ResponseEntity<?> addCard(@RequestBody Flashcard card, @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);
        flashcardService.createFlashcard(card);
        return ResponseEntity.ok(card);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCard(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token); // 拿住 userId !
        // 传递下去
        flashcardService.updateFlashcardContent(id, body.get("front"), body.get("back"), userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCard(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token); // 拿住 userId !
        // 传递下去
        flashcardService.deleteFlashcard(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> submitReview(@PathVariable Long id, @RequestParam String grade, @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);
        flashcardService.processReview(id, grade);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/deck/{deckId}/export")
    public ResponseEntity<byte[]> exportDeck(@PathVariable Integer deckId, @RequestHeader("Authorization") String token) {
        getUserIdFromToken(token);
        List<Flashcard> cards = flashcardService.getAllCards(deckId);

        StringBuilder csvContent = new StringBuilder("Front,Back,Status\n");
        for (Flashcard card : cards) {
            String front = "\"" + card.getFrontContent().replace("\"", "\"\"") + "\"";
            String back = "\"" + card.getBackContent().replace("\"", "\"\"") + "\"";
            csvContent.append(front).append(",").append(back).append(",").append(card.getStatus()).append("\n");
        }

        byte[] out = csvContent.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=deck_" + deckId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(out);
    }
}