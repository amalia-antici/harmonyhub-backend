package com.amalia.harmonyhub_backend.dtos;

public class GradeRequest {
    private Integer grade;
    private boolean winner;

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }
}
