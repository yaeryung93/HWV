import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router";

import { getProblems } from "../services/problemApi";
import { clearSessionUser, getSessionUser } from "../services/session";
import "./AppLayout.css";

const navigationItems = [
  { to: "/dashboard", label: "대시보드", icon: "⌂" },
  { to: "/problems", label: "AI 문제", icon: "▣" },
  { to: "/wrong-notes", label: "오답노트", icon: "▤" },
  { to: "/statistics", label: "학습 통계", icon: "▥" },
  { to: "/profile", label: "마이페이지", icon: "○" },
];

function AppLayout() {
  const navigate = useNavigate();
  const [currentProblem, setCurrentProblem] = useState(null);
  const user = getSessionUser();
  const displayName = user?.name || "사용자";

  useEffect(() => {
    let active = true;

    getProblems().then((problems) => {
      if (active) {
        setCurrentProblem(
          problems.find((problem) => problem.progress > 0) || null,
        );
      }
    });

    return () => {
      active = false;
    };
  }, []);

  function getNavigationClass({ isActive }) {
    return isActive
      ? "lab-sidebar__link lab-sidebar__link--active"
      : "lab-sidebar__link";
  }

  function handleLogout() {
    localStorage.removeItem("accessToken");
    clearSessionUser();
    navigate("/login");
  }

  return (
    <div className="lab-shell">
      <header className="lab-header">
        <button
          type="button"
          className="lab-brand"
          onClick={() => navigate("/dashboard")}
        >
          <span className="lab-brand__mark">♣</span>
          <strong>HWV</strong>
        </button>

        <div className="lab-header__actions">
          <button
            type="button"
            className="lab-icon-button"
            aria-label="알림"
          >
            ♢
          </button>

          <button
            type="button"
            className="lab-profile-button"
            onClick={handleLogout}
            title="클릭하면 로그아웃합니다."
          >
            <span className="lab-avatar">{displayName.slice(0, 1)}</span>
            <span>{displayName}</span>
            <span>⌄</span>
          </button>
        </div>
      </header>

      <aside className="lab-sidebar">
        <button
          type="button"
          className="lab-create-button"
          onClick={() => navigate("/problems/new")}
        >
          <span>＋</span>
          새 문제 만들기
        </button>

        <nav className="lab-sidebar__navigation">
          {navigationItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={getNavigationClass}
            >
              <span className="lab-sidebar__icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        {currentProblem && (
          <div className="lab-progress-card">
            <span>현재 진행 중</span>
            <strong>{currentProblem.title}</strong>
            <div className="lab-progress-card__track">
              <span style={{ width: `${currentProblem.progress}%` }} />
            </div>
            <small>{currentProblem.progress}%</small>
          </div>
        )}
      </aside>

      <main className="lab-main">
        <Outlet />
      </main>
    </div>
  );
}

export default AppLayout;
