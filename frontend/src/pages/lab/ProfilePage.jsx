import { useEffect, useState } from "react";
import { useNavigate } from "react-router";

import { connectGitHub, disconnectGitHub, getGitHubStatus, saveGitHubPublishToken } from "../../services/githubApi";
import { clearSessionUser, getSessionUser } from "../../services/session";
import "./LabPages.css";

function ProfilePage() {
  const navigate = useNavigate();
  const user = getSessionUser();
  const displayName = user?.name || "사용자";
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [githubStatus, setGitHubStatus] = useState({ connected: false });
  const [isGithubStatusLoading, setIsGithubStatusLoading] = useState(true);
  const [isConnecting, setIsConnecting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [connectionCompleted] = useState(
    () => new URLSearchParams(window.location.search).get("github") === "connected",
  );

  useEffect(() => {
    const parameters = new URLSearchParams(window.location.search);
    const githubToken = parameters.get("github_token");
    if (githubToken) {
      saveGitHubPublishToken(githubToken);
      window.history.replaceState({}, "", "/profile?github=connected");
    }
    getGitHubStatus()
      .then(setGitHubStatus)
      .catch((error) => setErrorMessage(`GitHub 연결 상태를 확인하지 못했습니다. ${error.message}`))
      .finally(() => setIsGithubStatusLoading(false));
  }, []);

  async function handleConnect(event) {
    event.preventDefault();
    try {
      setIsConnecting(true);
      setErrorMessage("");
      const result = await connectGitHub(repositoryUrl);
      window.location.href = result.installUrl;
    } catch (error) {
      setErrorMessage(error.message);
      setIsConnecting(false);
    }
  }

  async function handleDisconnect() {
    try {
      await disconnectGitHub();
      setGitHubStatus({ connected: false });
      setRepositoryUrl("");
    } catch (error) {
      setErrorMessage(error.message);
    }
  }

  function handleLogout() {
    const shouldLogout = window.confirm("로그아웃하시겠습니까?");

    if (!shouldLogout) return;

    localStorage.removeItem("accessToken");
    clearSessionUser();
    navigate("/login", { replace: true });
  }

  return (
    <div className="lab-page lab-page--narrow">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">마이페이지</span>
          <h1>마이페이지</h1>
          <p>계정 정보와 GitHub 연동 상태를 관리하세요.</p>
        </div>
      </div>

      <section className="surface-card profile-card">
        <div className="profile-card__avatar">{displayName.slice(0, 1)}</div>
        <div>
          <h2>{displayName}</h2>
          <p>{user?.email || "로그인 이메일 정보 없음"}</p>
        </div>

        <dl>
          <div>
            <dt>주 사용 언어</dt>
            <dd>Java</dd>
          </div>
          <div>
            <dt>가입 상태</dt>
            <dd>활성</dd>
          </div>
        </dl>
      </section>

      <section className="surface-card account-card">
        <div>
          <span className="lab-page__eyebrow">계정</span>
          <h2>계정 관리</h2>
          <p>현재 기기에서 안전하게 로그아웃할 수 있습니다.</p>
        </div>
        <button type="button" onClick={handleLogout}>로그아웃</button>
      </section>

      <section className="surface-card github-card">
        <div>
          <span className="lab-page__eyebrow">GitHub</span>
          <h2>GitHub 학습 기록 연동</h2>
          <p>통과한 문제 조건과 Solution 코드를 선택한 저장소에 커밋합니다.</p>
        </div>

        {isGithubStatusLoading ? (
          <p className="github-status-loading" role="status">GitHub 연결 상태를 확인하고 있습니다.</p>
        ) : githubStatus.connected ? (
          <>
            {connectionCompleted && (
              <p className="github-success" role="status">GitHub 저장소 연동이 완료되었습니다.</p>
            )}
            <div className="github-connection">
              <div>
                <span className="github-connection__status">GitHub 연동됨</span>
                <strong>{githubStatus.owner}/{githubStatus.repository}</strong>
                <span>{githubStatus.privateRepository ? "Private 저장소" : "Public 저장소"}</span>
              </div>
              <a href={githubStatus.url} target="_blank" rel="noreferrer">저장소 보기</a>
              <button type="button" onClick={handleDisconnect}>연결 해제</button>
            </div>
          </>
        ) : (
          <form className="github-connect-form" onSubmit={handleConnect}>
            <label htmlFor="github-repository">GitHub 저장소 URL</label>
            <input
              id="github-repository"
              type="url"
              required
              placeholder="https://github.com/사용자명/저장소명"
              value={repositoryUrl}
              onChange={(event) => setRepositoryUrl(event.target.value)}
            />
            <button type="submit" disabled={isConnecting}>
              {isConnecting ? "GitHub로 이동하고 있습니다..." : "GitHub 저장소 연결"}
            </button>
          </form>
        )}
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
      </section>
    </div>
  );
}

export default ProfilePage;
