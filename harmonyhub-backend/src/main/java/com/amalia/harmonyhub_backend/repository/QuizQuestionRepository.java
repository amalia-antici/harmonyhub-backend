package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.QuizQuestion;
import com.amalia.harmonyhub_backend.model.QuizScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    @Query(value = "SELECT * FROM quiz_question ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<QuizQuestion> findRandomQuestions(@Param("n") int n);
}
