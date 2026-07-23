import { useEffect, useState } from "react";
import { Link } from "react-router";

import { getDashboardSummary, getStudyStreak } from "../../services/problemApi";
import { getSessionUser } from "../../services/session";
import { useLanguage } from "../../i18n/LanguageContext";
import "./LabPages.css";

function DashboardPage() {
  const { language, t } = useLanguage();
  const [summary, setSummary] = useState(null);
  const [streak, setStreak] = useState({ currentStreak: 0, longestStreak: 0, lastStudyDate: null, studiedToday: false });
  const [isStreakLoading, setIsStreakLoading] = useState(true);
  const [streakUnavailable, setStreakUnavailable] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const user = getSessionUser();

  const copy = {
    ko: {
      loadFailed: "학습 현황을 불러오지 못했습니다.", greeting: (name) => `안녕하세요, ${name}님`, user: "사용자",
      intro: "Java 파일의 핵심 문법 3개를 분석해 문법별 코딩 문제를 만들어 줍니다.", count: "개",
      cards: [["생성된 문제", "누적 생성 문제"], ["AI 예상 성공", "통과한 제출"], ["보완 필요", "검토가 필요해요"], ["AI 예상 통과율", "누적 학습 결과"]],
      recent: "최근 제출", viewAll: "전체 보기", test: "테스트", passedTests: "통과", success: "성공", improve: "보완 필요",
      empty: "아직 제출 기록이 없습니다. Java 파일을 업로드해 첫 문제를 만들어 보세요.", flow: "학습 흐름", complete: "완료", pending: "진행 전", create: "새 문제 만들기",
      streakTitle: (days) => `🔥 ${days}일 연속 학습 중`, streakStart: "🔥 오늘부터 연속 학습을 시작해 보세요",
      longest: "최고 기록", streakDays: "일", studied: "오늘의 학습을 완료했어요!", notStudied: "오늘 문제를 풀고 기록을 이어가세요.",
      streakLoading: "연속 학습 기록을 불러오고 있습니다.", streakUnavailable: "연속 학습 기록을 불러오지 못해 기본값을 표시합니다.",
      steps: [["학습할 Java 파일 업로드", "Java 파일을 업로드하여 분석을 시작하세요."], ["AI가 핵심 Java 문법 3개 분석", "업로드한 코드에서 실제 사용된 문법을 추출합니다."], ["핵심 문법마다 코딩 문제 1개씩 생성", "문법별 코딩 문제를 총 3개 생성합니다."], ["코드를 작성하고 실행 결과 확인", "테스트 결과와 피드백으로 실력을 향상하세요."]],
    },
    en: {
      loadFailed: "We couldn't load your learning dashboard.", greeting: (name) => `Hello, ${name}`, user: "Learner",
      intro: "Analyze three core Java concepts from your file and get one coding problem for each.", count: "",
      cards: [["Generated problems", "Total problems"], ["Expected passes", "Passed submissions"], ["Needs improvement", "Review recommended"], ["Expected pass rate", "Overall learning result"]],
      recent: "Recent submissions", viewAll: "View all", test: "Tests", passedTests: "passed", success: "Passed", improve: "Needs improvement",
      empty: "No submissions yet. Upload a Java file and create your first problem.", flow: "Learning flow", complete: "Complete", pending: "Not started", create: "Create new problems",
      streakTitle: (days) => `🔥 ${days}-day learning streak`, streakStart: "🔥 Start your learning streak today",
      longest: "Longest streak", streakDays: " days", studied: "You completed today's learning!", notStudied: "Solve a problem today to keep your streak going.",
      streakLoading: "Loading your learning streak.", streakUnavailable: "Showing default values because the streak could not be loaded.",
      steps: [["Upload a Java file", "Upload a Java file to begin the analysis."], ["AI analyzes 3 core Java concepts", "Extract syntax actually used in the uploaded code."], ["Create one coding problem per concept", "Generate three coding problems in total."], ["Write code and check the results", "Improve with test results and feedback."]],
    },
    ja: {
      loadFailed: "学習状況を読み込めませんでした。", greeting: (name) => `こんにちは、${name}さん`, user: "ユーザー",
      intro: "Javaファイルから重要な文法を3つ分析し、文法ごとにコーディング問題を作成します。", count: "問",
      cards: [["生成した問題", "累計生成問題"], ["AI予想成功", "合格した提出"], ["改善が必要", "復習が必要です"], ["AI予想合格率", "累計学習結果"]],
      recent: "最近の提出", viewAll: "すべて見る", test: "テスト", passedTests: "合格", success: "成功", improve: "改善が必要",
      empty: "提出履歴がありません。Javaファイルをアップロードして最初の問題を作りましょう。", flow: "学習の流れ", complete: "完了", pending: "未開始", create: "新しい問題を作る",
      streakTitle: (days) => `🔥 ${days}日連続学習中`, streakStart: "🔥 今日から連続学習を始めましょう",
      longest: "最高記録", streakDays: "日", studied: "今日の学習を完了しました！", notStudied: "今日問題を解いて記録を続けましょう。",
      streakLoading: "連続学習記録を読み込んでいます。", streakUnavailable: "記録を読み込めなかったため、初期値を表示しています。",
      steps: [["学習するJavaファイルをアップロード", "Javaファイルをアップロードして分析を始めます。"], ["AIが重要なJava文法を3つ分析", "アップロードしたコードで実際に使われた文法を抽出します。"], ["文法ごとに問題を1問ずつ生成", "コーディング問題を合計3問生成します。"], ["コードを書いて実行結果を確認", "テスト結果とフィードバックで実力を伸ばします。"]],
    },
  }[language] || null;

  useEffect(() => {
    let active = true;

    getDashboardSummary().then((result) => {
      if (active) {
        setSummary(result);
      }
    }).catch((error) => { if (active) setErrorMessage(error.message); })
      .finally(() => { if (active) setIsLoading(false); });
    getStudyStreak().then((result) => {
      if (active) {
        setStreak(result);
        setStreakUnavailable(false);
      }
    }).catch(() => {
      if (active) {
        setStreak({ currentStreak: 0, longestStreak: 0, lastStudyDate: null, studiedToday: false });
        setStreakUnavailable(true);
      }
    }).finally(() => { if (active) setIsStreakLoading(false); });

    return () => {
      active = false;
    };
  }, [reloadKey]);

  function handleReload() {
    setIsLoading(true);
    setErrorMessage("");
    setIsStreakLoading(true);
    setStreakUnavailable(false);
    setReloadKey((value) => value + 1);
  }

  const cards = [
    {
      label: copy.cards[0][0],
      value: summary?.generatedProblems ?? 0,
      unit: copy.count,
      icon: "▤",
      description: copy.cards[0][1],
    },
    {
      label: copy.cards[1][0],
      value: summary?.correctAnswers ?? 0,
      unit: copy.count,
      icon: "✦",
      description: copy.cards[1][1],
    },
    {
      label: copy.cards[2][0],
      value: summary?.incorrectAnswers ?? 0,
      unit: copy.count,
      icon: "!",
      description: copy.cards[2][1],
    },
    {
      label: copy.cards[3][0],
      value: summary?.accuracy ?? 0,
      unit: "%",
      icon: "◎",
      description: copy.cards[3][1],
    },
  ];

  const hasProblems = (summary?.generatedProblems ?? 0) > 0;
  const hasAttempts = (summary?.correctAnswers ?? 0) + (summary?.incorrectAnswers ?? 0) > 0;
  const learningSteps = copy.steps.map(([title, description], index) => ({ title, description, completed: index < 3 ? hasProblems : hasAttempts }));

  if (isLoading) {
    return <div className="lab-page"><section className="large-empty">{t("loadingDashboard")}</section></div>;
  }

  if (errorMessage && !summary) {
    return (
      <div className="lab-page"><section className="large-empty">
        <strong>{copy.loadFailed}</strong><span>{errorMessage}</span>
        <button type="button" className="lab-primary-link" onClick={handleReload}>{t("retry")}</button>
      </section></div>
    );
  }

  return (
    <div className="lab-page">
      <div className="lab-page__heading">
        <div>
          <span className="dashboard-kicker">HWV CODE LAB</span>
          <h1>
            {copy.greeting(user?.name || copy.user)} <span aria-hidden="true">👋</span>
          </h1>
          <p>
            {copy.intro}
          </p>
        </div>
      </div>

      {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}

      <section className="metric-grid">
        {cards.map((card) => (
          <article className="metric-card dashboard-metric-card" key={card.label}>
            <div>
              <span>{card.label}</span>
              <strong>
                {card.value}
                <small>{card.unit}</small>
              </strong>
              <small className="dashboard-metric-card__description">{card.description}</small>
            </div>
            <b aria-hidden="true">{card.icon}</b>
          </article>
        ))}
      </section>

      <section className="surface-card dashboard-streak-card" aria-live="polite">
        <div className="dashboard-streak-card__icon" aria-hidden="true">🔥</div>
        <div className="dashboard-streak-card__content">
          <h2>
            {streak.currentStreak > 0
              ? copy.streakTitle(streak.currentStreak)
              : copy.streakStart}
          </h2>
          <p>
            {isStreakLoading
              ? copy.streakLoading
              : streakUnavailable
                ? copy.streakUnavailable
                : streak.studiedToday ? copy.studied : copy.notStudied}
          </p>
        </div>
        <dl>
          <div>
            <dt>{copy.longest}</dt>
            <dd>{streak.longestStreak}<small>{copy.streakDays}</small></dd>
          </div>
        </dl>
      </section>

      <div className="dashboard-grid">
        <section className="surface-card">
          <div className="surface-card__header">
            <h2>{copy.recent}</h2>
            <Link to="/wrong-notes">{copy.viewAll}</Link>
          </div>

          {summary?.recentAttempts?.length ? (
            <div className="activity-list">
              {summary.recentAttempts.map((attempt) => (
                <article key={attempt.id}>
                  <b className={attempt.passed ? "activity-status-icon activity-status-icon--passed" : "activity-status-icon"}>
                    {attempt.passed ? "✓" : "!"}
                  </b>
                  <div>
                    <Link to={`/problems/${attempt.problemId}`}><strong>{attempt.problemTitle}</strong></Link>
                    <span>
                      {copy.test} {attempt.passedCount}/{attempt.totalCount} {copy.passedTests}
                    </span>
                  </div>
                  <span
                    className={
                      attempt.passed
                        ? "result-pill result-pill--passed"
                        : "result-pill result-pill--failed"
                    }
                  >
                    {attempt.passed ? copy.success : copy.improve}
                  </span>
                </article>
              ))}
            </div>
          ) : (
            <div className="compact-empty">
              {copy.empty}
            </div>
          )}
        </section>

        <section className="surface-card dashboard-guide">
          <h2>{copy.flow}</h2>
          <ol>
            {learningSteps.map((step, index) => (
              <li key={step.title} className={step.completed ? "dashboard-guide__step dashboard-guide__step--completed" : "dashboard-guide__step"}>
                <b>{index + 1}</b>
                <span><strong>{step.title}</strong><small>{step.description}</small></span>
                <i aria-label={step.completed ? copy.complete : copy.pending}>{step.completed ? "✓" : "○"}</i>
              </li>
            ))}
          </ol>
          <Link className="dashboard-guide__action" to="/problems/new">{copy.create}</Link>
        </section>
      </div>
    </div>
  );
}

export default DashboardPage;
