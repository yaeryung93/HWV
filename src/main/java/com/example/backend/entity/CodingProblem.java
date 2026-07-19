package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CodingProblem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JsonIgnore private User user;
    @Column(nullable = false, length = 100) private String grammarName;
    @Column(nullable = false) private String title;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String description;
    @Lob @Column(columnDefinition = "LONGTEXT") private String requirementsJson;
    @Lob @Column(columnDefinition = "LONGTEXT") private String inputExample;
    @Lob @Column(columnDefinition = "LONGTEXT") private String outputExample;
    @Lob @Column(columnDefinition = "LONGTEXT") private String starterCode;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String testsJson;
    @Column(nullable = false) private String difficulty = "보통";
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public String getGrammarName() {
        return grammarName;
    }
    public void setGrammarName(String value) {
        grammarName = value;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String value) {
        title = value;
    }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getRequirementsJson() { return requirementsJson; }
    public void setRequirementsJson(String value) { requirementsJson = value; }
    public String getInputExample() { return inputExample; }
    public void setInputExample(String value) { inputExample = value; }
    public String getOutputExample() { return outputExample; }
    public void setOutputExample(String value) { outputExample = value; }
    public String getStarterCode() { return starterCode; }
    public void setStarterCode(String value) { starterCode = value; }
    public String getTestsJson() { return testsJson; }
    public void setTestsJson(String value) { testsJson = value; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String value) { difficulty = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
