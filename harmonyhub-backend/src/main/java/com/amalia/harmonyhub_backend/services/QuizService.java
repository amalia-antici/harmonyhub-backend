package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.dtos.QuizResult;
import com.amalia.harmonyhub_backend.model.QuizQuestion;
import com.amalia.harmonyhub_backend.model.QuizScore;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.repository.QuizQuestionRepository;
import com.amalia.harmonyhub_backend.repository.QuizScoreRepository;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizService {

    @Autowired private QuizQuestionRepository questionRepository;
    @Autowired private QuizScoreRepository scoreRepository;
    @Autowired private UserRepository userRepository;

    private static final int QUESTIONS_PER_GAME = 10;

    public List<Map<String, Object>> getQuestions() {
        return questionRepository.findRandomQuestions(QUESTIONS_PER_GAME)
                .stream()
                .map(q -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", q.getId());
                    dto.put("text", q.getText());
                    dto.put("hint", q.getHint());
                    return dto;
                })
                .toList();
    }

    @Transactional
    public QuizResult submitAnswers(String username, Map<Long, String> userAnswers) {
        User user = userRepository.findByUsername(username);

        int finalScore = 0;
        int totalQuestions = userAnswers.size();
        List<Map<String, Object>> breakdownList = new ArrayList<>();

        // 🛠️ FIX 1: Complete the grading logic by pulling correct answers from the database
        for (Map.Entry<Long, String> entry : userAnswers.entrySet()) {
            Long questionId = entry.getKey();
            String submittedAnswerStr = entry.getValue(); // "SHAKESPEARE" or "SONGWRITER"

            QuizQuestion question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));

            // Check correctness against the QuoteType Enum defined in your entity model
            boolean isCorrect = question.getCorrectAnswer().toString().equalsIgnoreCase(submittedAnswerStr);
            if (isCorrect) {
                finalScore++;
            }

            // Create the breakdown object structure the React frontend is expecting
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", question.getId());
            detail.put("text", question.getText());
            detail.put("author", question.getAuthor());
            detail.put("hint", question.getHint());
            detail.put("yourAnswer", submittedAnswerStr);
            detail.put("correctAnswer", question.getCorrectAnswer().toString());
            detail.put("correct", isCorrect);
            breakdownList.add(detail);
        }

        // 🛠️ FIX 2: Prevent multiple entries. Check if user already has an entry on the board
        if (user != null) {
            Optional<QuizScore> existingScoreOpt = scoreRepository.findByUserId(user.getId());

            if (existingScoreOpt.isPresent()) {
                QuizScore existingScore = existingScoreOpt.get();

                // 💡 OPTION: Decide if you want to keep their highest score or overwrite with latest.
                // Keeping highest score ensures leaderboard reflects personal bests:
                if (finalScore > existingScore.getScore()) {
                    existingScore.setScore(finalScore);
                    existingScore.setTotalQuestions(totalQuestions);
                    // Update the timestamp as well to match the new score attempt
                    existingScore.setPlayedAt(java.time.LocalDateTime.now());
                    scoreRepository.save(existingScore);
                }
            } else {
                // First time playing! Create a fresh row for this account
                QuizScore newScore = new QuizScore();
                newScore.setUser(user);
                newScore.setScore(finalScore);
                newScore.setTotalQuestions(totalQuestions);
                scoreRepository.save(newScore);
            }
        }

        // 🛠️ FIX 3: Return your mapped QuizResult DTO containing the final calculated scores
        return new QuizResult(finalScore, totalQuestions, breakdownList);
    }

    public List<Map<String, Object>> getLeaderboard() {
        return scoreRepository.findTop10ByOrderByScoreDesc()
                .stream()
                .map(s -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("username", s.getUser().getUsername());
                    dto.put("photo", s.getUser().getPhoto() != null ? s.getUser().getPhoto() : "");
                    dto.put("score", s.getScore());
                    dto.put("total", s.getTotalQuestions()); // Matches QuizScore.getTotalQuestions()
                    dto.put("playedAt", s.getPlayedAt());
                    return dto;
                })
                .toList();
    }

    public List<Map<String, Object>> getUserHistory(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return List.of();
        return scoreRepository.findByUserIdOrderByScoreDesc(user.getId())
                .stream()
                .map(s -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("score", s.getScore());
                    dto.put("total", s.getTotalQuestions()); // Matches QuizScore.getTotalQuestions()
                    dto.put("playedAt", s.getPlayedAt());
                    return dto;
                })
                .toList();
    }
}