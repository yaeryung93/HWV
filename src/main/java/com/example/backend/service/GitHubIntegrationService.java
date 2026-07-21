package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubIntegrationService {
    private static final Pattern REPOSITORY_URL = Pattern.compile("^https://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?/?$");
    private final GitHubConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final WebClient github = WebClient.builder().baseUrl("https://api.github.com").build();
    private final String appId;
    private final String appSlug;
    private final String privateKeyPem;
    private final String frontendUrl;

    public GitHubIntegrationService(GitHubConnectionRepository connectionRepository, UserRepository userRepository,
                                    CodingSubmissionRepository submissionRepository,
                                    @Value("${github.app-id:}") String appId,
                                    @Value("${github.app-slug:}") String appSlug,
                                    @Value("${github.private-key:}") String privateKeyPem,
                                    @Value("${github.frontend-url:https://hwv.vercel.app}") String frontendUrl) {
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.appId = appId;
        this.appSlug = appSlug;
        this.privateKeyPem = privateKeyPem;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
    }

    @Transactional
    public Map<String, Object> startConnection(Long userId, String repositoryUrl) {
        requireConfigured();
        Matcher matcher = REPOSITORY_URL.matcher(repositoryUrl == null ? "" : repositoryUrl.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("https://github.com/사용자명/저장소명 형식으로 입력해 주세요.");
        User user = user(userId);
        GitHubConnection connection = connectionRepository.findByUser(user).orElseGet(GitHubConnection::new);
        connection.setUser(user);
        connection.setRepositoryOwner(matcher.group(1));
        connection.setRepositoryName(matcher.group(2));
        connection.setInstallationId(null);
        connection.setConnectedAt(null);
        connection.setState(randomState());
        connection.setStateExpiresAt(LocalDateTime.now().plusMinutes(15));
        connectionRepository.save(connection);
        String installUrl = "https://github.com/apps/" + appSlug + "/installations/new?state="
            + URLEncoder.encode(connection.getState(), StandardCharsets.UTF_8);
        return Map.of("installUrl", installUrl);
    }

    @Transactional
    public String completeConnection(Long installationId, String state) {
        requireConfigured();
        GitHubConnection connection = connectionRepository.findByState(state == null ? "" : state)
            .orElseThrow(() -> new IllegalArgumentException("GitHub 연결 요청을 찾을 수 없습니다."));
        if (connection.getStateExpiresAt() == null || connection.getStateExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("GitHub 연결 시간이 만료되었습니다. 다시 연결해 주세요.");
        }
        Map<?, ?> repository = repository(installationToken(installationId), connection.getRepositoryOwner(), connection.getRepositoryName());
        connection.setInstallationId(installationId);
        connection.setPrivateRepository(Boolean.TRUE.equals(repository.get("private")));
        connection.setConnectedAt(LocalDateTime.now());
        String publishToken = randomState();
        connection.setPublishTokenHash(hash(publishToken));
        connection.setState(null);
        connection.setStateExpiresAt(null);
        connectionRepository.save(connection);
        return frontendUrl + "/profile?github=connected&github_token="
            + URLEncoder.encode(publishToken, StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status(Long userId) {
        return connectionRepository.findByUser(user(userId))
            .filter(connection -> connection.getInstallationId() != null)
            .map(connection -> Map.<String, Object>of(
                "connected", true,
                "owner", connection.getRepositoryOwner(),
                "repository", connection.getRepositoryName(),
                "privateRepository", connection.isPrivateRepository(),
                "url", "https://github.com/" + connection.getRepositoryOwner() + "/" + connection.getRepositoryName()
            )).orElseGet(() -> Map.of("connected", false));
    }

    @Transactional
    public void disconnect(Long userId) {
        connectionRepository.findByUser(user(userId)).ifPresent(connectionRepository::delete);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> publish(Long userId, Long submissionId, String publishToken) {
        User user = user(userId);
        CodingSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("제출 기록을 찾을 수 없습니다."));
        if (!submission.getUser().getId().equals(user.getId())) throw new IllegalArgumentException("다른 사용자의 제출 기록입니다.");
        if (!submission.isPassed()) throw new IllegalArgumentException("모든 테스트를 통과한 코드만 GitHub에 저장할 수 있습니다.");
        GitHubConnection connection = connectionRepository.findByUser(user)
            .filter(item -> item.getInstallationId() != null)
            .orElseThrow(() -> new IllegalArgumentException("마이페이지에서 GitHub 저장소를 먼저 연결해 주세요."));
        if (publishToken == null || connection.getPublishTokenHash() == null
            || !MessageDigest.isEqual(connection.getPublishTokenHash().getBytes(StandardCharsets.UTF_8), hash(publishToken).getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("GitHub 연결 인증이 만료되었습니다. 저장소를 다시 연결해 주세요.");
        }

        CodingProblem problem = submission.getProblem();
        String path = "HWV-Solutions/problem-" + problem.getId() + "-submission-" + submission.getId() + ".md";
        String content = markdown(problem, submission);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "docs: add HWV solution for problem " + problem.getId());
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        Map<?, ?> response = github.put()
            .uri("/repos/{owner}/{repo}/contents/HWV-Solutions/problem-{problemId}-submission-{submissionId}.md",
                connection.getRepositoryOwner(), connection.getRepositoryName(), problem.getId(), submission.getId())
            .headers(headers -> apiHeaders(headers, installationToken(connection.getInstallationId())))
            .contentType(MediaType.APPLICATION_JSON).bodyValue(body).retrieve().bodyToMono(Map.class).block();
        if (response == null || !(response.get("commit") instanceof Map<?, ?> commit)) {
            throw new IllegalStateException("GitHub 커밋 결과를 확인하지 못했습니다.");
        }
        return Map.of("message", "GitHub 저장이 완료되었습니다.", "commitUrl", String.valueOf(commit.get("html_url")), "path", path);
    }

    private Map<?, ?> repository(String token, String owner, String repo) {
        Map<?, ?> response = github.get().uri("/repos/{owner}/{repo}", owner, repo)
            .headers(headers -> apiHeaders(headers, token)).retrieve().bodyToMono(Map.class).block();
        if (response == null) throw new IllegalArgumentException("GitHub 저장소에 접근할 수 없습니다.");
        return response;
    }

    private String installationToken(Long installationId) {
        Map<?, ?> response = github.post().uri("/app/installations/{id}/access_tokens", installationId)
            .headers(headers -> apiHeaders(headers, appJwt())).contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of()).retrieve().bodyToMono(Map.class).block();
        if (response == null || response.get("token") == null) throw new IllegalStateException("GitHub 저장소 접근 토큰을 만들지 못했습니다.");
        return String.valueOf(response.get("token"));
    }

    private void apiHeaders(org.springframework.http.HttpHeaders headers, String token) {
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
    }

    private String appJwt() {
        try {
            long now = Instant.now().getEpochSecond();
            String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String payload = base64Url(("{\"iat\":" + (now - 60) + ",\"exp\":" + (now + 540) + ",\"iss\":\"" + appId + "\"}").getBytes(StandardCharsets.UTF_8));
            String signingInput = header + "." + payload;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey());
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return signingInput + "." + base64Url(signature.sign());
        } catch (Exception exception) {
            throw new IllegalStateException("GitHub App 인증 정보를 만들지 못했습니다.", exception);
        }
    }

    private PrivateKey privateKey() throws Exception {
        String normalized = privateKeyPem.replace("\\n", "\n").trim();
        if (normalized.length() >= 2 && ((normalized.startsWith("\"") && normalized.endsWith("\""))
            || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        boolean pkcs1 = normalized.contains("BEGIN RSA PRIVATE KEY");
        String base64 = normalized.replaceAll("-----BEGIN (?:RSA )?PRIVATE KEY-----", "")
            .replaceAll("-----END (?:RSA )?PRIVATE KEY-----", "").replaceAll("\\s", "");
        if (base64.isBlank()) throw new IllegalArgumentException("GitHub App private key PEM 내용이 비어 있습니다.");
        byte[] key = Base64.getDecoder().decode(base64);
        if (pkcs1) key = wrapPkcs1(key);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
    }

    private byte[] wrapPkcs1(byte[] pkcs1) throws Exception {
        byte[] algorithm = Base64.getDecoder().decode("MA0GCSqGSIb3DQEBAQUA");
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.write(0x02); content.write(0x01); content.write(0x00);
        content.write(algorithm);
        content.write(0x04); writeLength(content, pkcs1.length); content.write(pkcs1);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(0x30); writeLength(result, content.size()); content.writeTo(result);
        return result.toByteArray();
    }

    private void writeLength(ByteArrayOutputStream output, int length) {
        if (length < 128) { output.write(length); return; }
        int bytes = 0; int value = length;
        while (value > 0) { bytes++; value >>= 8; }
        output.write(0x80 | bytes);
        for (int shift = (bytes - 1) * 8; shift >= 0; shift -= 8) output.write((length >> shift) & 0xff);
    }

    private String markdown(CodingProblem problem, CodingSubmission submission) {
        return "# " + problem.getTitle() + "\n\n"
            + "- 난이도: " + problem.getDifficulty() + "\n"
            + "- 핵심 문법: " + problem.getGrammarName() + "\n"
            + "- 테스트 결과: " + submission.getPassedCount() + "/" + submission.getTotalCount() + " 통과\n\n"
            + "## 문제 설명\n\n" + problem.getDescription() + "\n\n"
            + "## 제출 코드\n\n~~~java\n" + submission.getSourceCode() + "\n~~~\n";
    }

    private User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private String randomState() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return base64Url(bytes);
    }
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("GitHub 연결 인증값을 처리하지 못했습니다.", exception);
        }
    }
    private String base64Url(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private void requireConfigured() {
        if (appId.isBlank() || appSlug.isBlank() || privateKeyPem.isBlank()) throw new IllegalStateException("GitHub App 환경변수 설정이 필요합니다.");
    }
}
