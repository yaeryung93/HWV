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

  function handleReload() {
    setIsLoading(true);
    setErrorMessage("");
    setReloadKey((value) => value + 1);
  }

  const correctAnswers = statistics?.correctAnswers ?? 0;
  const incorrectAnswers = statistics?.incorrectAnswers ?? 0;
  const answerCount = correctAnswers + incorrectAnswers;
  const accuracy =
    statistics?.accuracy ??
    (answerCount ? Math.round((correctAnswers / answerCount) * 100) : 0);
  const weekDays = ["월", "화", "수", "목", "금", "토", "일"];
  const weeklyAttempts = Array.from(
    { length: 7 },
    (_, index) => statistics?.weeklyAttempts?.[index] ?? 0,
  );
  const dailyAccuracy = Array.from({ length: 7 }, (_, index) => {
    const item = statistics?.dailyAccuracy?.[index];
    return {
      accuracy: Math.max(0, Math.min(100, item?.accuracy ?? 0)),
      correct: item?.correct ?? 0,
      total: item?.total ?? weeklyAttempts[index],
    };
  });
  const accuracyEmoji = (value) => {
    if (value <= 20) return "😭";
    if (value <= 40) return "😢";
    if (value <= 60) return "😐";
    if (value <= 80) return "😏";
    return "🤭";
  };
  const weeklyPoints = dailyAccuracy.map((item, index) => ({
    day: weekDays[index],
    ...item,
    emoji: accuracyEmoji(item.accuracy),
    x: 70 + index * (1060 / 6),
    y: 135 - item.accuracy,
  }));
  const weeklyPolyline = weeklyPoints.map((point) => `${point.x},${point.y}`).join(" ");

  if (isLoading) {
    return <div className="lab-page"><section className="large-empty">학습 통계를 불러오고 있습니다.</section></div>;
  }

  if (errorMessage && !statistics) {
    return (
      <div className="lab-page"><section className="large-empty">
        <strong>학습 통계를 불러오지 못했습니다.</strong><span>{errorMessage}</span>
        <button type="button" className="lab-primary-link" onClick={handleReload}>다시 불러오기</button>
      </section></div>
    );
  }

  return (
    <div className="lab-page">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">HWV CODE LAB</span>
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
          <h2>최근 7일 일별 정답률</h2>
          <div className="weekly-chart__line">
            <svg
              viewBox="0 0 1200 180"
              role="img"
              aria-label={`최근 7일 일별 정답률: ${dailyAccuracy.map((item) => item.accuracy + "%").join(", ")}`}
            >
              {[0, 20, 40, 60, 80, 100].map((percentage) => (
                <g key={percentage}>
                  <line className="weekly-chart__grid-line" x1="70" x2="1130" y1={135 - percentage} y2={135 - percentage} />
                  <text className="weekly-chart__axis-label" x="48" y={139 - percentage}>{percentage}%</text>
                </g>
              ))}
              <polyline className="weekly-chart__polyline" points={weeklyPolyline} />
              {weeklyPoints.map((point) => (
                <g key={point.day}>
                  <text className="weekly-chart__value" x={point.x} y={Math.max(18, point.y - 14)}>
                    {point.accuracy}%
                  </text>
                  <text className="weekly-chart__emoji" x={point.x} y={point.y + 6}>{point.emoji}</text>
                  <text className="weekly-chart__day" x={point.x} y="168">{point.day}</text>
                  <title>{`${point.day}요일: ${point.correct}/${point.total} 정답, 정답률 ${point.accuracy}%`}</title>
                </g>
              ))}
            </svg>
          </div>
        </section>
      </div>
    </div>
  );
}

export default StatisticsPage;
