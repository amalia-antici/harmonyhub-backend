package com.amalia.harmonyhub_backend.dtos;

import java.util.List;
import java.util.Map;

public class QuizResult {
    private int score;
    private int total;
    private List<Map<String, Object>> results;

    public QuizResult(int score, int total, List<Map<String, Object>> results) {
        this.score = score;
        this.total = total;
        this.results = results;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }
}
