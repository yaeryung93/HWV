import { useMemo } from "react";

function CodeEditor({ value, onChange, language }) {
  const lineNumbers = useMemo(() => {
    const count = Math.max(value.split("\n").length, 16);
    return Array.from({ length: count }, (_, index) => index + 1);
  }, [value]);

  return (
    <section className="code-card">
      <div className="code-card__header">
        <h2>코드 작성</h2>

        <output
          className="code-card__language"
          aria-label="자동 감지된 프로그래밍 언어"
        >
          <span>자동 감지</span>
          <strong>{language}</strong>
        </output>
      </div>

      <div className="code-editor">
        <pre className="code-editor__lines" aria-hidden="true">
          {lineNumbers.join("\n")}
        </pre>
        <textarea
          value={value}
          spellCheck="false"
          aria-label="소스 코드"
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
    </section>
  );
}

export default CodeEditor;
