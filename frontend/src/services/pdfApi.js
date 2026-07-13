const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export async function summarizePdf(selectedFile) {
  const formData = new FormData();

  formData.append("file", selectedFile);

  const response = await fetch(`${API_BASE_URL}/pdf/summary`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`PDF 요약에 실패했습니다. (${response.status})`);
  }

  return response.text();
}
