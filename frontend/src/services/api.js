const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "https://sumquiz.onrender.com"
).replace(/\/$/, "");
const REQUEST_TIMEOUT_MS = 15000;

export async function requestApi(endpoint, options = {}) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(
    () => controller.abort(),
    REQUEST_TIMEOUT_MS,
  );
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
    });
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error(
        "서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.",
      );
    }
    throw new Error("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
  } finally {
    window.clearTimeout(timeoutId);
  }

  const contentType = response.headers.get("content-type") || "";

  let result;

  if (contentType.includes("application/json")) {
    result = await response.json();
  } else {
    result = await response.text();
  }

  if (!response.ok) {
    const message =
      result?.message ||
      result?.error ||
      result ||
      `요청에 실패했습니다.  (${response.status})`;

    throw new Error(message);
  }

  return result;
}
