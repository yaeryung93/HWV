import { useRef, useState } from "react";
import { useNavigate } from "react-router";

import {
  analyzeJavaFile,
  createJavaQuiz,
} from "../../services/javaLearningApi";
import "./LabPages.css";

function ProblemCreatePage() {
  const fileInputRef = useRef(null);
  const navigate = useNavigate();

  const [selectedFile, setSelectedFile] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  function selectJavaFile(fileList) {
    const file = Array.from(fileList).find((item) =>
      item.name.toLowerCase().endsWith(".java"),
    );

    if (!file) {
      setSelectedFile(null);
      setAnalysis(null);
      setErrorMessage("확장자가 .java인 Java 파일만 업로드할 수 있습니다.");
      return;
    }

    setSelectedFile(file);
    setAnalysis(null);
    setErrorMessage("");
  }

  function handleFileChange(event) {
    selectJavaFile(event.target.files);
    event.target.value = "";
  }

  async function handleAnalyze(event) {
    event.preventDefault();

    if (!selectedFile) {
      setErrorMessage("분석할 Java 파일을 먼저 선택해 주세요.");
      return;
    }

    try {
      setIsAnalyzing(true);
      setErrorMessage("");
      setAnalysis(await analyzeJavaFile(selectedFile));
    } catch (error) {
      setErrorMessage(error.message || "Java 파일을 분석하지 못했습니다.");
    } finally {
      setIsAnalyzing(false);
    }
  }

  async function handleCreateQuiz() {
    if (!analysis) {
      setErrorMessage("Java 파일 분석을 먼저 완료해 주세요.");
      return;
    }

    try {
      setIsCreating(true);
      setErrorMessage("");
      await createJavaQuiz(analysis);
      navigate("/quiz");
    } catch (error) {
      setErrorMessage(error.message || "문제를 생성하지 못했습니다.");
    } finally {
      setIsCreating(false);
    }
  }

  return (
    <div className="lab-page lab-page--narrow">
      <div className="lab-page__heading">
        <div>
          <span className="lab-page__eyebrow">JAVA AI LEARNING</span>
          <h1>Java 파일 분석</h1>
          <p>
            Java 파일을 업로드하면 AI가 핵심 문법 3개를 분석하고 맞춤 문제
            5개를 만듭니다.
          </p>
        </div>
      </div>

      <form className="creation-card" onSubmit={handleAnalyze}>
        <section>
          <div className="section-heading">
            <span>1</span>
            <div>
              <h2>Java 파일 업로드</h2>
              <p>분석할 .java 파일 하나를 선택해 주세요.</p>
            </div>
          </div>

          <div
            className="project-dropzone"
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => {
              event.preventDefault();
              selectJavaFile(event.dataTransfer.files);
            }}
          >
            <strong>Java 파일을 끌어다 놓거나 아래 버튼으로 선택하세요.</strong>
            <span>.java 파일만 업로드할 수 있습니다.</span>
            <div>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
              >
                Java 파일 선택
              </button>
            </div>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept=".java"
            onChange={handleFileChange}
            hidden
          />

          {selectedFile && (
            <div className="selected-project">
              <div>
                <strong>파일 선택 완료</strong>
                <span className="selected-project__language">
                  분석 언어 <b>Java</b>
                </span>
                <span>{selectedFile.name}</span>
                <small>
                  {(selectedFile.size / 1024).toFixed(1)}KB · AI 분석을 시작할 수
                  있습니다.
                </small>
              </div>
              <button
                type="button"
                onClick={() => {
                  setSelectedFile(null);
                  setAnalysis(null);
                  setErrorMessage("");
                }}
              >
                초기화
              </button>
            </div>
          )}

          <button
            type="submit"
            className="creation-analyze-button"
            disabled={isAnalyzing || !selectedFile}
          >
            {isAnalyzing ? "AI가 Java 코드를 분석하고 있습니다..." : "AI 분석 시작"}
          </button>
        </section>

        {analysis && (
          <section className="analysis-result-section">
            <div className="section-heading">
              <span>2</span>
              <div>
                <h2>AI 분석 결과</h2>
                <p>업로드한 코드에서 찾은 핵심 Java 문법입니다.</p>
              </div>
            </div>

            <div className="grammar-card-grid">
              {analysis.grammars.map((grammar) => (
                <article className="grammar-card" key={grammar.name}>
                  <strong>{grammar.name}</strong>
                  <span
                    className="grammar-card__rating"
                    aria-label={`중요도 5점 중 ${grammar.rating}점`}
                  >
                    {"★".repeat(grammar.rating)}
                    <i>{"★".repeat(5 - grammar.rating)}</i>
                  </span>
                  <p>{grammar.description}</p>
                </article>
              ))}
            </div>

            <button
              type="button"
              className="creation-submit"
              disabled={isCreating}
              onClick={handleCreateQuiz}
            >
              {isCreating ? "AI가 문제 5개를 만들고 있습니다..." : "문제 5개 생성하기"}
            </button>
          </section>
        )}

        {errorMessage && (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        )}
      </form>
    </div>
  );
}

export default ProblemCreatePage;
