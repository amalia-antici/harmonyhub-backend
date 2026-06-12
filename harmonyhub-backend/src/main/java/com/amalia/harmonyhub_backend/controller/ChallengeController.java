package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.dtos.ChallengeRequest;
import com.amalia.harmonyhub_backend.dtos.GradeRequest;
import com.amalia.harmonyhub_backend.dtos.SubmissionRequest;
import com.amalia.harmonyhub_backend.services.ChallengeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    @Autowired
    private ChallengeService challengeService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    // ── Public ──────────────────────────────────────────

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        try {
            return ResponseEntity.ok(challengeService.getActiveChallenge());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/winners")
    public ResponseEntity<?> getWinners(@PathVariable Long id) {
        return ResponseEntity.ok(challengeService.getWinners(id));
    }

    // ── User ────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id,
                                    @RequestBody SubmissionRequest request) {
        try {
            return ResponseEntity.ok(challengeService.submit(getCurrentUsername(), id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // ── Admin ────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ChallengeRequest request) {
        return ResponseEntity.ok(challengeService.createChallenge(request));
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<?> getSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(challengeService.getAllSubmissions(id));
    }

    @PutMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<?> grade(@PathVariable Long submissionId,
                                   @RequestBody GradeRequest request) {
        try {
            return ResponseEntity.ok(challengeService.gradeSubmission(submissionId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}