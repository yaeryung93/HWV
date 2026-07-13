import Header from "../components/common/Header";
import HeroSection from "../components/home/HeroSection";
import RecentFiles from "../components/home/RecentFiles";

import "./HomePage.css";

function HomePage() {
  return (
    <div className="home-page">
      <Header />

      <main className="home-page__main">
        <HeroSection />

        <section className="home-page__quick-menu">
          <div className="home-page__section-heading">
            <h2>빠른 메뉴</h2>
            <p>필요한 기능을 바로 시작해 보세요.</p>
          </div>

          <div className="home-page__menu-grid">
            <a href="/upload">
              <span>↑</span>
              <strong>자료 업로드</strong>
              <small>학습할 파일을 등록해요.</small>
            </a>

            <a href="/summary">
              <span>AI</span>
              <strong>AI 요약</strong>
              <small>핵심 내용을 정리해요.</small>
            </a>

            <a href="/quiz">
              <span>?</span>
              <strong>퀴즈 풀기</strong>
              <small>학습 결과를 확인해요.</small>
            </a>

            <a href="/report">
              <span>✓</span>
              <strong>학습 리포트</strong>
              <small>학습 기록을 분석해요.</small>
            </a>
          </div>
        </section>

        <RecentFiles />
      </main>

      <footer className="home-page__footer">
        © 2026 HWV. AI 코딩 학습 도우미
      </footer>
    </div>
  );
}

export default HomePage;
