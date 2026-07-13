import Header from "../components/common/Header";
import LoginForm from "../components/login/LoginForm";

import "./LoginPage.css";

function LoginPage() {
  return (
    <div className="login-page">
      <Header simple />

      <main className="login-page__main">
        <section className="login-page__introduction">
          <b className="login-page__welcome">Help With Vision</b>

          <h1 className="login-page__title">
            Sum<span>Quiz</span>
          </h1>

          <p className="login-page__description">
            AI가 요약하고, 퀴즈로 학습해주는
            <br />
            당신만의 똑똑한 학습 파트너
          </p>

          <div className="login-page__illustration">
            <div className="login-page__book login-page__book--back" />

            <div className="login-page__book login-page__book--front">
              <span>AI</span>
            </div>

            <div className="login-page__card">
              <span className="login-page__card-line" />
              <span className="login-page__card-line" />
              <span className="login-page__card-line login-page__card-line--short" />
            </div>

            <span className="login-page__sparkle login-page__sparkle--one">
              ✦
            </span>

            <span className="login-page__sparkle login-page__sparkle--two">
              ✦
            </span>
          </div>
        </section>

        <LoginForm />
      </main>
    </div>
  );
}

export default LoginPage;