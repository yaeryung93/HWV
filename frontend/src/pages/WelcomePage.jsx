import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { BarChart3, Code2, Sparkles } from "lucide-react";

import "./WelcomePage.css";

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "https://sumquiz.onrender.com"
).replace(/\/$/, "");

const features = [
  {
    icon: Code2,
    title: "Java 코드 분석",
    description: "업로드한 Java 코드를 분석하여 핵심 문법을 자동 추출합니다.",
  },
  {
    icon: Sparkles,
    title: "AI 문제 생성",
    description: "학습 수준에 맞는 코딩 문제를 자동 생성합니다.",
  },
  {
    icon: BarChart3,
    title: "학습 기록 관리",
    description: "풀이 결과와 성장 과정을 한눈에 확인할 수 있습니다.",
  },
];

function WelcomePage() {
  const navigate = useNavigate();
  const [isReady, setIsReady] = useState(false);
  const [progress, setProgress] = useState(30);
  const requestController = useRef(null);

  useEffect(() => {
    let active = true;

    const progressTimer = window.setInterval(() => {
      setProgress((current) => (current < 70 ? Math.min(70, current + 5) : current));
    }, 700);

    const delay = (milliseconds) => new Promise((resolve) => {
      window.setTimeout(resolve, milliseconds);
    });

    async function waitForServer() {
      while (active) {
        const controller = new AbortController();
        requestController.current = controller;
        const timeoutId = window.setTimeout(() => controller.abort(), 15000);

        try {
          const response = await fetch(`${API_BASE_URL}/health`, {
            cache: "no-store",
            signal: controller.signal,
          });

          if (response.ok && active) {
            setProgress(100);
            setIsReady(true);
            window.clearInterval(progressTimer);
            return;
          }
        } catch {
          // Render가 깨어나는 동안 재시도합니다.
        } finally {
          window.clearTimeout(timeoutId);
        }

        await delay(2500);
      }
    }

    waitForServer();

    return () => {
      active = false;
      window.clearInterval(progressTimer);
      requestController.current?.abort();
    };
  }, []);

  return (
    <div className="introduction-page">
      <main className="introduction-page__layout">
        <section className="introduction-page__content">
          <header className="introduction-page__header">
            <img src="/images/hwv-logo-cutout.png" alt="HWV" />
          </header>

          <div className="introduction-page__hero">
            <p className="introduction-page__eyebrow">HELP WITH VISION</p>
            <h1>
              Java 코드 한 파일로
              <br />
              <span>AI 맞춤형 학습</span>을
              <br />
              시작하세요.
            </h1>
            <p className="introduction-page__description">
              HWV는 Java 코드를 분석하여 핵심 문법을 이해하고,
              <br />
              개인 맞춤형 코딩 문제와 학습 콘텐츠를 제공합니다.
            </p>
          </div>

          <div className="introduction-page__cards">
            {features.map(({ icon: Icon, title, description }, index) => (
              <article
                className="introduction-page__card"
                style={{ "--card-delay": `${index * 0.1 + 0.2}s` }}
                key={title}
              >
                <span><Icon aria-hidden="true" size={22} strokeWidth={1.8} /></span>
                <h2>{title}</h2>
                <p>{description}</p>
              </article>
            ))}
          </div>
        </section>

        <aside className="introduction-page__visual" aria-label="서버 준비 상태">
          <img className="introduction-page__large-logo" src="/images/hwv-logo-cutout.png" alt="" />

          <div className="introduction-page__server">
            <p className={isReady ? "is-ready" : "is-preparing"}>
              <i />
              Server Status
            </p>
            <strong>{isReady ? "Ready" : "Preparing..."}</strong>
          </div>

          <div className="introduction-page__progress-area">
            <div className="introduction-page__progress-label">
              <span>{isReady ? "Server ready" : "Preparing server..."}</span>
              <b>{progress}%</b>
            </div>
            <div
              className="introduction-page__progress-track"
              role="progressbar"
              aria-valuemin="0"
              aria-valuemax="100"
              aria-valuenow={progress}
            >
              <span style={{ width: `${progress}%` }} />
            </div>
          </div>

          <button
            className="introduction-page__button"
            type="button"
            disabled={!isReady}
            onClick={() => navigate("/login")}
          >
            {isReady ? "로그인 시작하기" : "Preparing..."}
          </button>

          <p className="introduction-page__notice">
            무료 시연 서버는 첫 연결에 최대 1분 정도 걸릴 수 있습니다.
          </p>
        </aside>
      </main>
    </div>
  );
}

export default WelcomePage;
