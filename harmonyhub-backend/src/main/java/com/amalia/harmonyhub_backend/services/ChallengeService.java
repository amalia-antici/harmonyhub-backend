package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.dtos.ChallengeRequest;
import com.amalia.harmonyhub_backend.dtos.GradeRequest;
import com.amalia.harmonyhub_backend.dtos.SubmissionRequest;
import com.amalia.harmonyhub_backend.model.Challenge;
import com.amalia.harmonyhub_backend.model.ChallengeSubmission;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.repository.ChallengeRepository;
import com.amalia.harmonyhub_backend.repository.ChallengeSubmissionRepository;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChallengeService {

    @Autowired
    private ChallengeRepository challengeRepository;
    @Autowired private ChallengeSubmissionRepository submissionRepository;
    @Autowired private UserRepository userRepository;

    // ── Public ──────────────────────────────────────────

    public Map<String, Object> getActiveChallenge() {
        Challenge c = challengeRepository.findByActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active challenge"));
        return toChallengeDto(c);
    }

    public List<Map<String, Object>> getWinners(Long challengeId) {
        return submissionRepository.findByChallengeIdAndWinnerTrue(challengeId)
                .stream().map(this::toSubmissionDto).toList();
    }


    public Map<String, Object> submit(String username, Long challengeId, SubmissionRequest request) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        User user = userRepository.findByUsername(username);

        submissionRepository.findByChallengeIdAndUserId(challengeId, user.getId())
                .ifPresent(s -> { throw new RuntimeException("Already submitted"); });

        ChallengeSubmission submission = new ChallengeSubmission();
        submission.setChallenge(challenge);
        submission.setUser(user);
        submission.setInstagramLink(request.getInstagramLink());

        return toSubmissionDto(submissionRepository.save(submission));
    }


    public Map<String, Object> createChallenge(ChallengeRequest request) {
        challengeRepository.findByActiveTrue().ifPresent(c -> {
            c.setActive(false);
            challengeRepository.save(c);
        });

        Challenge challenge = new Challenge();
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setHashtag(request.getHashtag());
        challenge.setStartsAt(request.getStartsAt());
        challenge.setEndsAt(request.getEndsAt());
        challenge.setActive(true);

        return toChallengeDto(challengeRepository.save(challenge));
    }

    public Map<String, Object> gradeSubmission(Long submissionId, GradeRequest request) {
        ChallengeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        submission.setGrade(request.getGrade());
        submission.setWinner(request.isWinner());
        return toSubmissionDto(submissionRepository.save(submission));
    }

    public List<Map<String, Object>> getAllSubmissions(Long challengeId) {
        return submissionRepository.findByChallengeIdOrderByGradeDesc(challengeId)
                .stream().map(this::toSubmissionDto).toList();
    }


    private Map<String, Object> toChallengeDto(Challenge c) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", c.getId());
        dto.put("title", c.getTitle());
        dto.put("description", c.getDescription());
        dto.put("hashtag", c.getHashtag());
        dto.put("startsAt", c.getStartsAt());
        dto.put("endsAt", c.getEndsAt());
        dto.put("active", c.isActive());
        return dto;
    }

    private Map<String, Object> toSubmissionDto(ChallengeSubmission s) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", s.getId());
        dto.put("instagramLink", s.getInstagramLink());
        dto.put("grade", s.getGrade());
        dto.put("winner", s.isWinner());
        dto.put("submittedAt", s.getSubmittedAt());
        dto.put("username", s.getUser().getUsername());
        dto.put("userPhoto", s.getUser().getPhoto() != null ? s.getUser().getPhoto() : "");
        dto.put("challengeId", s.getChallenge().getId());
        return dto;
    }
}
