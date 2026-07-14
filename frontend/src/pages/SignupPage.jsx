import Header from "../components/common/Header";
import SignupForm from "../components/signup/SignupForm";

import "./SignupPage.css";

function SignupPage() {
  return (
    <div className="signup-page">
      <Header simple />

      <main className="signup-page__main">
        <section className="signup-page__introduction">
          <p className="signup-page__eyebrow">Start Java learning with HWV</p>

          <h1>
            Java 코드 한 파일로
            <br />
            <span>핵심 문법부터 퀴즈까지</span>
          </h1>

          <p className="signup-page__description">
            Java 파일을 업로드하면 AI가 코드의 핵심 문법 3개를 분석하고
            맞춤형 퀴즈 5개를 만들어드립니다.
          </p>

          <div className="signup-page__features">
            <div>
              <span>1</span>

              <p>
                <strong>Java 파일 업로드</strong>
                학습할 .java 파일을 간편하게 등록하세요.
              </p>
            </div>

            <div>
              <span>2</span>

              <p>
                <strong>AI 핵심 문법 분석</strong>
                코드에서 사용된 핵심 Java 문법 3개를 확인하세요.
              </p>
            </div>

            <div>
              <span>3</span>

              <p>
                <strong>맞춤 퀴즈와 학습 통계</strong>
                문제 5개를 풀고 정답과 오답을 확인하세요.
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
