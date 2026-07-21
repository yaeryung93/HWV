import { useEffect, useState } from "react";

import { connectGitHub, disconnectGitHub, getGitHubStatus, saveGitHubPublishToken } from "../../services/githubApi";
import { getSessionUser } from "../../services/session";
import "./LabPages.css";

function ProfilePage() {
  const user = getSessionUser();
  const displayName = user?.name || "사용자";
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [githubStatus, setGitHubStatus] = useState({ connected: false });
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
    getGitHubStatus().then(setGitHubStatus).catch(() => {});
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

  return (
    <div className="lab-page lab-page--narrow">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">MY PAGE</span>
          <h1>마이페이지</h1>
          <p>계정 정보와 학습 설정을 관리하세요.</p>
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
            <dd>업로드 시 자동 감지</dd>
          </div>
          <div>
            <dt>목표 난이도</dt>
            <dd>문제 생성 시 선택</dd>
          </div>
          <div>
            <dt>가입 상태</dt>
            <dd>활성</dd>
          </div>
        </dl>
      </section>

      <section className="surface-card github-card">
        <div>
          <span className="lab-page__eyebrow">GITHUB SOLUTIONS</span>
          <h2>GitHub 학습 기록 연동</h2>
          <p>통과한 문제 조건과 Solution 코드를 선택한 저장소에 커밋합니다.</p>
        </div>

        {githubStatus.connected ? (
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
