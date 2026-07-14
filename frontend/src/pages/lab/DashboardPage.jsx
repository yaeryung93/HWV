import { useEffect, useState } from "react";
import { Link } from "react-router";

import { getDashboardSummary } from "../../services/problemApi";
import { getSessionUser } from "../../services/session";
import "./LabPages.css";

function DashboardPage() {
  const [summary, setSummary] = useState(null);
  const user = getSessionUser();

  useEffect(() => {
    let active = true;

    getDashboardSummary().then((result) => {
      if (active) {
        setSummary(result);
      }
    });

    return () => {
      active = false;
    };
  }, []);

  const cards = [
    {
      label: "생성된 문제",
      value: summary?.generatedProblems ?? 0,
      unit: "개",
    },
    { label: "정답", value: summary?.correctAnswers ?? 0, unit: "개" },
    { label: "오답", value: summary?.incorrectAnswers ?? 0, unit: "개" },
    { label: "퀴즈 정답률", value: summary?.accuracy ?? 0, unit: "%" },
  ];

  return (
    <div className="lab-page">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">HWV CODE LAB</span>
          <h1>
            안녕하세요{user?.name ? `, ${user.name}님` : ""}
          </h1>
          <p>
            Java 파일을 업로드하면 AI가 핵심 문법을 분석하고 맞춤 퀴즈를
            만들어 줍니다.
          </p>
        </div>

        <Link className="lab-primary-link" to="/problems/new">
          ＋ 새 문제 만들기
        </Link>
      </div>

      <section className="metric-grid">
        {cards.map((card) => (
          <article className="metric-card" key={card.label}>
            <span>{card.label}</span>
            <strong>
              {card.value}
              <small>{card.unit}</small>
            </strong>
          </article>
        ))}
      </section>

      <div className="dashboard-grid">
        <section className="surface-card">
          <div className="surface-card__header">
            <h2>최근 제출</h2>
            <Link to="/wrong-notes">전체 보기</Link>
          </div>

          {summary?.recentAttempts?.length ? (
            <div className="activity-list">
              {summary.recentAttempts.map((attempt) => (
                <article key={attempt.id}>
                  <div>
                    <strong>{attempt.problemTitle}</strong>
                    <span>
                      테스트 {attempt.passedCount}/{attempt.totalCount} 통과
                    </span>
                  </div>
                  <span
                    className={
                      attempt.passed
                        ? "result-pill result-pill--passed"
                        : "result-pill result-pill--failed"
                    }
                  >
                    {attempt.passed ? "성공" : "보완 필요"}
                  </span>
                </article>
              ))}
            </div>
          ) : (
            <div className="compact-empty">
              아직 제출 기록이 없습니다. 학습자료를 업로드해 첫 문제를 만들어
              보세요.
            </div>
          )}
        </section>

        <section className="surface-card dashboard-guide">
          <h2>학습 흐름</h2>
          <ol>
            <li>
              <b>1</b>
              <span>학습할 Java 파일 업로드</span>
            </li>
            <li>
              <b>2</b>
              <span>AI가 핵심 Java 문법 3개 분석</span>
            </li>
            <li>
              <b>3</b>
              <span>분석 결과를 바탕으로 퀴즈 5개 생성</span>
            </li>
            <li>
              <b>4</b>
              <span>퀴즈를 풀고 정답과 오답 확인</span>
            </li>
          </ol>
        </section>
      </div>
    </div>
  );
}

export default DashboardPage;
