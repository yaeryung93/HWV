import { useEffect, useState } from "react";

import { getLearningStatistics } from "../../services/problemApi";
import "./LabPages.css";

function StatisticsPage() {
  const [statistics, setStatistics] = useState(null);

  useEffect(() => {
    let active = true;

    getLearningStatistics().then((result) => {
      if (active) {
        setStatistics(result);
      }
    });

    return () => {
      active = false;
    };
  }, []);

  const correctAnswers = statistics?.correctAnswers ?? 0;
  const incorrectAnswers = statistics?.incorrectAnswers ?? 0;
  const answerCount = correctAnswers + incorrectAnswers;
  const accuracy =
    statistics?.accuracy ??
    (answerCount ? Math.round((correctAnswers / answerCount) * 100) : 0);

  return (
    <div className="lab-page">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">LEARNING REPORT</span>
          <h1>학습 통계</h1>
          <p>문제 풀이 기록과 테스트 통과율을 한눈에 확인하세요.</p>
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
          <span>정답</span>
          <strong>
            {correctAnswers}<small>개</small>
          </strong>
        </article>
        <article className="metric-card">
          <span>오답</span>
          <strong>
            {incorrectAnswers}<small>개</small>
          </strong>
        </article>
      </section>

      <div className="statistics-grid">
        <section className="surface-card accuracy-card">
          <h2>전체 테스트 정답률</h2>
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
          <p>퀴즈 응답 {answerCount}개 기준</p>
        </section>

        <section className="surface-card category-chart">
          <h2>영역별 정답률</h2>
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
