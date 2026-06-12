package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.QuizQuestion;
import com.amalia.harmonyhub_backend.model.QuizScore;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.repository.QuizQuestionRepository;
import com.amalia.harmonyhub_backend.repository.QuizScoreRepository;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    @Autowired private QuizQuestionRepository questionRepository;
    @Autowired
    private QuizScoreRepository scoreRepository;
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
                    // NOTE: correctAnswer is NOT sent to frontend
                    return dto;
                })
                .toList();
    }

    public Map<String, Object> submitAnswers(String username, Map<Long, String> answers) {
        List<QuizQuestion> questions = questionRepository.findAllById(answers.keySet());

        int correct = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        for (QuizQuestion q : questions) {
            String submitted = answers.get(q.getId());
            boolean isCorrect = q.getCorrectAnswer().name().equals(submitted);
            if (isCorrect) correct++;

            Map<String, Object> result = new HashMap<>();
            result.put("id", q.getId());
            result.put("text", q.getText());
            result.put("hint", q.getHint());
            result.put("correctAnswer", q.getCorrectAnswer());
            result.put("author", q.getAuthor());
            result.put("yourAnswer", submitted);
            result.put("correct", isCorrect);
            results.add(result);
        }

        // Save score if user is logged in
        if (username != null) {
            User user = userRepository.findByUsername(username);
            if (user != null) {
                QuizScore score = new QuizScore();
                score.setUser(user);
                score.setScore(correct);
                score.setTotalQuestions(questions.size());
                scoreRepository.save(score);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("score", correct);
        response.put("total", questions.size());
        response.put("results", results);
        return response;
    }

    public List<Map<String, Object>> getLeaderboard() {
        return scoreRepository.findTop10ByOrderByScoreDesc()
                .stream()
                .map(s -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("username", s.getUser().getUsername());
                    dto.put("photo", s.getUser().getPhoto() != null ? s.getUser().getPhoto() : "");
                    dto.put("score", s.getScore());
                    dto.put("total", s.getTotalQuestions());
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
                    dto.put("total", s.getTotalQuestions());
                    dto.put("playedAt", s.getPlayedAt());
                    return dto;
                })
                .toList();
    }
}
