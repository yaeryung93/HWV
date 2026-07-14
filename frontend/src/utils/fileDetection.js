export const LANGUAGE_BY_EXTENSION = {
  java: "Java",
};

export const ACCEPTED_FILE_TYPES = ".java";

export function getFileExtension(fileName) {
  return fileName.toLowerCase().split(".").pop();
}

export function filterSupportedFiles(fileList) {
  return Array.from(fileList).filter(
    (file) => getFileExtension(file.name) === "java",
  );
}

export function detectUploadedMaterial(files) {
  return {
    language: files.length ? "Java" : "감지되지 않음",
    pdfCount: 0,
    codeFileCount: files.length,
    materialType: "code",
  };
}
