import { useEffect, useState } from "react";
import { Link } from "react-router";

import { getDashboardSummary } from "../../services/problemApi";
import { getSessionUser } from "../../services/session";
import "./LabPages.css";

function DashboardPage() {
  const [summary, setSummary] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const user = getSessionUser();

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setErrorMessage("");

    getDashboardSummary().then((result) => {
      if (active) {
        setSummary(result);
      }
    }).catch((error) => { if (active) setErrorMessage(error.message); })
      .finally(() => { if (active) setIsLoading(false); });

    return () => {
      active = false;
    };
  }, [reloadKey]);

  const cards = [
    {
      label: "생성된 문제",
      value: summary?.generatedProblems ?? 0,
      unit: "개",
    },
    { label: "AI 예상 성공", value: summary?.correctAnswers ?? 0, unit: "개" },
    { label: "보완 필요", value: summary?.incorrectAnswers ?? 0, unit: "개" },
    { label: "AI 예상 통과율", value: summary?.accuracy ?? 0, unit: "%" },
  ];

  if (isLoading) {
    return <div className="lab-page"><section className="large-empty">학습 현황을 불러오고 있습니다.</section></div>;
  }

  if (errorMessage && !summary) {
    return (
      <div className="lab-page"><section className="large-empty">
        <strong>학습 현황을 불러오지 못했습니다.</strong><span>{errorMessage}</span>
        <button type="button" className="lab-primary-link" onClick={() => setReloadKey((value) => value + 1)}>다시 불러오기</button>
      </section></div>
    );
  }

  return (
    <div className="lab-page">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">HWV CODE LAB</span>
          <h1>
            안녕하세요{user?.name ? `, ${user.name}님` : ""}
          </h1>
          <p>
            Java 파일의 핵심 문법 3개를 분석해 문법별 코딩 문제를 만들어 줍니다.
          </p>
        </div>

        <Link className="lab-primary-link" to="/problems/new">
          ＋ 새 문제 만들기
        </Link>
      </div>

      {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}

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
              <span>핵심 문법마다 코딩 문제 1개씩, 총 3개 생성</span>
            </li>
            <li>
              <b>4</b>
              <span>코드를 작성하고 AI 예상 테스트 및 피드백 확인</span>
            </li>
          </ol>
        </section>
      </div>
    </div>
  );
}

export default DashboardPage;
