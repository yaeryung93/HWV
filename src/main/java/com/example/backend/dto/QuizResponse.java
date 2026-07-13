package com.example.backend.dto;

import java.util.List;

public class QuizResponse {

    private Long userId;
    private int totalCount;
    private List<Question> questions;

    public QuizResponse() {
    }

    public QuizResponse(Long userId, List<Question> questions) {
        this.userId = userId;
        this.questions = questions;
        this.totalCount = questions.size();
    }

    public Long getUserId() {
        return userId;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
        this.totalCount = questions.size();
    }

    public static class Question {

        private Long id;
        private String question;
        private List<String> options;

        private Integer answer;       // 정답 (채점용)
        private String explanation;   // 해설

        public Question() {
        }

        public Question(Long id,
                        String question,
                        List<String> options,
                        Integer answer,
                        String explanation) {

            this.id = id;
            this.question = question;
            this.options = options;
            this.answer = answer;
            this.explanation = explanation;
        }

        public Long getId() {
            return id;
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getOptions() {
            return options;
        }

        public Integer getAnswer() {
            return answer;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public void setAnswer(Integer answer) {
            this.answer = answer;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }
}