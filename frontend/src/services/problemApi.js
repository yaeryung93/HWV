import { getLocalJavaQuizStatistics } from "./javaLearningApi";
import { getUserId } from "./session";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const USE_MOCK_API = import.meta.env.VITE_USE_MOCK_API !== "false";
const PROBLEM_STORAGE_KEY = "sumquiz-generated-problems-v2";
const ATTEMPT_STORAGE_KEY = "sumquiz-code-attempts-v2";

function wait(milliseconds = 350) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, milliseconds);
  });
}

function readStorage(key) {
  try {
    const saved = localStorage.getItem(key);
    return saved ? JSON.parse(saved) : [];
  } catch {
    return [];
  }
}

function getMockProblems() {
  return readStorage(PROBLEM_STORAGE_KEY);
}

function saveMockProblems(problems) {
  localStorage.setItem(PROBLEM_STORAGE_KEY, JSON.stringify(problems));
}

function getMockAttempts() {
  return readStorage(ATTEMPT_STORAGE_KEY);
}

function saveMockAttempt(attempt) {
  const nextAttempts = [attempt, ...getMockAttempts()];
  localStorage.setItem(ATTEMPT_STORAGE_KEY, JSON.stringify(nextAttempts));
}

async function request(endpoint, options = {}) {
  const response = await fetch(API_BASE_URL + endpoint, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  const contentType = response.headers.get("content-type") || "";
  const result = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    throw new Error(
      result?.message || result || "요청 처리 중 오류가 발생했습니다.",
    );
  }

  return result;
}

function getStarterCode(language) {
  if (language.includes("PDF")) {
    return "// PDF 내용을 분석한 문제에 맞게 코드를 작성하세요.\n";
  }

  if (language.includes("Python")) {
    return "def solution(data):\n    # AI 요약을 참고해 코드를 작성하세요.\n    pass\n";
  }

  if (language === "Java") {
    return "class Solution {\n    public Object solution(Object input) {\n        // AI 요약을 참고해 코드를 작성하세요.\n        return null;\n    }\n}\n";
  }

  if (language === "Kotlin") {
    return "fun solution(input: Any): Any? {\n    // AI 요약을 참고해 코드를 작성하세요.\n    return null\n}\n";
  }

  if (language === "C" || language === "C++") {
    return "// AI 요약을 참고해 solution 함수를 작성하세요.\n";
  }

  return "function solution(input) {\n  // AI 요약을 참고해 코드를 작성하세요.\n}\n";
}

function createMockProblem({ files, language, difficulty, index, materialType }) {
  const fileNames = files
    .slice(0, 3)
    .map((file) => file.name)
    .join(", ");
  const titles = [
    "코드 흐름 이해하기",
    "핵심 로직 완성하기",
    "예외 상황 처리하기",
    "입력과 출력 검증하기",
    "코드 품질 개선하기",
  ];
  const isPdfMaterial = materialType === "pdf";
  const category = isPdfMaterial ? "PDF 학습자료" : language;
  const requirements = isPdfMaterial
    ? [
        "AI가 요약한 PDF의 핵심 개념과 예제 코드를 확인하세요.",
        "문제에서 요구하는 프로그래밍 언어와 입출력 형식에 맞춰 구현하세요.",
      ]
    : [
        "AI가 요약한 코드의 목적과 실행 흐름을 확인하세요.",
        `감지된 ${language} 문법으로 요구 기능을 구현하세요.`,
      ];

  if (difficulty !== "쉬움") {
    requirements.push("비어 있는 값과 잘못된 입력도 안전하게 처리하세요.");
  }

  if (difficulty === "어려움") {
    requirements.push("중복 계산을 줄이고 코드의 시간·공간 효율도 고려하세요.");
  }

  return {
    id: `ai-problem-${Date.now()}-${index + 1}`,
    title: `${category} ${titles[index]}`,
    category,
    difficulty,
    progress: 0,
    solved: false,
    description: isPdfMaterial
      ? `${fileNames} PDF의 내용을 바탕으로 만든 ${difficulty} 난이도 문제입니다. 문서의 핵심 개념과 코드 예제를 이해하고 요구사항을 완성하세요.`
      : `${fileNames} 코드를 AI가 분석한 내용을 바탕으로 만든 ${difficulty} 난이도 문제입니다. 업로드한 코드의 핵심 동작을 이해하고 요구사항을 완성하세요.`,
    summary: isPdfMaterial
      ? `업로드한 PDF ${files.length}개의 텍스트와 코드 예제를 분석할 자료로 준비했습니다.`
      : `업로드한 ${files.length}개 ${language} 소스 파일의 핵심 구조와 실행 흐름을 분석했습니다.`,
    requirements,
    inputExample: "AI가 분석한 코드의 입력 형식을 따릅니다.",
    outputExample: "요구사항을 만족하는 실행 결과를 반환합니다.",
    starterCode: getStarterCode(language),
    language,
    sourceFiles: files.map((file) => file.name),
    tests: [
      { id: 1, name: "기본 동작", status: "pending" },
      { id: 2, name: "입력 처리", status: "pending" },
      { id: 3, name: "결과 검증", status: "pending" },
      ...(difficulty === "쉬움"
        ? []
        : [{ id: 4, name: "예외 상황", status: "pending" }]),
      ...(difficulty === "어려움"
        ? [{ id: 5, name: "효율성", status: "pending" }]
        : []),
    ],
  };
}

export async function getProblems() {
  if (!USE_MOCK_API) {
    return request("/api/problems");
  }

  await wait();
  return getMockProblems();
}

export async function getProblem(problemId) {
  if (!USE_MOCK_API) {
    return request("/api/problems/" + problemId);
  }

  await wait(220);
  return getMockProblems().find((problem) => problem.id === problemId) || null;
}

export async function createProblemsFromProject({
  files,
  language,
  difficulty,
  count,
  materialType,
}) {
  if (!USE_MOCK_API) {
    const formData = new FormData();

    files.forEach((file) => {
      formData.append("files", file, file.webkitRelativePath || file.name);
    });

    formData.append("language", language);
    formData.append("difficulty", difficulty);
    formData.append("count", String(count));
    formData.append("materialType", materialType || "code");

    const response = await fetch(API_BASE_URL + "/api/projects/problems", {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      throw new Error("AI 문제 생성에 실패했습니다.");
    }

    return response.json();
  }

  await wait(900);

  const createdProblems = Array.from({ length: Number(count) }, (_, index) =>
    createMockProblem({
      files,
      language,
      difficulty,
      index,
      materialType,
    }),
  );
  const nextProblems = [...createdProblems, ...getMockProblems()];
  saveMockProblems(nextProblems);

  return createdProblems;
}

function createMockTestResults(problem, sourceCode) {
  const normalizedCode = sourceCode.replace(/\s/g, "");
  const hasImplementation =
    normalizedCode.length > 45 &&
    !normalizedCode.includes("pass") &&
    !normalizedCode.includes("returnnull") &&
    !normalizedCode.includes("TODO");

  return problem.tests.map((test, index) => {
    const passed = hasImplementation && index < Math.max(1, problem.tests.length - 1);

    return {
      ...test,
      status: passed ? "passed" : "failed",
      runtime: passed ? `${12 + index * 3}ms` : null,
      memory: passed ? `${(3.1 + index * 0.14).toFixed(2)}MB` : null,
      input: passed ? null : "업로드한 코드 기반 테스트 입력",
      expected: passed ? null : "요구사항을 만족하는 결과",
      actual: passed ? null : "현재 코드의 실행 결과",
    };
  });
}

export async function submitSolution({ problemId, language, sourceCode }) {
  if (!USE_MOCK_API) {
    return request("/api/submissions", {
      method: "POST",
      body: JSON.stringify({ problemId, language, sourceCode }),
    });
  }

  await wait(750);

  const problem = await getProblem(problemId);

  if (!problem) {
    throw new Error("문제를 찾을 수 없습니다.");
  }

  const tests = createMockTestResults(problem, sourceCode);
  const passedCount = tests.filter((test) => test.status === "passed").length;
  const allPassed = passedCount === tests.length;
  const attempt = {
    id: String(Date.now()),
    problemId,
    problemTitle: problem.title,
    category: problem.category,
    language,
    passedCount,
    totalCount: tests.length,
    passed: allPassed,
    submittedAt: new Date().toISOString(),
  };

  saveMockAttempt(attempt);

  return {
    attempt,
    tests,
    status: allPassed ? "passed" : "failed",
    hint: allPassed
      ? null
      : "AI 요약과 문제 요구사항을 다시 확인하고, 실패한 입력에서 값이 어떻게 변하는지 단계별로 살펴보세요.",
    improvement: allPassed
      ? "전체 테스트를 통과했습니다. 함수 이름과 변수 이름을 더 명확하게 다듬으면 읽기 좋은 코드가 됩니다."
      : "기본 실행 흐름뿐 아니라 비어 있는 값, 경계값 같은 예외 상황도 함께 처리해 보세요.",
  };
}

export async function getDashboardSummary() {
  if (!USE_MOCK_API) {
    return request(`/api/me/dashboard?userId=${getUserId()}`);
  }

  await wait(180);
  const problems = getMockProblems();
  const attempts = getMockAttempts();
  const solvedProblemIds = new Set(
    attempts.filter((attempt) => attempt.passed).map((attempt) => attempt.problemId),
  );
  const totalTests = attempts.reduce(
    (sum, attempt) => sum + attempt.totalCount,
    0,
  );
  const passedTests = attempts.reduce(
    (sum, attempt) => sum + attempt.passedCount,
    0,
  );
  const javaQuizStatistics = getLocalJavaQuizStatistics();
  const javaAnswerTotal =
    javaQuizStatistics.correctAnswers + javaQuizStatistics.incorrectAnswers;

  return {
    generatedProblems:
      problems.length + javaQuizStatistics.generatedProblems,
    attempts: attempts.length,
    solvedProblems: solvedProblemIds.size,
    correctAnswers: javaQuizStatistics.correctAnswers,
    incorrectAnswers: javaQuizStatistics.incorrectAnswers,
    accuracy: javaAnswerTotal
      ? Math.round((javaQuizStatistics.correctAnswers / javaAnswerTotal) * 100)
      : totalTests
        ? Math.round((passedTests / totalTests) * 100)
        : 0,
    recentAttempts: attempts.slice(0, 4),
  };
}

export async function getWrongNotes() {
  if (!USE_MOCK_API) {
    return request(`/api/me/wrong-notes?userId=${getUserId()}`);
  }

  await wait(180);
  return getMockAttempts().filter((attempt) => !attempt.passed);
}

export async function getLearningStatistics() {
  if (!USE_MOCK_API) {
    return request(`/api/me/statistics?userId=${getUserId()}`);
  }

  const summary = await getDashboardSummary();
  const attempts = getMockAttempts();
  const categoryMap = attempts.reduce((map, attempt) => {
    const category = attempt.category || "기타";
    const current = map[category] || { passed: 0, total: 0 };
    current.passed += attempt.passedCount;
    current.total += attempt.totalCount;
    map[category] = current;
    return map;
  }, {});
  const today = new Date();
  const weeklyAttempts = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today);
    date.setDate(today.getDate() - (6 - index));
    return attempts.filter((attempt) => {
      const attemptDate = new Date(attempt.submittedAt);
      return attemptDate.toDateString() === date.toDateString();
    }).length;
  });

  return {
    ...summary,
    categoryAccuracy: Object.entries(categoryMap).map(([name, value]) => ({
      name,
      value: value.total ? Math.round((value.passed / value.total) * 100) : 0,
    })),
    weeklyAttempts,
  };
}
