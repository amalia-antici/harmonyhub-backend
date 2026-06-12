package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.QuizScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizScoreRepository extends JpaRepository<QuizScore, Long> {
    List<QuizScore> findByUserIdOrderByScoreDesc(Long userId);
    List<QuizScore> findTop10ByOrderByScoreDesc();
}
