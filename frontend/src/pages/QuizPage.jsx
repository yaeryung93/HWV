import { useState } from "react";
import { Link } from "react-router";

import Button from "../components/common/Button";
import {
  getCurrentJavaQuiz,
  submitJavaQuizAnswers,
} from "../services/javaLearningApi";
import "./lab/LabPages.css";

function QuizPage() {
  const [questions] = useState(() => getCurrentJavaQuiz());
  const [selectedAnswers, setSelectedAnswers] = useState({});
  const [result, setResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleAnswerChange(questionId, answer) {
    setSelectedAnswers((previousAnswers) => ({
      ...previousAnswers,
      [questionId]: answer,
    }));
    setResult(null);
    setErrorMessage("");
  }

  async function handleSubmit() {
    if (questions.some((question) => !selectedAnswers[question.id])) {
      setErrorMessage("5개 문제에 모두 답을 선택해 주세요.");
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMessage("");
      setResult(await submitJavaQuizAnswers(questions, selectedAnswers));
    } catch (error) {
      setErrorMessage(error.message || "퀴즈 결과를 저장하지 못했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (questions.length === 0) {
    return (
      <div className="lab-page lab-page--narrow">
        <section className="large-empty">
          <strong>생성된 Java 문제가 없습니다.</strong>
          <span>Java 파일을 분석하고 문제 5개를 먼저 생성해 주세요.</span>
          <Link className="lab-primary-link" to="/problems/new">
            ＋ Java 파일 분석하기
          </Link>
        </section>
      </div>
    );
  }

  return (
    <div className="lab-page lab-page--narrow">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">JAVA QUIZ</span>
          <h1>AI Java 퀴즈</h1>
          <p>업로드한 Java 코드의 핵심 문법으로 만든 5개 문제입니다.</p>
        </div>
      </div>

      <div className="java-quiz-list">
        {questions.map((question, questionIndex) => (
          <section className="empty-card java-quiz-card" key={question.id}>
            <span className="java-quiz-card__number">
              문제 {questionIndex + 1} / {questions.length}
            </span>
            <h2>{question.question}</h2>

            <div className="java-quiz-options">
              {question.options.map((option) => (
                <label key={option}>
                  <input
                    type="radio"
                    name={`quiz-answer-${question.id}`}
                    value={option}
                    checked={selectedAnswers[question.id] === option}
                    onChange={() => handleAnswerChange(question.id, option)}
                  />
                  <span>{option}</span>
                </label>
              ))}
            </div>
          </section>
        ))}
      </div>

      {errorMessage && (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      )}

      <div className="java-quiz-submit">
        <Button disabled={isSubmitting} onClick={handleSubmit}>
          {isSubmitting ? "결과 저장 중..." : "정답 확인"}
        </Button>
      </div>

      {result && (
        <section className="quiz-result-card">
          <strong>퀴즈 결과</strong>
          <p>
            정답 <b>{result.correctCount}개</b> · 오답{" "}
            <b>{result.wrongCount}개</b>
          </p>
        </section>
      )}
    </div>
  );
}

export default QuizPage;
