import { useEffect, useState } from "react";

import { getLearningStatistics } from "../../services/problemApi";
import "./LabPages.css";

function StatisticsPage() {
  const [statistics, setStatistics] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setErrorMessage("");

    getLearningStatistics().then((result) => {
      if (active) {
        setStatistics(result);
      }
    }).catch((error) => { if (active) setErrorMessage(error.message); })
      .finally(() => { if (active) setIsLoading(false); });

    return () => {
      active = false;
    };
  }, [reloadKey]);

  const correctAnswers = statistics?.correctAnswers ?? 0;
  const incorrectAnswers = statistics?.incorrectAnswers ?? 0;
  const answerCount = correctAnswers + incorrectAnswers;
  const accuracy =
    statistics?.accuracy ??
    (answerCount ? Math.round((correctAnswers / answerCount) * 100) : 0);

  if (isLoading) {
    return <div className="lab-page"><section className="large-empty">학습 통계를 불러오고 있습니다.</section></div>;
  }

  if (errorMessage && !statistics) {
    return (
      <div className="lab-page"><section className="large-empty">
        <strong>학습 통계를 불러오지 못했습니다.</strong><span>{errorMessage}</span>
        <button type="button" className="lab-primary-link" onClick={() => setReloadKey((value) => value + 1)}>다시 불러오기</button>
      </section></div>
    );
  }

  return (
    <div className="lab-page">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">LEARNING REPORT</span>
          <h1>학습 통계</h1>
          <p>AI 코딩 문제 생성과 예상 테스트 통과 기록을 확인하세요.</p>
        </div>
      </div>

      <section className="metric-grid statistics-metrics">
        <article className="metric-card">
          <span>생성된 문제</span>
          <strong>
            {statistics?.generatedProblems ?? 0}<small>개</small>
          </strong>
        </article>
        <article className="metric-card">
          <span>AI 예상 성공</span>
          <strong>
            {correctAnswers}<small>개</small>
          </strong>
        </article>
        <article className="metric-card">
          <span>보완 필요</span>
          <strong>
            {incorrectAnswers}<small>개</small>
          </strong>
        </article>
      </section>

      <div className="statistics-grid">
        <section className="surface-card accuracy-card">
          <h2>전체 AI 예상 통과율</h2>
          <div
            className="accuracy-ring"
            style={{
              "--accuracy": accuracy * 3.6 + "deg",
            }}
          >
            <div>
              <strong>{accuracy}%</strong>
              <span>정답률</span>
            </div>
          </div>
          <p>AI 코드 검토 {answerCount}회 기준</p>
        </section>

        <section className="surface-card category-chart">
          <h2>문법별 예상 통과율</h2>
          {statistics?.categoryAccuracy?.length ? (
            <div>
              {statistics.categoryAccuracy.map((item) => (
              <article key={item.name}>
                <span>{item.name}</span>
                <div>
                  <b style={{ width: item.value + "%" }} />
                </div>
                <strong>{item.value}%</strong>
              </article>
              ))}
            </div>
          ) : (
            <div className="compact-empty">
              문제를 풀면 문법별 정답률이 표시됩니다.
            </div>
          )}
        </section>

        <section className="surface-card weekly-chart">
          <h2>최근 7일 제출</h2>
          <div className="weekly-chart__bars">
            {(statistics?.weeklyAttempts || []).map((value, index) => (
              <div key={index}>
                <span style={{ height: value ? value * 12 + "px" : "0" }} />
                <small>{["월", "화", "수", "목", "금", "토", "일"][index]}</small>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

export default StatisticsPage;
