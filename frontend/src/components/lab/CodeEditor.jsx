import { useMemo, useRef } from "react";

function CodeEditor({ value, onChange, language }) {
  const textareaRef = useRef(null);
  const lineNumbers = useMemo(() => {
    const count = Math.max(value.split("\n").length, 16);
    return Array.from({ length: count }, (_, index) => index + 1);
  }, [value]);

  function restoreSelection(start, end) {
    window.requestAnimationFrame(() => {
      textareaRef.current?.focus();
      textareaRef.current?.setSelectionRange(start, end);
    });
  }

  function handleKeyDown(event) {
    if (event.key !== "Tab") {
      return;
    }

    event.preventDefault();

    const textarea = event.currentTarget;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const indentation = "    ";

    if (start === end && !event.shiftKey) {
      onChange(value.slice(0, start) + indentation + value.slice(end));
      restoreSelection(start + indentation.length, start + indentation.length);
      return;
    }

    const blockStart = value.lastIndexOf("\n", Math.max(0, start - 1)) + 1;
    const nextNewLine = value.indexOf("\n", end);
    const blockEnd = nextNewLine === -1 ? value.length : nextNewLine;
    const lines = value.slice(blockStart, blockEnd).split("\n");
    const firstLineOffset = start - blockStart;

    if (event.shiftKey) {
      const removedCounts = lines.map((line) => line.match(/^ {1,4}/)?.[0].length || 0);
      const transformed = lines
        .map((line, index) => line.slice(removedCounts[index]))
        .join("\n");
      const totalRemoved = removedCounts.reduce((sum, count) => sum + count, 0);
      const newStart = start - Math.min(removedCounts[0], firstLineOffset);
      const newEnd = Math.max(newStart, end - totalRemoved);

      onChange(value.slice(0, blockStart) + transformed + value.slice(blockEnd));
      restoreSelection(newStart, newEnd);
      return;
    }

    const transformed = lines.map((line) => indentation + line).join("\n");
    const addedLength = indentation.length * lines.length;

    onChange(value.slice(0, blockStart) + transformed + value.slice(blockEnd));
    restoreSelection(start + indentation.length, end + addedLength);
  }

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
          ref={textareaRef}
          value={value}
          spellCheck="false"
          aria-label="소스 코드"
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={handleKeyDown}
        />
      </div>
    </section>
  );
}

export default CodeEditor;
