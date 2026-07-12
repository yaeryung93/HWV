import { loginUser } from "../../services/authApi";
import { useState } from "react";
import { Link, useNavigate } from "react-router";

import Button from "../common/Button";
import "./LoginForm.css";

function LoginForm() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: "",
    password: "",
    rememberLogin: false,
  });

  const [errorMessage, setErrorMessage] = useState("");

  function handleInputChange(event) {
    const { name, value, type, checked } = event.target;

    setFormData((previousData) => ({
      ...previousData,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    console.log("로그인 버튼 클릭됨");

    if (formData.email.trim() === "") {
      setErrorMessage("이메일을 입력해 주세요.");
      return;
    }

    if (formData.password.trim() === "") {
      setErrorMessage("비밀번호를 입력해 주세요.");
      return;
    }

    setErrorMessage("");

    try {
      const result = await loginUser(formData.email, formData.password);

      console.log("로그인 성공", result);

      navigate("/home");
    } catch (error) {
      console.error(error);
      setErrorMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  }

  function handleGoogleLogin() {
    alert("Google 로그인은 나중에 백엔드와 연결합니다.");
  }

  return (
    <section className="login-form">
      <div className="login-form__heading">
        <h2>로그인</h2>

        <p>
          <strong>SumQuiz</strong>에 오신 것을 환영합니다!
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="login-form__field">
          <label htmlFor="email">이메일</label>

          <input
            id="email"
            name="email"
            type="email"
            value={formData.email}
            placeholder="이메일을 입력하세요"
            autoComplete="email"
            onChange={handleInputChange}
          />
        </div>

        <div className="login-form__field">
          <label htmlFor="password">비밀번호</label>

          <input
            id="password"
            name="password"
            type="password"
            value={formData.password}
            placeholder="비밀번호를 입력하세요"
            autoComplete="current-password"
            onChange={handleInputChange}
          />
        </div>

        {errorMessage && <p className="login-form__error">{errorMessage}</p>}

        <div className="login-form__options">
          <label className="login-form__remember">
            <input
              name="rememberLogin"
              type="checkbox"
              checked={formData.rememberLogin}
              onChange={handleInputChange}
            />

            <span>로그인 상태 유지</span>
          </label>

          <Link to="/find-password">비밀번호 찾기</Link>
        </div>

        <Button type="submit" fullWidth>
          로그인
        </Button>
      </form>

      <div className="login-form__divider">
        <span>또는</span>
      </div>

      <button
        className="login-form__google"
        type="button"
        onClick={handleGoogleLogin}
      >
        <span className="login-form__google-icon">G</span>
        Google로 로그인
      </button>

      <p className="login-form__signup">
        아직 계정이 없으신가요?
        <Link to="/signup"> 회원가입</Link>
      </p>
    </section>
  );
}

export default LoginForm;
