import Header from "../components/common/Header";
import SignupForm from "../components/signup/SignupForm";

import "./SignupPage.css";

function SignupPage() {
  return (
    <div className="signup-page">
      <Header simple />

      <main className="signup-page__main">
        <section className="signup-page__introduction">
          <p className="signup-page__eyebrow">Start with SumQuiz</p>

          <h1>
            학습 자료 하나로
            <br />
            <span>더 똑똑하게 공부하세요.</span>
          </h1>

          <p className="signup-page__description">
            PDF와 뉴스 기사 링크를 등록하면 AI가 핵심 내용을 요약하고, 나만의
            퀴즈와 플래시카드를 만들어드립니다.
          </p>

          <div className="signup-page__features">
            <div>
              <span>1</span>

              <p>
                <strong>PDF·링크 업로드</strong>
                학습할 자료를 간편하게 등록하세요.
              </p>
            </div>

            <div>
              <span>2</span>

              <p>
                <strong>AI 요약·퀴즈 생성</strong>
                핵심 요약과 맞춤형 문제를 받아보세요.
              </p>
            </div>

            <div>
              <span>3</span>

              <p>
                <strong>오답 복습과 학습 리포트</strong>
                나의 학습 기록과 부족한 부분을 확인하세요.
              </p>
            </div>
          </div>
        </section>

        <SignupForm />
      </main>
    </div>
  );
}

export default SignupPage;
