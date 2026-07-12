const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export async function requestApi(endpoint, options = {}) {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  if (!response.ok) {
    throw new Error("로그인 실패");
  }

  return response.json();
}