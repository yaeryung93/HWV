import Header from "../components/common/Header";
import SignupForm from "../components/signup/SignupForm";

import "./SignupPage.css";

function SignupPage() {
  return (
    <div className="signup-page">
      <Header simple />

      <main className="signup-page__main">
        <section className="signup-page__introduction">
          <p className="signup-page__eyebrow">Start coding with HWV</p>

          <h1>
            학습 자료에서 시작하는
            <br />
            <span>나만의 AI 코딩 연습</span>
          </h1>

          <p className="signup-page__description">
            소스코드, 프로젝트 폴더 또는 PDF를 업로드하면 AI가 사용 언어와
            내용을 분석해 난이도별 코딩 문제를 만들어드립니다.
          </p>

          <div className="signup-page__features">
            <div>
              <span>1</span>

              <p>
                <strong>파일·폴더·PDF 업로드</strong>
                공부할 코드와 자료를 한 번에 등록하세요.
              </p>
            </div>

            <div>
              <span>2</span>

              <p>
                <strong>AI 코딩 문제 생성</strong>
                언어와 난이도에 맞는 문제를 받아보세요.
              </p>
            </div>

            <div>
              <span>3</span>

              <p>
                <strong>코드 실행·맞춤 피드백</strong>
                테스트 결과와 AI 힌트로 부족한 점을 보완하세요.
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
