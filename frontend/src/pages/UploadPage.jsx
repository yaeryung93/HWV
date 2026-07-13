import { useRef, useState } from "react";

import Header from "../components/common/Header";
import { summarizePdf } from "../services/pdfApi";
import "./UploadPage.css";

const STORAGE_KEY = "sumquiz-uploaded-items";
const MAX_FILE_SIZE = 50 * 1024 * 1024;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

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

function LinkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M10.5 13.5a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1.7 1.7"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
      <path
        d="M13.5 10.5a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1.7-1.7"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
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
    return bytes + "B";
  }

  if (bytes < 1024 * 1024) {
    return (bytes / 1024).toFixed(1) + "KB";
  }

  return (bytes / (1024 * 1024)).toFixed(1) + "MB";
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString("ko-KR");
}

function getStoredItems() {
  try {
    const storedItems = localStorage.getItem(STORAGE_KEY);
    return storedItems ? JSON.parse(storedItems) : [];
  } catch (error) {
    console.error("업로드 내역을 불러오지 못했습니다.", error);
    return [];
  }
}

function UploadPage() {
  const fileInputRef = useRef(null);

  const [activeTab, setActiveTab] = useState("link");
  const [selectedFile, setSelectedFile] = useState(null);
  const [linkUrl, setLinkUrl] = useState("");
  const [uploadedItems, setUploadedItems] = useState(getStoredItems);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [summary, setSummary] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  function changeTab(nextTab) {
    setActiveTab(nextTab);
    setSummary("");
    setErrorMessage("");
  }

  function addUploadedItem(item) {
    setUploadedItems((currentItems) => {
      const nextItems = [item, ...currentItems];
      localStorage.setItem(STORAGE_KEY, JSON.stringify(nextItems));
      return nextItems;
    });
  }

  function selectFile(file) {
    if (!file) {
      return;
    }

    const isPdf =
      file.type === "application/pdf" ||
      file.name.toLowerCase().endsWith(".pdf");

    if (!isPdf) {
      setErrorMessage("PDF 파일만 선택할 수 있습니다.");
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setErrorMessage("파일은 최대 50MB까지 업로드할 수 있습니다.");
      return;
    }

    setSelectedFile(file);
    setSummary("");
    setErrorMessage("");
  }

  function handleFileChange(event) {
    selectFile(event.target.files[0]);
  }

  function handleDrop(event) {
    event.preventDefault();
    setIsDragging(false);
    selectFile(event.dataTransfer.files[0]);
  }

  async function handlePdfUpload() {
    if (!selectedFile) {
      setErrorMessage("먼저 PDF 파일을 선택해 주세요.");
      return;
    }

    try {
      setIsUploading(true);
      setSummary("");
      setErrorMessage("");

      const summaryResult = await summarizePdf(selectedFile);
      setSummary(summaryResult);

      addUploadedItem({
        id: crypto.randomUUID(),
        type: "pdf",
        name: selectedFile.name,
        detail: formatFileSize(selectedFile.size),
        uploadedAt: new Date().toISOString(),
      });

      setSelectedFile(null);

      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    } catch (error) {
      console.error("PDF 업로드 오류:", error);
      setErrorMessage(
        error.message || "PDF를 요약하는 중 오류가 발생했습니다.",
      );
    } finally {
      setIsUploading(false);
    }
  }

  async function handleLinkUpload(event) {
    event.preventDefault();

    let normalizedUrl;

    try {
      normalizedUrl = new URL(linkUrl.trim());

      if (!["http:", "https:"].includes(normalizedUrl.protocol)) {
        throw new Error("invalid protocol");
      }
    } catch {
      setErrorMessage("올바른 HTTP 또는 HTTPS 주소를 입력해 주세요.");
      return;
    }

    try {
      setIsUploading(true);
      setSummary("");
      setErrorMessage("");

      const response = await fetch(API_BASE_URL + "/link/summary", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ url: normalizedUrl.href }),
      });

      if (!response.ok) {
        throw new Error("링크 요약에 실패했습니다. (" + response.status + ")");
      }

      const summaryResult = await response.text();
      setSummary(summaryResult);

      addUploadedItem({
        id: crypto.randomUUID(),
        type: "link",
        name: normalizedUrl.hostname,
        detail: normalizedUrl.href,
        uploadedAt: new Date().toISOString(),
      });

      setLinkUrl("");
    } catch (error) {
      console.error("링크 업로드 오류:", error);
      setErrorMessage(
        error.message || "링크를 요약하는 중 오류가 발생했습니다.",
      );
    } finally {
      setIsUploading(false);
    }
  }

  function handleDelete(itemId) {
    const shouldDelete = window.confirm("이 항목을 삭제하시겠습니까?");

    if (!shouldDelete) {
      return;
    }

    setUploadedItems((currentItems) => {
      const nextItems = currentItems.filter((item) => item.id !== itemId);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(nextItems));
      return nextItems;
    });
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

        <section className="upload-tabs" aria-label="업로드 방식">
          <button
            type="button"
            className={
              "upload-tab " + (activeTab === "pdf" ? "upload-tab--active" : "")
            }
            onClick={() => changeTab("pdf")}
          >
            PDF 업로드
          </button>

          <button
            type="button"
            className={
              "upload-tab " + (activeTab === "link" ? "upload-tab--active" : "")
            }
            onClick={() => changeTab("link")}
          >
            링크 업로드
          </button>
        </section>

        {activeTab === "pdf" ? (
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
              className={
                "upload-dropzone " +
                (isDragging ? "upload-dropzone--dragging" : "")
              }
              role="button"
              tabIndex={0}
              onClick={() => fileInputRef.current?.click()}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  fileInputRef.current?.click();
                }
              }}
              onDragOver={(event) => {
                event.preventDefault();
                setIsDragging(true);
              }}
              onDragLeave={(event) => {
                event.preventDefault();
                setIsDragging(false);
              }}
              onDrop={handleDrop}
            >
              <span className="upload-panel__icon">
                <FileIcon />
              </span>
              <strong>PDF 파일을 드래그하거나 클릭하여 업로드하세요.</strong>
              <span>PDF, 최대 50MB</span>
            </div>

            {selectedFile && (
              <div className="selected-file">
                <span className="file-circle">
                  <FileIcon />
                </span>

                <div className="item-information">
                  <strong>{selectedFile.name}</strong>
                  <span>{formatFileSize(selectedFile.size)}</span>
                </div>

                <button
                  type="button"
                  className="primary-action"
                  onClick={handlePdfUpload}
                  disabled={isUploading}
                >
                  {isUploading ? "AI 요약 중..." : "AI 요약 시작"}
                </button>
              </div>
            )}
          </section>
        ) : (
          <form className="link-upload-panel" onSubmit={handleLinkUpload}>
            <span className="upload-panel__icon">
              <LinkIcon />
            </span>
            <strong>URL을 입력하거나 붙여넣어 주세요.</strong>
            <span>기사, 블로그, 논문 등 공개 URL 지원</span>

            <div className="link-upload-panel__form">
              <input
                type="url"
                value={linkUrl}
                placeholder="https://example.com/article"
                aria-label="요약할 링크"
                onChange={(event) => setLinkUrl(event.target.value)}
              />
              <button type="submit" disabled={isUploading}>
                {isUploading ? "요약 중..." : "링크 요약"}
              </button>
            </div>
          </form>
        )}

        {errorMessage && (
          <p className="upload-feedback upload-feedback--error" role="alert">
            {errorMessage}
          </p>
        )}

        {summary && (
          <section className="upload-summary">
            <h2>AI 핵심 요약</h2>
            <p>{summary}</p>
          </section>
        )}

        <section className="upload-history">
          <h2>업로드한 자료</h2>

          {uploadedItems.length === 0 ? (
            <div className="upload-history__empty">
              아직 업로드한 자료가 없습니다.
            </div>
          ) : (
            <div className="upload-history__list">
              {uploadedItems.map((item) => (
                <article className="uploaded-item" key={item.id}>
                  <span className="file-circle">
                    {item.type === "link" ? <LinkIcon /> : <FileIcon />}
                  </span>

                  <div className="item-information">
                    <strong>{item.name}</strong>
                    <span>
                      {formatDate(item.uploadedAt)}
                      <b>•</b>
                      {item.detail}
                    </span>
                  </div>

                  <span className="uploaded-item__status">완료</span>

                  <button
                    type="button"
                    className="uploaded-item__delete"
                    aria-label={item.name + " 삭제"}
                    onClick={() => handleDelete(item.id)}
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
            <span>
              <LightIcon />
            </span>
            <strong>Tip!</strong>
          </div>

          <ul>
            <li>
              텍스트가 많은 PDF일수록 더 좋은 요약 결과를 얻을 수 있습니다.
            </li>
            <li>스캔 파일은 요약 정확도가 낮을 수 있습니다.</li>
            <li>로그인이 필요한 비공개 링크는 분석할 수 없습니다.</li>
          </ul>
        </aside>
      </main>
    </div>
  );
}

export default UploadPage;
