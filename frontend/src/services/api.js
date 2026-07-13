const API_BASE_URL = "https://sumquiz.onrender.com";

export async function requestApi(endpoint, options = {}) {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

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
      `요청에 실패했습니다. (${response.status})`;

    throw new Error(message);
  }

  return result;
}
