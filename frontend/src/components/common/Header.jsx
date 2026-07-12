import { Link, NavLink, useNavigate } from "react-router";

import Button from "./Button";
import "./Header.css";

function Header({ simple = false }) {
  const navigate = useNavigate();

  function getNavigationClass({ isActive }) {
    return isActive
      ? "header__navigation-link header__navigation-link--active"
      : "header__navigation-link";
  }

  function handleLogout() {
    const shouldLogout = window.confirm("로그아웃하시겠습니까?");

    if (shouldLogout) {
      localStorage.removeItem("accessToken");
      navigate("/login");
    }
  }

  return (
    <header className="header">
      <Link className="header__logo" to={simple ? "/login" : "/home"}>
        <span className="header__logo-icon">♧</span>

        <span>
          Sum<span className="header__logo-green">Quiz</span>
        </span>
      </Link>

      {simple ? (
        <div className="header__simple-menu">
          <button className="header__help" type="button">
            도움말
          </button>

          <Button variant="outline" onClick={() => navigate("/signup")}>
            회원가입
          </Button>
        </div>
      ) : (
        <>
          <nav className="header__navigation">
            <NavLink className={getNavigationClass} to="/home">
              홈
            </NavLink>

            <NavLink className={getNavigationClass} to="/upload">
              업로드
            </NavLink>

            <NavLink className={getNavigationClass} to="/summary">
              AI 요약
            </NavLink>

            <NavLink className={getNavigationClass} to="/quiz">
              퀴즈
            </NavLink>

            <NavLink className={getNavigationClass} to="/report">
              학습 리포트
            </NavLink>
          </nav>

          <div className="header__user">
            <button
              className="header__notification"
              type="button"
              aria-label="알림"
            >
              🔔
            </button>

            <button
              className="header__profile"
              type="button"
              onClick={handleLogout}
            >
              <span className="header__avatar">이름</span>
              <span className="header__user-name">사용자</span>
            </button>
          </div>
        </>
      )}
    </header>
  );
}

export default Header;
