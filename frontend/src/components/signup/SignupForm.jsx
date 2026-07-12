import { useState } from "react";
import { Link, useNavigate } from "react-router";

import { signupUser } from "../../services/authApi";
import Button from "../common/Button";
import "./SignupForm.css";

function SignupForm() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    passwordConfirm: "",
  });

  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleInputChange(event) {
    const { name, value } = event.target;

    setFormData((previousData) => ({
      ...previousData,
      [name]: value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (formData.name.trim() === "") {
      setErrorMessage("이름을 입력해 주세요.");
      return;
    }

    if (formData.email.trim() === "") {
      setErrorMessage("이메일을 입력해 주세요.");
      return;
    }

    if (formData.password.trim() === "") {
      setErrorMessage("비밀번호를 입력해 주세요.");
      return;
    }

    if (formData.password.length < 8) {
      setErrorMessage("비밀번호는 8자 이상 입력해 주세요.");
      return;
    }

    if (formData.password !== formData.passwordConfirm) {
      setErrorMessage("비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      setErrorMessage("");
      setIsSubmitting(true);

      const result = await signupUser({
        name: formData.name,
        email: formData.email,
        password: formData.password,
      });

      console.log("회원가입 성공:", result);

      alert("회원가입이 완료되었습니다. 로그인해 주세요.");
      navigate("/login");
    } catch (error) {
      console.error("회원가입 오류:", error);
      setErrorMessage(error.message || "회원가입에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="signup-form">
      <div className="signup-form__heading">
        <h2>회원가입</h2>

        <p>
          <strong>SumQuiz</strong>와 함께 새로운 학습을 시작하세요.
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="signup-form__field">
          <label htmlFor="name">이름</label>

          <input
            id="name"
            name="name"
            type="text"
            value={formData.name}
            placeholder="이름을 입력하세요"
            autoComplete="name"
            onChange={handleInputChange}
          />
        </div>

        <div className="signup-form__field">
          <label htmlFor="signup-email">이메일</label>

          <input
            id="signup-email"
            name="email"
            type="email"
            value={formData.email}
            placeholder="이메일을 입력하세요"
            autoComplete="email"
            onChange={handleInputChange}
          />
        </div>

        <div className="signup-form__field">
          <label htmlFor="signup-password">비밀번호</label>

          <input
            id="signup-password"
            name="password"
            type="password"
            value={formData.password}
            placeholder="비밀번호를 8자 이상 입력하세요"
            autoComplete="new-password"
            onChange={handleInputChange}
          />
        </div>

        <div className="signup-form__field">
          <label htmlFor="password-confirm">비밀번호 확인</label>

          <input
            id="password-confirm"
            name="passwordConfirm"
            type="password"
            value={formData.passwordConfirm}
            placeholder="비밀번호를 다시 입력하세요"
            autoComplete="new-password"
            onChange={handleInputChange}
          />
        </div>

        {errorMessage && <p className="signup-form__error">{errorMessage}</p>}

        <Button type="submit" fullWidth disabled={isSubmitting}>
          {isSubmitting ? "가입 중..." : "회원가입"}
        </Button>
      </form>

      <p className="signup-form__login">
        이미 계정이 있으신가요?
        <Link to="/login"> 로그인</Link>
      </p>
    </section>
  );
}

export default SignupForm;
