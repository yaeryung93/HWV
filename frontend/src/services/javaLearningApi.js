import { getUserId } from "./session";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "https://sumquiz.onrender.com";

function normalizeQuestions(result) {
  const questions = Array.isArray(result) ? result : result?.questions || result?.quizzes || [];
  return questions.slice(0, 3).map((question) => ({
    id: question.id,
    question: question.question,
    grammarName: question.grammarName,
    options: question.options || [question.option1, question.option2, question.option3, question.option4, question.option5],
    answer: Number(question.answer),
    explanation: question.explanation,
  }));
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const result = contentType.includes("application/json") ? await response.json() : await response.text();
  if (!response.ok) throw new Error(result?.message || result?.error || result || `요청에 실패했습니다. (${response.status})`);
  return result;
}

export async function analyzeJavaFile(file) {
  const userId = getUserId();
  if (!userId) throw new Error("로그인 후 Java 파일을 분석해 주세요.");
  const formData = new FormData();
  formData.append("file", file);
  formData.append("userId", String(userId));
  const result = await parseResponse(await fetch(`${API_BASE_URL}/java/analyze`, { method: "POST", body: formData }));
  if (result?.grammars?.length !== 3) throw new Error("AI 분석 결과에 핵심 문법 3개가 필요합니다.");
  return result;
}

export async function createJavaQuiz(analysis) {
  const userId = getUserId();
  if (!userId) throw new Error("로그인 후 문제를 생성해 주세요.");
  const result = await parseResponse(await fetch(`${API_BASE_URL}/quiz`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, code: analysis.sourceCode }),
  }));
  const questions = normalizeQuestions(result);
  if (questions.length !== 3) throw new Error("AI가 생성한 문제 3개가 모두 필요합니다.");
  return questions;
}

export async function getCurrentJavaQuiz() {
  const userId = getUserId();
  if (!userId) return [];
  return normalizeQuestions(await parseResponse(await fetch(`${API_BASE_URL}/quiz?userId=${userId}`)));
}

export async function submitJavaQuizAnswers(questions, selectedAnswers) {
  const userId = getUserId();
  if (!userId) throw new Error("로그인 후 정답을 제출해 주세요.");
  return parseResponse(await fetch(`${API_BASE_URL}/quiz/result`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, answers: questions.map((question) => ({
      quizId: question.id, selectedAnswer: selectedAnswers[question.id],
    })) }),
  }));
}
