import { useRef, useState } from "react";

import Header from "../components/common/Header";
import "./UploadPage.css";

const STORAGE_KEY = "sumquiz-uploaded-files";
const MAX_FILE_SIZE = 50 * 1024 * 1024;

function UploadCloudIcon() {
  return (
    <svg
      viewBox="0 0 64 64"
      className="upload-dropzone__icon"
      aria-hidden="true"
    >
      <path
        d="M20 48H15C8.9 48 4 43.1 4 37s4.9-11 11-11h1.2C18.1 17.9 25.2 12 33.5 12c9.3 0 17 7.3 17.5 16.5C56.2 29.8 60 34.5 60 40c0 6.6-5.4 12-12 12H38"
        fill="none"
        stroke="currentColor"
        strokeWidth="5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      <path
        d="M32 48V25M23 34l9-9 9 9"
        fill="none"
        stroke="currentColor"
        strokeWidth="5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function FileIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M6 2h8l4 4v16H6V2Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />

      <path
        d="M14 2v5h4"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14M10 11v6M14 11v6"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function LightIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M9 18h6M10 22h4M8.5 15.5A7 7 0 1 1 15.5 15.5C14.5 16.3 14 17 14 18h-4c0-1-.5-1.7-1.5-2.5Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function formatFileSize(bytes) {
  if (bytes < 1024) {
    return `${bytes}B`;
  }

<<<<<<< HEAD
=======
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)}KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString("ko-KR");
}

function getStoredFiles() {
  try {
    const savedFiles = localStorage.getItem(STORAGE_KEY);

    if (!savedFiles) {
      return [];
    }

    return JSON.parse(savedFiles);
  } catch (error) {
    console.error("업로드 내역을 불러오지 못했습니다.", error);
    return [];
  }
}

function UploadPage() {
  const fileInputRef = useRef(null);

  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadedFiles, setUploadedFiles] = useState(getStoredFiles);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  // 백엔드에서 받은 AI 요약 결과
  const [summary, setSummary] = useState("");

  // 업로드 오류 메시지
  const [errorMessage, setErrorMessage] = useState("");

  function saveUploadedFiles(files) {
    setUploadedFiles(files);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(files));
  }

  function selectFile(file) {
    if (!file) {
      return;
    }

    const isPdf =
      file.type === "application/pdf" ||
      file.name.toLowerCase().endsWith(".pdf");

    if (!isPdf) {
      alert("PDF 파일만 선택할 수 있습니다.");
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      alert("파일은 최대 50MB까지 업로드할 수 있습니다.");
      return;
    }

    setSelectedFile(file);

    // 새로운 파일을 선택하면 이전 결과 초기화
    setSummary("");
    setErrorMessage("");
  }

  function handleFileChange(event) {
    selectFile(event.target.files[0]);
  }

  function handleDragOver(event) {
    event.preventDefault();
    setIsDragging(true);
  }

  function handleDragLeave(event) {
    event.preventDefault();
    setIsDragging(false);
  }

  function handleDrop(event) {
    event.preventDefault();
    setIsDragging(false);

    const file = event.dataTransfer.files[0];
    selectFile(file);
  }

>>>>>>> ba1ecab (Netlify React 라우팅 설정)
  async function handleUpload() {
    if (!selectedFile) {
      alert("먼저 파일을 선택해 주세요.");
      return;
    }

    const formData = new FormData();
<<<<<<< HEAD
    formData.append("file", selectedFile);

    try {
      const response = await fetch(
          "https://sumquiz.onrender.com/pdf/summary",
          {
            method: "POST",
            body: formData,
          }
      );

      const summary = await response.text();

      console.log(summary);

      alert(summary);

    } catch (e) {
      console.error(e);
      alert("업로드 실패");
    }
=======

    // 백엔드에서 요구하는 이름이 file이므로 바꾸면 안 됨
    formData.append("file", selectedFile);

    try {
      setIsUploading(true);
      setSummary("");
      setErrorMessage("");

      const response = await fetch("https://sumquiz.onrender.com/pdf/summary", {
        method: "POST",

        // Content-Type을 직접 작성하면 안 됨
        // 브라우저가 multipart/form-data를 자동 설정함
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`PDF 요약에 실패했습니다. (${response.status})`);
      }

      // 백엔드 응답이 String이므로 text() 사용
      const summaryResult = await response.text();

      console.log("AI 요약 결과:", summaryResult);

      // 화면에 요약 결과 표시
      setSummary(summaryResult);

      const uploadedFile = {
        id:
          typeof crypto !== "undefined" && crypto.randomUUID
            ? crypto.randomUUID()
            : `${Date.now()}-${selectedFile.name}`,
        name: selectedFile.name,
        size: selectedFile.size,
        uploadedAt: new Date().toISOString(),
      };

      saveUploadedFiles([uploadedFile, ...uploadedFiles]);

      setSelectedFile(null);

      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }

      alert("PDF 요약이 완료되었습니다.");
    } catch (error) {
      console.error("PDF 업로드 오류:", error);

      setErrorMessage(
        error.message || "PDF를 요약하는 중 오류가 발생했습니다.",
      );
    } finally {
      setIsUploading(false);
    }
  }

  function handleDelete(fileId) {
    const shouldDelete = window.confirm(
      "업로드 목록에서 이 파일을 삭제하시겠습니까?",
    );

    if (!shouldDelete) {
      return;
    }

    const nextFiles = uploadedFiles.filter((file) => file.id !== fileId);
    saveUploadedFiles(nextFiles);
  }

  function handleLinkTab() {
    alert("링크 업로드 화면은 아직 연결되지 않았습니다.");
>>>>>>> ba1ecab (Netlify React 라우팅 설정)
  }

  return (
    <div className="page upload-page">
      <Header />

      <main className="upload-page__container">
        <header className="upload-page__heading">
          <h1>자료 업로드</h1>

          <p>
            PDF 파일 또는 링크를 업로드하시면
            <br />
            AI가 핵심 내용을 요약해 드립니다!
          </p>
        </header>

        <section className="upload-tabs">
          <button type="button" className="upload-tab upload-tab--active">
            PDF 업로드
          </button>

          <button type="button" className="upload-tab" onClick={handleLinkTab}>
            링크 업로드
          </button>
        </section>

        <section className="upload-section">
          <input
            ref={fileInputRef}
            id="pdf-file-input"
            type="file"
            accept=".pdf,application/pdf"
            onChange={handleFileChange}
            hidden
          />

          <div
            className={`upload-dropzone ${
              isDragging ? "upload-dropzone--dragging" : ""
            }`}
            role="button"
            tabIndex={0}
            onClick={() => fileInputRef.current?.click()}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                fileInputRef.current?.click();
              }
            }}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <UploadCloudIcon />

            <strong>PDF 파일을 드래그하거나 클릭하여 업로드하세요</strong>

            <span>PDF, 최대 50MB</span>
          </div>

          {selectedFile && (
            <div className="selected-file">
              <div className="selected-file__icon">
                <FileIcon />
              </div>

              <div className="selected-file__information">
                <strong>{selectedFile.name}</strong>
                <span>{formatFileSize(selectedFile.size)}</span>
              </div>

              <button
                type="button"
                className="selected-file__upload-button"
                onClick={handleUpload}
                disabled={isUploading}
              >
                {isUploading ? "AI 요약 중..." : "AI 요약 시작"}
              </button>
            </div>
          )}

          {isUploading && (
            <p
              style={{
                marginTop: "18px",
                color: "var(--color-primary)",
                textAlign: "center",
                fontWeight: "700",
              }}
            >
              PDF를 분석하고 있습니다. 잠시 기다려 주세요.
            </p>
          )}

          {errorMessage && (
            <p
              style={{
                marginTop: "18px",
                color: "var(--color-danger)",
                textAlign: "center",
              }}
            >
              {errorMessage}
            </p>
          )}
        </section>

        {/* 백엔드에서 받은 AI 요약 결과 */}
        {summary && (
          <section
            className="upload-summary"
            style={{
              marginTop: "32px",
              padding: "30px",
              backgroundColor: "#ffffff",
              border: "1px solid var(--color-border)",
              borderRadius: "16px",
              boxShadow: "var(--shadow-card)",
            }}
          >
            <h2
              style={{
                marginBottom: "20px",
                fontSize: "24px",
              }}
            >
              AI 핵심 요약
            </h2>

            <p
              style={{
                margin: 0,
                color: "var(--color-text)",
                lineHeight: "1.9",
                whiteSpace: "pre-wrap",
              }}
            >
              {summary}
            </p>
          </section>
        )}

        <section className="upload-history">
          <h2>업로드한 파일</h2>

          {uploadedFiles.length === 0 ? (
            <div className="upload-history__empty">
              아직 업로드한 파일이 없습니다.
            </div>
          ) : (
            <div className="upload-history__list">
              {uploadedFiles.map((file) => (
                <article className="uploaded-file" key={file.id}>
                  <div className="uploaded-file__icon">
                    <FileIcon />
                  </div>

                  <div className="uploaded-file__information">
                    <strong>{file.name}</strong>

                    <span>
                      {formatDate(file.uploadedAt)}
                      <b>·</b>
                      {formatFileSize(file.size)}
                    </span>
                  </div>

                  <span className="uploaded-file__status">완료</span>

                  <button
                    type="button"
                    className="uploaded-file__delete"
                    aria-label={`${file.name} 삭제`}
                    onClick={() => handleDelete(file.id)}
                  >
                    <TrashIcon />
                  </button>
                </article>
              ))}
            </div>
          )}
        </section>

        <aside className="upload-tip">
          <div className="upload-tip__title">
            <span className="upload-tip__icon">
              <LightIcon />
            </span>

            <strong>Tip!</strong>
          </div>

          <ul>
            <li>더 좋은 요약 결과를 위해 텍스트가 많은 PDF를 사용해주세요.</li>
            <li>스캔 파일의 경우 요약 정확도가 낮을 수 있습니다.</li>
          </ul>
        </aside>
      </main>
    </div>
  );
}

export default UploadPage;
