package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.services.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizService quizService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser"))
            return null;
        return auth.getName();
    }

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions() {
        return ResponseEntity.ok(quizService.getQuestions());
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody Map<Long, String> answers) {
        return ResponseEntity.ok(quizService.submitAnswers(getCurrentUsername(), answers));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard() {
        return ResponseEntity.ok(quizService.getLeaderboard());
    }

    @GetMapping("/history")
    public ResponseEntity<?> history() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(401).body("Not authenticated");
        return ResponseEntity.ok(quizService.getUserHistory(username));
    }
}
