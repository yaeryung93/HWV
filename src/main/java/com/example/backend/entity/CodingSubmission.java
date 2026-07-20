package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CodingSubmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private CodingProblem problem;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String sourceCode;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String testsJson;
    @Lob @Column(columnDefinition = "LONGTEXT") private String hint;
    @Lob @Column(columnDefinition = "LONGTEXT") private String improvement;
    private int passedCount;
    private int totalCount;
    private boolean passed;
    private LocalDateTime submittedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User value) { user = value; }
    public CodingProblem getProblem() { return problem; }
    public void setProblem(CodingProblem value) { problem = value; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String value) { sourceCode = value; }
    public String getTestsJson() { return testsJson; }
    public void setTestsJson(String value) { testsJson = value; }
    public String getHint() { return hint; }
    public void setHint(String value) { hint = value; }
    public String getImprovement() { return improvement; }
    public void setImprovement(String value) { improvement = value; }
    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int value) { passedCount = value; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int value) { totalCount = value; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean value) { passed = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
