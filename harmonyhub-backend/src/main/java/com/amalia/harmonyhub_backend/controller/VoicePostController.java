package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.dtos.VoicePostCommentRequest;
import com.amalia.harmonyhub_backend.dtos.VoicePostRequest;
import com.amalia.harmonyhub_backend.services.VoicePostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/api/voice-posts")
public class VoicePostController {

    @Autowired
    private VoicePostService voicePostService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody VoicePostRequest request) {
        try {
            String username = getCurrentUsername();
            if (username == null) return ResponseEntity.status(401).body("Not authenticated");
            return ResponseEntity.ok(voicePostService.createPost(username, request));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(voicePostService.getFeed(pageable, getCurrentUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(voicePostService.getPost(id, getCurrentUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody VoicePostCommentRequest request) {
        try {
            String username = getCurrentUsername();
            if (username == null) return ResponseEntity.status(401).body("Not authenticated");
            return ResponseEntity.ok(voicePostService.addComment(id, username, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(voicePostService.getComments(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
