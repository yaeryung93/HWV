import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";

import LanguageSelector from "../components/common/LanguageSelector";
import {
  WelcomeAboutSection,
  WelcomeBenefitsSection,
  WelcomeFlowSection,
  WelcomeHeroSection,
  WelcomeServerSection,
} from "../components/welcome/WelcomeSections";
import { useLanguage } from "../i18n/LanguageContext";
import "./WelcomePage.css";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "https://sumquiz.onrender.com").replace(/\/$/, "");

const copy = {
  ko: {
    heroTitle: "코드에서 시작되는", heroAccent: "AI 맞춤형 Java 학습",
    scroll: "스크롤하여 HWV 알아보기",
    aboutEyebrow: "ABOUT HWV",
    aboutTitle: <>HWV는 일반적인 <br/>Java 문제를 무작위로 <br/>제공하지 않습니다.</>,
    aboutDescription: "사용자가 직접 작성한 Java 코드에서 실제로 사용된 핵심 문법을 분석하고, 그 문법을 연습할 수 있는 맞춤형 코딩 문제를 생성합니다.",
    analysisFeatures: [
      { title: "Java 코드 분석", description: "업로드한 코드의 구조와 흐름을 살펴봅니다." },
      { title: "핵심 문법 3개 추출", description: "실제 코드에 쓰인 중요한 문법만 선별합니다." },
      { title: "맞춤형 문제 생성", description: "선택한 난이도에 맞춰 코딩 문제를 만듭니다." },
      { title: "실제 코드 실행 및 채점", description: "Java 코드를 실행해 테스트 케이스로 검증합니다." },
      { title: "오답노트 · 통계", description: "실패 원인과 문법별 학습 결과를 확인합니다." },
      { title: "연속 학습", description: "매일의 정답 기록으로 학습 흐름을 이어갑니다." },
    ],
    flowEyebrow: "HOW IT WORKS", flowTitle: "한 파일에서 시작하는 학습 흐름", flowDescription: "복잡한 설정 없이 Java 파일 하나만 업로드하면 됩니다.",
    flowSteps: [
      { title: "Java 파일 업로드", description: "학습할 .java 파일을 선택합니다." },
      { title: "핵심 문법 분석", description: "코드에서 실제 사용한 문법을 찾습니다." },
      { title: "맞춤형 문제 생성", description: "문법과 난이도에 맞는 문제를 만듭니다." },
      { title: "코드 실행 및 채점", description: "작성한 답안을 실제 테스트로 확인합니다." },
      { title: "학습 기록", description: "결과를 오답노트와 통계에 반영합니다." },
    ],
    benefitEyebrow: "WHY HWV", benefitTitle: "학습에 필요한 과정을 한곳에서",
    benefits: [
      { title: "실제 작성한 코드 분석", description: "교과서 예제가 아닌 내가 작성한 코드에서 학습을 시작합니다." },
      { title: "맞춤형 문제 생성", description: "발견된 문법과 선택한 난이도에 맞춘 문제를 제공합니다." },
      { title: "실제 Java 실행", description: "AI의 추측이 아니라 실제 실행 결과로 정답을 확인합니다." },
      { title: "학습 통계", description: "문법별 통과율과 최근 학습 흐름을 한눈에 확인합니다." },
      { title: "오답노트", description: "실패한 제출과 이해하기 쉬운 수정 방향을 다시 살펴봅니다." },
      { title: "연속 학습", description: "오늘의 학습 여부와 최고 연속 기록을 관리합니다." },
    ],
    audienceEyebrow: "FOR LEARNERS", audienceTitle: "이런 분께 추천합니다.",
    audiences: ["Java를 처음 배우는 학생", "부족한 문법을 복습하고 싶은 사람", "자신의 코드로 학습하고 싶은 사람", "반복 학습으로 실력을 키우고 싶은 사람"],
    audienceStatement: "내가 작성한 코드를 바탕으로 부족한 Java 문법을 연습하고 싶은 학습자를 위한 서비스입니다.",
    serverEyebrow: "READY TO START", serverTitle: "이제 나만의 Java 학습을 시작해보세요.",
    serverDescription: "무료 서버를 준비하는 동안 위에서 HWV의 학습 흐름을 확인할 수 있습니다.",
    serverStatus: "현재 서버 상태", ready: "Ready", preparing: "Preparing...", serverReady: "서버 준비 완료", preparingServer: "서버를 준비하고 있습니다...",
    start: "서버 시작하기", notice: "무료 서버는 첫 연결에 최대 1분 정도 걸릴 수 있습니다.",
  },
  en: {
    heroTitle: "Personalized Java learning", heroAccent: "that begins with your code",
    scroll: "Scroll to discover HWV",
    aboutEyebrow: "ABOUT HWV",
    aboutTitle: <>HWV does not give you<br />random, generic Java problems.</>,
    aboutDescription: "It analyzes the syntax actually used in your own Java code and creates personalized coding problems that help you practice it.",
    analysisFeatures: [
      { title: "Java code analysis", description: "Review the structure and flow of your uploaded code." },
      { title: "Three core concepts", description: "Select only the most important syntax used in the code." },
      { title: "Personalized problems", description: "Create coding problems at your selected difficulty." },
      { title: "Real execution and grading", description: "Run Java and verify it against test cases." },
      { title: "Wrong notes and stats", description: "Review failures and results by syntax." },
      { title: "Learning streak", description: "Keep learning with a daily correct-answer record." },
    ],
    flowEyebrow: "HOW IT WORKS", flowTitle: "A learning flow that starts with one file", flowDescription: "All you need is a Java file—no complicated setup required.",
    flowSteps: [
      { title: "Upload Java file", description: "Choose the .java file you want to learn from." },
      { title: "Analyze core syntax", description: "Find the syntax actually used in your code." },
      { title: "Generate problems", description: "Create problems for the syntax and level." },
      { title: "Run and grade", description: "Verify your answer with real test cases." },
      { title: "Track learning", description: "Save results to notes and statistics." },
    ],
    benefitEyebrow: "WHY HWV", benefitTitle: "Everything you need to keep learning",
    benefits: [
      { title: "Analyze your real code", description: "Start with code you wrote, not a generic textbook example." },
      { title: "Personalized problems", description: "Practice the detected syntax at a difficulty you choose." },
      { title: "Real Java execution", description: "Check answers using actual execution instead of AI guesses." },
      { title: "Learning statistics", description: "See pass rates by syntax and your recent learning flow." },
      { title: "Wrong-answer notes", description: "Revisit failed submissions with clear directions for improvement." },
      { title: "Learning streak", description: "Track today's completion and your longest streak." },
    ],
    audienceEyebrow: "FOR LEARNERS", audienceTitle: "HWV is a good fit for",
    audiences: ["Students learning Java for the first time", "Learners who want to review weak syntax", "People who want to learn from their own code", "Learners who improve through repetition"],
    audienceStatement: "HWV is for learners who want to practice weak Java concepts using code they wrote themselves.",
    serverEyebrow: "READY TO START", serverTitle: "Start your personalized Java learning.",
    serverDescription: "While the free server starts, explore how learning works with HWV above.",
    serverStatus: "Current server status", ready: "Ready", preparing: "Preparing...", serverReady: "Server ready", preparingServer: "Preparing server...",
    start: "Start server", notice: "The free server may take up to a minute for the first connection.",
  },
  ja: {
    heroTitle: "コードから始まる", heroAccent: "AIパーソナライズJava学習",
    scroll: "スクロールしてHWVを見る",
    aboutEyebrow: "ABOUT HWV",
    aboutTitle: <>HWVは一般的なJava問題を<br />ランダムに提供しません。</>,
    aboutDescription: "自分で作成したJavaコードで実際に使われている重要な文法を分析し、その文法を練習できるコーディング問題を生成します。",
    analysisFeatures: [
      { title: "Javaコード分析", description: "アップロードしたコードの構造と流れを確認します。" },
      { title: "重要な文法を3つ抽出", description: "実際に使われた重要な文法だけを選びます。" },
      { title: "カスタム問題生成", description: "選択した難易度に合わせて問題を作ります。" },
      { title: "実行と採点", description: "Javaコードを実行しテストケースで検証します。" },
      { title: "復習ノート・統計", description: "失敗の原因と文法別の学習結果を確認します。" },
      { title: "連続学習", description: "毎日の正解記録で学習を続けられます。" },
    ],
    flowEyebrow: "HOW IT WORKS", flowTitle: "1つのファイルから始まる学習", flowDescription: "複雑な設定なしでJavaファイルを1つアップロードするだけです。",
    flowSteps: [
      { title: "Javaファイルをアップロード", description: "学習する.javaファイルを選びます。" },
      { title: "重要な文法を分析", description: "コードで実際に使われた文法を探します。" },
      { title: "問題を生成", description: "文法と難易度に合う問題を作ります。" },
      { title: "実行・採点", description: "実際のテストで回答を確認します。" },
      { title: "学習記録", description: "結果を復習ノートと統計に反映します。" },
    ],
    benefitEyebrow: "WHY HWV", benefitTitle: "学習に必要な流れを一つの場所で",
    benefits: [
      { title: "自分のコードを分析", description: "教科書の例ではなく自分が作成したコードから学びます。" },
      { title: "カスタム問題生成", description: "検出された文法と選択した難易度に合わせます。" },
      { title: "実際のJava実行", description: "AIの推測ではなく実行結果で正解を確認します。" },
      { title: "学習統計", description: "文法別の合格率と最近の学習状況を確認します。" },
      { title: "復習ノート", description: "失敗した提出と分かりやすい改善方法を見直します。" },
      { title: "連続学習", description: "今日の学習と最長連続記録を管理します。" },
    ],
    audienceEyebrow: "FOR LEARNERS", audienceTitle: "こんな方におすすめです。",
    audiences: ["Javaを初めて学ぶ学生", "苦手な文法を復習したい方", "自分のコードで学習したい方", "反復学習で実力を伸ばしたい方"],
    audienceStatement: "自分が作成したコードをもとに、苦手なJava文法を練習したい学習者のためのサービスです。",
    serverEyebrow: "READY TO START", serverTitle: "自分だけのJava学習を始めましょう。",
    serverDescription: "無料サーバーを準備している間に、上のHWV学習フローをご覧ください。",
    serverStatus: "現在のサーバー状態", ready: "Ready", preparing: "Preparing...", serverReady: "サーバー準備完了", preparingServer: "サーバーを準備しています...",
    start: "サーバーを開始", notice: "無料サーバーの初回接続には最大1分ほどかかる場合があります。",
  },
};

function WelcomePage() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const text = copy[language] || copy.ko;
  const [isReady, setIsReady] = useState(false);
  const [progress, setProgress] = useState(30);
  const requestController = useRef(null);

  useEffect(() => {
    let active = true;
    const progressTimer = window.setInterval(() => setProgress((current) => current < 70 ? Math.min(70, current + 5) : current), 700);
    const delay = (milliseconds) => new Promise((resolve) => window.setTimeout(resolve, milliseconds));

    async function waitForServer() {
      while (active) {
        const controller = new AbortController();
        requestController.current = controller;
        const timeoutId = window.setTimeout(() => controller.abort(), 15000);
        try {
          const response = await fetch(`${API_BASE_URL}/health`, { cache: "no-store", signal: controller.signal });
          if (response.ok && active) {
            setProgress(100); setIsReady(true); window.clearInterval(progressTimer); return;
          }
        } catch { /* Render가 깨어나는 동안 다시 확인합니다. */ }
        finally { window.clearTimeout(timeoutId); }
        await delay(2500);
      }
    }
    waitForServer();
    return () => { active = false; window.clearInterval(progressTimer); requestController.current?.abort(); };
  }, []);

  useEffect(() => {
    const elements = document.querySelectorAll(".intro-reveal:not(.is-visible)");
    if (!("IntersectionObserver" in window)) {
      elements.forEach((element) => element.classList.add("is-visible"));
      return undefined;
    }

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.14, rootMargin: "0px 0px -40px" });

    elements.forEach((element) => observer.observe(element));
    return () => observer.disconnect();
  }, [language]);

  return (
    <div className="introduction-page">
      <div className="introduction-page__language"><LanguageSelector /></div>
      <main>
        <WelcomeHeroSection text={text} />
        <WelcomeAboutSection text={text} />
        <WelcomeFlowSection text={text} />
        <WelcomeBenefitsSection text={text} />
        <WelcomeServerSection
          text={text}
          isReady={isReady}
          progress={progress}
          onStart={() => navigate("/login")}
        />
      </main>
    </div>
  );
}

export default WelcomePage;
