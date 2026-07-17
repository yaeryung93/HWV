import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router";

import { getProblems } from "../services/problemApi";
import { clearSessionUser, getSessionUser } from "../services/session";
import "./AppLayout.css";

const navigationItems = [
  { to: "/dashboard", label: "대시보드", icon: "⌂" },
  { to: "/problems/new", label: "코드 분석", icon: "</>" },
  { to: "/quiz", label: "문제 풀이", icon: "✓" },
  { to: "/wrong-notes", label: "오답 노트", icon: "▤" },
  { to: "/statistics", label: "학습 통계", icon: "▥" },
  { to: "/profile", label: "마이페이지", icon: "♙" },
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
          className="lab-header__mobile-brand"
          onClick={() => navigate("/dashboard")}
          aria-label="HWV 대시보드"
        >
          <span>✱</span>
          HWV
        </button>

        <div className="lab-header__actions">
          <button type="button" className="lab-icon-button" aria-label="알림">
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
            <span className="lab-profile-button__chevron">⌄</span>
          </button>
        </div>
      </header>

      <aside className="lab-sidebar">
        <button
          type="button"
          className="lab-brand"
          onClick={() => navigate("/dashboard")}
        >
          <span className="lab-brand__mark">✱</span>
          <span className="lab-brand__copy">
            <strong>HWV</strong>
            <small>AI 기반 Java 학습 플랫폼</small>
          </span>
        </button>

        <button
          type="button"
          className="lab-create-button"
          onClick={() => navigate("/problems/new")}
        >
          <span>＋</span>
          새 프로젝트 만들기
        </button>

        <nav className="lab-sidebar__navigation" aria-label="주요 메뉴">
          {navigationItems.map((item) => (
            <NavLink key={item.to} to={item.to} className={getNavigationClass}>
              <span className="lab-sidebar__icon" aria-hidden="true">
                {item.icon}
              </span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="lab-sidebar__bottom">
          {currentProblem && (
            <div className="lab-progress-card">
              <span>현재 진행 중</span>
              <strong>{currentProblem.title}</strong>
              <div className="lab-progress-card__track">
                <span style={{ width: `${currentProblem.progress}%` }} />
              </div>
              <small>{currentProblem.progress}% 완료</small>
            </div>
          )}

          <div className="lab-upload-promo">
            <div>
              <strong>코드를 업로드하고</strong>
              <span>AI가 맞춤 문제를 생성해드려요!</span>
            </div>
            <div className="lab-upload-promo__art" aria-hidden="true">
              <span>↑</span>
            </div>
            <button type="button" onClick={() => navigate("/problems/new")}>
              코드 업로드하기
            </button>
          </div>

          <button
            type="button"
            className="lab-sidebar-user"
            onClick={() => navigate("/profile")}
          >
            <span className="lab-sidebar-user__avatar">
              {displayName.slice(0, 1)}
            </span>
            <span>
              <strong>{displayName}님</strong>
              <small>{user?.email || "HWV 학습자"}</small>
            </span>
            <span aria-hidden="true">⌄</span>
          </button>
        </div>
      </aside>

      <main className="lab-main">
        <Outlet />
      </main>
    </div>
  );
}

export default AppLayout;
