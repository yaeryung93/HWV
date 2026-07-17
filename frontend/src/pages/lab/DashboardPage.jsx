import { useEffect, useState } from "react";
import { Link } from "react-router";

import { getDashboardSummary, getProblems } from "../../services/problemApi";
import { getSessionUser } from "../../services/session";
import "./DashboardPage.css";

function formatAttemptDate(value) {
  if (!value) {
    return "최근 학습";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "최근 학습";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

function DashboardPage() {
  const [summary, setSummary] = useState(null);
  const [problems, setProblems] = useState([]);
  const user = getSessionUser();

  useEffect(() => {
    let active = true;

    Promise.all([getDashboardSummary(), getProblems()]).then(
      ([dashboardSummary, problemList]) => {
        if (active) {
          setSummary(dashboardSummary);
          setProblems(problemList);
        }
      },
    );

    return () => {
      active = false;
    };
  }, []);

  const generatedProblems = summary?.generatedProblems ?? 0;
  const correctAnswers = summary?.correctAnswers ?? 0;
  const incorrectAnswers = summary?.incorrectAnswers ?? 0;
  const accuracy = summary?.accuracy ?? 0;
  const recentAttempts = summary?.recentAttempts ?? [];

  const cards = [
    {
      label: "생성된 문제",
      value: generatedProblems,
      unit: "개",
      icon: "▤",
      tone: "green",
      trend: `▲ ${Math.max(generatedProblems, 0)}개 누적`,
    },
    {
      label: "맞힌 문제",
      value: correctAnswers,
      unit: "개",
      icon: "◆",
      tone: "blue",
      trend: `▲ ${correctAnswers}개 정답`,
    },
    {
      label: "오답 문제",
      value: incorrectAnswers,
      unit: "개",
      icon: "×",
      tone: "orange",
      trend: `▼ ${incorrectAnswers}개 복습 필요`,
    },
    {
      label: "정답률",
      value: accuracy,
      unit: "%",
      icon: "↗",
      tone: "purple",
      trend: `▲ 최근 학습 ${accuracy}%`,
    },
  ];

  const learningAreas = ["자료구조", "알고리즘", "입출력", "예외처리", "컬렉션"]
    .map((name, index) => ({
      name,
      value: accuracy ? Math.max(18, Math.min(100, accuracy - index * 5 + 8)) : 0,
    }));

  const today = new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(new Date());

  return (
    <div className="hwv-dashboard">
      <div className="hwv-dashboard__layout">
        <div className="hwv-dashboard__main-column">
          <section className="hwv-welcome">
            <div>
              <h1>
                안녕하세요{user?.name ? `, ${user.name}님!` : "!"} <span>👋</span>
              </h1>
              <p>오늘도 Java 실력을 한 단계 더 성장시켜보세요.</p>
            </div>

            <div className="hwv-robot-scene" aria-hidden="true">
              <span className="hwv-robot-scene__spark hwv-robot-scene__spark--one">
                ✦
              </span>
              <span className="hwv-robot-scene__spark hwv-robot-scene__spark--two">
                ✦
              </span>
              <div className="hwv-laptop">●</div>
              <div className="hwv-robot">
                <span className="hwv-robot__antenna" />
                <div className="hwv-robot__head">
                  <i />
                  <i />
                  <b>⌣</b>
                </div>
                <div className="hwv-robot__body" />
              </div>
            </div>
          </section>

          <section className="hwv-metric-grid" aria-label="학습 요약">
            {cards.map((card) => (
              <article className="hwv-metric-card" key={card.label}>
                <div className={`hwv-metric-card__icon hwv-metric-card__icon--${card.tone}`}>
                  {card.icon}
                </div>
                <div className="hwv-metric-card__content">
                  <span>{card.label}</span>
                  <strong>
                    {card.value}
                    <small>{card.unit}</small>
                  </strong>
                  <em className={card.tone === "orange" ? "is-orange" : ""}>
                    {card.trend}
                  </em>
                </div>
              </article>
            ))}
          </section>

          <div className="hwv-activity-grid">
            <section className="hwv-panel hwv-recent-card">
              <div className="hwv-panel__header">
                <h2>최근 분석한 코드</h2>
                <Link to="/problems">더보기 <span>›</span></Link>
              </div>

              {problems.length ? (
                <div className="hwv-code-list">
                  {problems.slice(0, 5).map((problem) => {
                    const fileName = problem.sourceFiles?.[0] || problem.title;
                    const extension = fileName.includes(".")
                      ? fileName.split(".").pop()
                      : problem.language || "java";

                    return (
                      <Link key={problem.id} to={`/problems/${problem.id}`}>
                        <span className="hwv-file-badge">.{extension}</span>
                        <strong>{fileName}</strong>
                        <time>{problem.progress || 0}% 학습</time>
                      </Link>
                    );
                  })}
                </div>
              ) : (
                <div className="hwv-empty-state">
                  아직 분석한 코드가 없습니다. 첫 Java 파일을 업로드해보세요.
                </div>
              )}
            </section>

            <section className="hwv-panel hwv-recent-card">
              <div className="hwv-panel__header">
                <h2>최근 푼 문제</h2>
                <Link to="/wrong-notes">더보기 <span>›</span></Link>
              </div>

              {recentAttempts.length ? (
                <div className="hwv-attempt-list">
                  {recentAttempts.slice(0, 5).map((attempt) => (
                    <Link key={attempt.id} to={`/problems/${attempt.problemId}`}>
                      <span
                        className={
                          attempt.passed
                            ? "hwv-attempt-dot is-correct"
                            : "hwv-attempt-dot is-wrong"
                        }
                      >
                        {attempt.passed ? "✓" : "!"}
                      </span>
                      <strong>{attempt.problemTitle}</strong>
                      <em className={attempt.passed ? "is-correct" : "is-wrong"}>
                        {attempt.passed ? "정답" : "오답"}
                      </em>
                      <time>{formatAttemptDate(attempt.submittedAt)}</time>
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="hwv-empty-state">
                  아직 문제 풀이 기록이 없습니다. 첫 퀴즈를 시작해보세요.
                </div>
              )}
            </section>
          </div>

          <section className="hwv-panel hwv-learning-card">
            <div className="hwv-panel__header">
              <h2>학습 통계</h2>
              <Link to="/statistics">자세히 보기 <span>›</span></Link>
            </div>

            <div className="hwv-learning-card__body">
              <div className="hwv-accuracy-summary">
                <span>전체 정답률</span>
                <div
                  className="hwv-accuracy-ring"
                  style={{ "--dashboard-accuracy": `${accuracy * 3.6}deg` }}
                >
                  <div>
                    <strong>{accuracy}%</strong>
                    <small>정답률</small>
                  </div>
                </div>
                <p>지난 학습 대비 <b>▲ {accuracy ? "성장 중" : "학습 시작"}</b></p>
              </div>

              <div className="hwv-area-chart" aria-label="영역별 정답률">
                <span>영역별 정답률</span>
                {learningAreas.map((area) => (
                  <div key={area.name}>
                    <label>{area.name}</label>
                    <div>
                      <b style={{ width: `${area.value}%` }} />
                    </div>
                    <strong>{area.value}%</strong>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section className="hwv-panel hwv-today-card">
            <div className="hwv-panel__header">
              <h2>오늘의 학습 현황</h2>
              <time>{today} ▣</time>
            </div>
            <div className="hwv-today-grid">
              <article>
                <span>생성한 문제</span>
                <strong>{generatedProblems}<small>개</small></strong>
                <em>전체 누적</em>
              </article>
              <article>
                <span>정답률</span>
                <strong>{accuracy}<small>%</small></strong>
                <em className="is-green">▲ 학습 기록</em>
              </article>
              <article>
                <span>제출 기록</span>
                <strong>{summary?.attempts ?? 0}<small>회</small></strong>
                <em>오늘도 도전해보세요</em>
              </article>
              <article>
                <span>완료한 문제</span>
                <strong>{summary?.solvedProblems ?? 0}<small>개</small></strong>
                <em>목표를 향해 성장 중</em>
              </article>
            </div>
          </section>
        </div>

        <aside className="hwv-dashboard__side-column">
          <section className="hwv-panel hwv-upload-card">
            <div>
              <h2>코드를 업로드하고<br />AI가 맞춤 문제를 생성해드려요!</h2>
              <div className="hwv-upload-illustration" aria-hidden="true">
                <span>↑</span>
              </div>
              <Link to="/problems/new">Java 파일 업로드하기</Link>
              <small>지원 파일 : .java</small>
            </div>
          </section>

          <section className="hwv-panel hwv-month-card">
            <h2>이번 달 학습 요약</h2>
            <dl>
              <div><dt>▤ <span>학습 문제</span></dt><dd>{generatedProblems}개</dd></div>
              <div><dt>◉ <span>평균 정답률</span></dt><dd>{accuracy}%</dd></div>
              <div><dt>◷ <span>제출 기록</span></dt><dd>{summary?.attempts ?? 0}회</dd></div>
              <div><dt>ϟ <span>완료한 문제</span></dt><dd>{summary?.solvedProblems ?? 0}개</dd></div>
            </dl>
          </section>

          <section className="hwv-panel hwv-recommend-card">
            <h2>추천 학습</h2>
            <div>
              <Link to="/problems/new">
                <span className="hwv-recommend-card__icon">&lt;/&gt;</span>
                <span><strong>코드 분석하기</strong><small>Java 파일을 업로드하고<br />AI가 핵심 포인트를 분석해요.</small></span>
                <b>›</b>
              </Link>
              <Link to="/quiz">
                <span className="hwv-recommend-card__icon">✓</span>
                <span><strong>문제 풀기</strong><small>AI가 생성한 맞춤 문제로<br />실력을 테스트해보세요.</small></span>
                <b>›</b>
              </Link>
              <Link to="/wrong-notes">
                <span className="hwv-recommend-card__icon">▤</span>
                <span><strong>오답 노트</strong><small>틀린 문제를 다시 확인하고<br />개념을 확실히 정리해요.</small></span>
                <b>›</b>
              </Link>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

export default DashboardPage;
