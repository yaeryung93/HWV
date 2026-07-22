package com.example.backend.service;

import com.example.backend.dto.CodingProblemDraft;
import com.example.backend.dto.CodingReviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaCodeExecutionService {
    private static final long COMPILE_TIMEOUT_SECONDS = 10;
    private static final long RUN_TIMEOUT_SECONDS = 3;
    private static final int MAX_SOURCE_LENGTH = 50_000;
    private static final int MAX_OUTPUT_BYTES = 32_768;
    private static final Pattern MAIN_CLASS = Pattern.compile("\\bpublic\\s+class\\s+Main\\b");
    private static final Pattern SOLUTION_CLASS = Pattern.compile("\\bpublic\\s+class\\s+Solution\\b");
    private static final Pattern BLOCKED_API = Pattern.compile(
        "\\b(package|ProcessBuilder|Runtime\\s*\\.|System\\s*\\.\\s*(exit|getenv|getProperties|getProperty|load|loadLibrary)|java\\s*\\.\\s*(io|nio|net|lang\\s*\\.\\s*reflect)|ClassLoader)\\b"
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodingReviewResponse execute(CodingProblemDraft problem, String sourceCode) {
        if (problem.methodName() != null && !problem.methodName().isBlank()) {
            return executeSolution(problem, sourceCode);
        }
        return executeMain(problem, sourceCode);
    }

    private CodingReviewResponse executeMain(CodingProblemDraft problem, String sourceCode) {
        validateSource(sourceCode, MAIN_CLASS, "기본 실행 코드가 손상되었습니다. 코드 초기화 버튼을 누른 뒤 solution 메서드 내부만 수정해 주세요.");
        Path workDirectory = null;
        try {
            workDirectory = Files.createTempDirectory("hwv-java-");
            Files.writeString(workDirectory.resolve("Main.java"), sourceCode, StandardCharsets.UTF_8);

            ProcessResult compilation = runProcess(
                List.of(javaCommand("javac"), "-encoding", "UTF-8", "Main.java"),
                workDirectory, "", COMPILE_TIMEOUT_SECONDS
            );
            if (compilation.timedOut()) {
                return failedAll(problem, "컴파일 시간이 초과되었습니다.", compilation.output());
            }
            if (compilation.exitCode() != 0) {
                return failedAll(problem, "컴파일 오류를 수정해 주세요.", compilation.output());
            }

            List<CodingReviewResponse.TestResult> results = new ArrayList<>();
            for (CodingProblemDraft.TestCase test : problem.tests()) {
                ProcessResult execution = runProcess(
                    List.of(javaCommand("java"), "-Xmx64m", "-XX:MaxMetaspaceSize=64m", "-cp", ".", "Main"),
                    workDirectory, test.input(), RUN_TIMEOUT_SECONDS
                );
                String actual = execution.output().trim();
                String expected = test.expected().trim();
                boolean passed = !execution.timedOut()
                    && execution.exitCode() == 0
                    && normalize(actual).equals(normalize(expected));
                String reason = passed
                    ? "실제 실행 결과가 기대 출력과 일치합니다."
                    : execution.timedOut()
                        ? "실행 제한 시간 3초를 초과했습니다."
                        : execution.exitCode() != 0
                            ? friendlyExecutionError(execution.output())
                            : "실제 출력이 기대 출력과 다릅니다.";
                results.add(new CodingReviewResponse.TestResult(
                    test.id(), test.name(), passed ? "passed" : "failed",
                    test.input(), test.expected(), actual, reason
                ));
            }

            boolean allPassed = results.stream().allMatch(test -> "passed".equals(test.status()));
            return new CodingReviewResponse(
                allPassed ? "passed" : "failed",
                allPassed ? "모든 테스트를 통과했습니다." : "입력 처리와 출력 형식을 다시 확인해 주세요.",
                allPassed ? "실제 Java 실행 결과가 모든 기대 출력과 일치합니다."
                    : firstFailureReason(results, "실패한 테스트의 실제 출력과 기대 출력을 비교해 수정해 주세요."),
                results
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Java 실행 환경을 준비하지 못했습니다.", exception);
        } finally {
            deleteDirectory(workDirectory);
        }
    }

    private CodingReviewResponse executeSolution(CodingProblemDraft problem, String sourceCode) {
        validateSource(sourceCode, SOLUTION_CLASS, "기본 Solution 클래스가 손상되었습니다. 코드 초기화 버튼을 누른 뒤 solution 메서드 내부만 수정해 주세요.");
        Path workDirectory = null;
        try {
            workDirectory = Files.createTempDirectory("hwv-solution-");
            Files.writeString(workDirectory.resolve("Solution.java"), sourceCode, StandardCharsets.UTF_8);
            Files.writeString(workDirectory.resolve("Main.java"), buildHarness(problem), StandardCharsets.UTF_8);
            ProcessResult compilation = runProcess(
                List.of(javaCommand("javac"), "-encoding", "UTF-8", "Solution.java", "Main.java"),
                workDirectory, "", COMPILE_TIMEOUT_SECONDS
            );
            if (compilation.timedOut()) return failedAll(problem, "컴파일 시간이 초과되었습니다.", compilation.output());
            if (compilation.exitCode() != 0) return failedAll(problem, "solution 메서드의 컴파일 오류를 수정해 주세요.", compilation.output());

            List<CodingReviewResponse.TestResult> results = new ArrayList<>();
            for (int index = 0; index < problem.tests().size(); index++) {
                CodingProblemDraft.TestCase test = problem.tests().get(index);
                ProcessResult execution = runProcess(
                    List.of(javaCommand("java"), "-Xmx64m", "-XX:MaxMetaspaceSize=64m", "-cp", ".", "Main", String.valueOf(index)),
                    workDirectory, "", RUN_TIMEOUT_SECONDS
                );
                String actual = execution.output().trim();
                boolean passed = !execution.timedOut() && execution.exitCode() == 0
                    && normalize(actual).equals(normalize(test.expected().trim()));
                String reason = passed ? "solution 반환값이 기대값과 일치합니다."
                    : execution.timedOut() ? "실행 제한 시간 3초를 초과했습니다."
                    : execution.exitCode() != 0 ? friendlyExecutionError(execution.output())
                    : "solution 반환값이 기대값과 다릅니다.";
                results.add(new CodingReviewResponse.TestResult(test.id(), test.name(), passed ? "passed" : "failed",
                    test.input(), test.expected(), actual, reason));
            }
            boolean allPassed = results.stream().allMatch(test -> "passed".equals(test.status()));
            return new CodingReviewResponse(allPassed ? "passed" : "failed",
                allPassed ? "모든 테스트를 통과했습니다." : "실패한 테스트의 반환값을 확인해 주세요.",
                allPassed ? "solution 반환값이 모든 기대값과 일치합니다."
                    : firstFailureReason(results, "실제 반환값과 기대값을 비교해 solution 메서드를 수정해 주세요."), results);
        } catch (IOException exception) {
            throw new IllegalStateException("Java 실행 환경을 준비하지 못했습니다.", exception);
        } finally {
            deleteDirectory(workDirectory);
        }
    }

    private void validateSource(String sourceCode, Pattern requiredClass, String classError) {
        if (sourceCode == null || sourceCode.isBlank()) throw new IllegalArgumentException("실행할 Java 코드를 입력해 주세요.");
        if (sourceCode.length() > MAX_SOURCE_LENGTH) throw new IllegalArgumentException("Java 코드는 50,000자 이하로 입력해 주세요.");
        if (!requiredClass.matcher(sourceCode).find()) throw new IllegalArgumentException(classError);
        if (BLOCKED_API.matcher(sourceCode).find()) throw new IllegalArgumentException("파일, 네트워크 또는 외부 프로세스 접근 코드는 실행할 수 없습니다.");
    }

    private String buildHarness(CodingProblemDraft problem) {
        if (problem.parameterTypes() == null || problem.parameterTypes().isEmpty()) {
            throw new IllegalArgumentException("문제의 solution 매개변수 정보가 없습니다.");
        }
        StringBuilder cases = new StringBuilder();
        for (int i = 0; i < problem.tests().size(); i++) {
            CodingProblemDraft.TestCase test = problem.tests().get(i);
            if (test.arguments() == null || test.arguments().size() != problem.parameterTypes().size()) {
                throw new IllegalArgumentException("테스트 인자 정보가 올바르지 않습니다.");
            }
            List<String> arguments = new ArrayList<>();
            for (int j = 0; j < problem.parameterTypes().size(); j++) {
                arguments.add(javaLiteral(problem.parameterTypes().get(j), test.arguments().get(j)));
            }
            cases.append("case \"").append(i).append("\" -> solution.")
                .append(problem.methodName()).append("(").append(String.join(", ", arguments)).append(");\n");
        }
        return """
            import java.lang.reflect.Array;
            public class Main {
                private static String format(Object value) {
                    if (value == null) return "null";
                    if (!value.getClass().isArray()) return String.valueOf(value);
                    StringBuilder result = new StringBuilder("[");
                    for (int i = 0; i < Array.getLength(value); i++) {
                        if (i > 0) result.append(", ");
                        result.append(format(Array.get(value, i)));
                    }
                    return result.append(']').toString();
                }
                public static void main(String[] args) {
                    Solution solution = new Solution();
                    Object result = switch (args[0]) {
                        %s
                        default -> throw new IllegalArgumentException("unknown test");
                    };
                    System.out.print(format(result));
                }
            }
            """.formatted(cases);
    }

    private String javaLiteral(String type, String rawValue) {
        try {
            if (type.endsWith("[]")) {
                String elementType = type.substring(0, type.length() - 2);
                JsonNode values = objectMapper.readTree(rawValue);
                if (!values.isArray()) throw new IllegalArgumentException("배열 인자는 JSON 배열 형식이어야 합니다.");
                List<String> elements = new ArrayList<>();
                values.forEach(value -> elements.add(javaLiteral(elementType,
                    value.isContainerNode() ? value.toString() : value.asText())));
                return "new " + elementType + "[]{" + String.join(", ", elements) + "}";
            }
            return switch (type) {
                case "int" -> String.valueOf(Integer.parseInt(rawValue.trim()));
                case "long" -> Long.parseLong(rawValue.trim()) + "L";
                case "double" -> Double.toString(Double.parseDouble(rawValue.trim()));
                case "boolean" -> {
                    if (!"true".equals(rawValue) && !"false".equals(rawValue)) throw new IllegalArgumentException("boolean 인자가 올바르지 않습니다.");
                    yield rawValue;
                }
                case "String" -> objectMapper.writeValueAsString(rawValue);
                default -> throw new IllegalArgumentException("지원하지 않는 매개변수 타입입니다: " + type);
            };
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("테스트 인자를 Java 값으로 변환하지 못했습니다.", exception);
        }
    }

    private ProcessResult runProcess(List<String> command, Path directory, String input, long timeoutSeconds) throws IOException {
        Process process = new ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .start();
        process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().close();

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException("Java 실행이 중단되었습니다.", exception);
        }
        if (!finished) process.destroyForcibly();
        try {
            process.waitFor(1, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        byte[] output = process.getInputStream().readNBytes(MAX_OUTPUT_BYTES + 1);
        String text = new String(output, 0, Math.min(output.length, MAX_OUTPUT_BYTES), StandardCharsets.UTF_8);
        if (output.length > MAX_OUTPUT_BYTES) text += "\n[출력이 너무 길어 중단되었습니다.]";
        return new ProcessResult(finished ? process.exitValue() : -1, text, !finished);
    }

    private CodingReviewResponse failedAll(CodingProblemDraft problem, String message, String actual) {
        String improvement = friendlyCompileError(actual, message);
        List<CodingReviewResponse.TestResult> tests = problem.tests().stream()
            .map(test -> new CodingReviewResponse.TestResult(test.id(), test.name(), "failed", test.input(), test.expected(), actual, improvement))
            .toList();
        return new CodingReviewResponse("failed", message, improvement, tests);
    }

    private String friendlyCompileError(String output, String fallback) {
        String text = output == null ? "" : output;
        String lower = text.toLowerCase();
        String location = errorLine(text);
        if (lower.contains("incompatible types")) return location + "사용한 값의 타입이 선언된 타입과 맞지 않습니다. 변수 타입, 매개변수 타입 또는 반환형을 확인해 주세요.";
        if (lower.contains("cannot find symbol")) return location + "변수, 메서드 또는 클래스 이름을 찾을 수 없습니다. 이름의 오타와 선언 여부를 확인해 주세요.";
        if (lower.contains("missing return statement")) return location + "모든 실행 경로에서 반환값을 돌려주지 않습니다. 필요한 return 문을 추가해 주세요.";
        if (lower.contains("';' expected")) return location + "문장 끝에 세미콜론(;)이 필요합니다.";
        if (lower.contains("')' expected") || lower.contains("']' expected") || lower.contains("'}' expected")) return location + "괄호가 올바르게 닫히지 않았습니다. 여는 괄호와 닫는 괄호의 짝을 확인해 주세요.";
        if (lower.contains("unclosed string literal")) return location + "문자열의 큰따옴표가 닫히지 않았습니다.";
        if (lower.contains("method") && lower.contains("cannot be applied")) return location + "메서드에 전달한 인자의 개수 또는 타입이 선언과 다릅니다.";
        if (lower.contains("illegal start") || lower.contains("not a statement")) return location + "Java 문법에 맞지 않는 코드가 있습니다. 해당 줄의 연산자, 괄호와 문장 구조를 확인해 주세요.";
        if (lower.contains("reached end of file")) return location + "코드 마지막의 닫는 중괄호(})가 부족합니다.";
        return location + (fallback == null || fallback.isBlank() ? "컴파일 오류가 발생했습니다. 표시된 줄의 문법과 타입을 확인해 주세요." : fallback);
    }

    private String friendlyExecutionError(String output) {
        String lower = output == null ? "" : output.toLowerCase();
        if (lower.contains("numberformatexception")) return "문자열을 숫자로 변환할 수 없습니다. 입력값과 숫자 변환 코드를 확인해 주세요.";
        if (lower.contains("arrayindexoutofboundsexception") || lower.contains("indexoutofboundsexception")) return "배열이나 목록의 범위를 벗어난 위치에 접근했습니다. 반복문의 시작값과 종료 조건을 확인해 주세요.";
        if (lower.contains("nullpointerexception")) return "값이 없는(null) 객체를 사용했습니다. 객체가 생성되거나 값이 할당됐는지 확인해 주세요.";
        if (lower.contains("arithmeticexception") && lower.contains("/ by zero")) return "0으로 나누는 연산이 발생했습니다. 나누기 전에 분모가 0인지 확인해 주세요.";
        if (lower.contains("stackoverflowerror")) return "재귀 호출이 끝나지 않아 호출 한도를 넘었습니다. 종료 조건을 확인해 주세요.";
        if (lower.contains("outofmemoryerror")) return "실행 중 사용할 수 있는 메모리를 초과했습니다. 너무 큰 배열이나 반복적인 객체 생성을 확인해 주세요.";
        if (lower.contains("classcastexception")) return "서로 호환되지 않는 타입으로 변환했습니다. 형 변환 대상의 실제 타입을 확인해 주세요.";
        return "실행 중 오류가 발생했습니다. 배열 범위, null 값, 숫자 연산과 반복 조건을 확인해 주세요.";
    }

    private String errorLine(String output) {
        Matcher matcher = Pattern.compile("(?:Main|Solution)\\.java:(\\d+):").matcher(output == null ? "" : output);
        return matcher.find() ? matcher.group(1) + "번째 줄: " : "";
    }

    private String firstFailureReason(List<CodingReviewResponse.TestResult> results, String fallback) {
        return results.stream().filter(test -> "failed".equals(test.status())).map(CodingReviewResponse.TestResult::reason)
            .filter(reason -> reason != null && !reason.isBlank()).findFirst().orElse(fallback);
    }

    private String javaCommand(String name) {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? name + ".exe" : name;
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private String normalize(String value) {
        return value.replace("\r\n", "\n").stripTrailing();
    }

    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private record ProcessResult(int exitCode, String output, boolean timedOut) { }
}
