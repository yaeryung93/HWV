import { getUserId } from "./session";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const USE_MOCK_API = import.meta.env.VITE_USE_MOCK_API !== "false";
const CURRENT_QUIZ_KEY = "hwv-current-java-quiz";
const QUIZ_RESULTS_KEY_PREFIX = "hwv-java-quiz-results";
const GENERATED_QUIZ_COUNT_KEY_PREFIX = "hwv-generated-java-quiz-count";

const mockGrammars = [
  {
    name: "Collection Framework",
    rating: 5,
    description: "동적으로 크기가 증가하는 리스트와 컬렉션을 사용하고 있습니다.",
  },
  {
    name: "Generic",
    rating: 4,
    description: "제네릭을 사용하여 타입 안정성을 확보했습니다.",
  },
  {
    name: "Exception",
    rating: 3,
    description: "예외 처리를 통해 실행 중 발생할 수 있는 오류에 대응하고 있습니다.",
  },
];

const mockQuestions = [
  {
    id: "java-quiz-1",
    question: "ArrayList가 속한 Java의 주요 프레임워크는 무엇인가요?",
    options: ["Collection Framework", "Thread API", "Stream I/O", "Reflection"],
    answer: "Collection Framework",
  },
  {
    id: "java-quiz-2",
    question: "제네릭을 사용하는 가장 중요한 이유는 무엇인가요?",
    options: ["타입 안정성 확보", "실행 속도 증가", "메모리 자동 해제", "클래스 삭제"],
    answer: "타입 안정성 확보",
  },
  {
    id: "java-quiz-3",
    question: "예외를 직접 처리할 때 주로 사용하는 구문은 무엇인가요?",
    options: ["try-catch", "if-import", "class-new", "for-switch"],
    answer: "try-catch",
  },
  {
    id: "java-quiz-4",
    question: "List 인터페이스의 특징으로 알맞은 것은 무엇인가요?",
    options: ["요소의 순서를 유지한다", "중복을 허용하지 않는다", "키와 값만 저장한다", "파일만 저장한다"],
    answer: "요소의 순서를 유지한다",
  },
  {
    id: "java-quiz-5",
    question: "컴파일 시점에 잘못된 타입 사용을 찾도록 돕는 기능은 무엇인가요?",
    options: ["Generic", "Package", "Annotation", "Module"],
    answer: "Generic",
  },
];

function wait(milliseconds) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

function normalizeRating(value) {
  if (typeof value === "number") {
    return Math.round(Math.min(5, Math.max(1, value)));
  }

  if (typeof value === "string") {
    const filledStars = (value.match(/★/g) || []).length;
    return Math.round(filledStars || Number(value) || 3);
  }

  return 3;
}

function normalizeAnalysis(result) {
  const sourceGrammars =
    result?.coreGrammars || result?.grammars || result?.syntaxes || [];
  const grammars = sourceGrammars.slice(0, 3).map((grammar, index) => ({
    name: grammar.name || grammar.title || `핵심 문법 ${index + 1}`,
    rating: normalizeRating(grammar.rating || grammar.score || grammar.level),
    description: grammar.description || grammar.explanation || "분석 설명이 없습니다.",
  }));

  return {
    summary:
      result?.summary ||
      grammars.map((grammar) => `${grammar.name}: ${grammar.description}`).join("\n"),
    grammars,
  };
}

function normalizeQuestions(result) {
  const questions = Array.isArray(result)
    ? result
    : result?.questions || result?.quizzes || [];

  return questions.slice(0, 5).map((question, index) => ({
    id: question.id || `java-quiz-${index + 1}`,
    question: question.question || question.title,
    options: question.options || question.choices || [],
    answer: question.answer || question.correctAnswer,
  }));
}

function saveCurrentQuiz(questions) {
  localStorage.setItem(CURRENT_QUIZ_KEY, JSON.stringify(questions));
}

export function getCurrentJavaQuiz() {
  try {
    return JSON.parse(localStorage.getItem(CURRENT_QUIZ_KEY)) || [];
  } catch {
    return [];
  }
}

export function recordJavaQuizResult({ correctCount, wrongCount }) {
  const userId = getUserId() || "guest";
  const key = `${QUIZ_RESULTS_KEY_PREFIX}-${userId}`;
  let results;

  try {
    results = JSON.parse(localStorage.getItem(key)) || [];
  } catch {
    results = [];
  }

  results.unshift({
    id: String(Date.now()),
    correctCount,
    wrongCount,
    answeredAt: new Date().toISOString(),
  });
  localStorage.setItem(key, JSON.stringify(results));
}

function recordGeneratedJavaQuiz(count) {
  const userId = getUserId() || "guest";
  const key = `${GENERATED_QUIZ_COUNT_KEY_PREFIX}-${userId}`;
  const currentCount = Number(localStorage.getItem(key)) || 0;

  localStorage.setItem(key, String(currentCount + count));
}

export async function submitJavaQuizAnswers(questions, selectedAnswers) {
  const userId = getUserId();

  if (!userId) {
    throw new Error("로그인 후 정답을 제출해 주세요.");
  }

  if (USE_MOCK_API) {
    await wait(350);
    const correctCount = questions.filter(
      (question) => selectedAnswers[question.id] === question.answer,
    ).length;
    const result = {
      correctCount,
      wrongCount: questions.length - correctCount,
    };

    recordJavaQuizResult(result);
    return result;
  }

  const response = await fetch(`${API_BASE_URL}/quiz/result`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId,
      answers: questions.map((question) => ({
        quizId: question.id,
        selectedAnswer: selectedAnswers[question.id],
      })),
    }),
  });

  if (!response.ok) {
    throw new Error(`퀴즈 결과 저장에 실패했습니다. (${response.status})`);
  }

  const result = await response.json();

  return {
    correctCount: result.correctCount ?? result.correctAnswers ?? 0,
    wrongCount: result.wrongCount ?? result.incorrectAnswers ?? 0,
  };
}

export function getLocalJavaQuizStatistics() {
  const userId = getUserId() || "guest";
  const key = `${QUIZ_RESULTS_KEY_PREFIX}-${userId}`;
  const generatedKey = `${GENERATED_QUIZ_COUNT_KEY_PREFIX}-${userId}`;
  const generatedProblems = Number(localStorage.getItem(generatedKey)) || 0;

  try {
    const results = JSON.parse(localStorage.getItem(key)) || [];

    return results.reduce(
      (statistics, result) => ({
        generatedProblems,
        correctAnswers: statistics.correctAnswers + result.correctCount,
        incorrectAnswers: statistics.incorrectAnswers + result.wrongCount,
      }),
      { generatedProblems, correctAnswers: 0, incorrectAnswers: 0 },
    );
  } catch {
    return { generatedProblems, correctAnswers: 0, incorrectAnswers: 0 };
  }
}

export async function analyzeJavaFile(file) {
  if (USE_MOCK_API) {
    await wait(700);
    return {
      summary: `${file.name}에서 Java 핵심 문법 3개를 분석했습니다.`,
      grammars: mockGrammars,
    };
  }

  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/java/analyze`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`Java 파일 분석에 실패했습니다. (${response.status})`);
  }

  const analysis = normalizeAnalysis(await response.json());

  if (analysis.grammars.length !== 3) {
    throw new Error("AI 분석 결과에 핵심 문법 3개가 필요합니다.");
  }

  return analysis;
}

export async function createJavaQuiz(analysis) {
  const userId = getUserId();

  if (!userId) {
    throw new Error("로그인 후 문제를 생성해 주세요.");
  }

  if (USE_MOCK_API) {
    await wait(700);
    saveCurrentQuiz(mockQuestions);
    recordGeneratedJavaQuiz(mockQuestions.length);
    return mockQuestions;
  }

  const response = await fetch(`${API_BASE_URL}/quiz`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      summary: analysis.summary,
      userId,
    }),
  });

  if (!response.ok) {
    throw new Error(`문제 생성에 실패했습니다. (${response.status})`);
  }

  const questions = normalizeQuestions(await response.json());

  if (questions.length !== 5) {
    throw new Error("AI가 생성한 문제 5개가 모두 필요합니다.");
  }

  saveCurrentQuiz(questions);
  return questions;
}
