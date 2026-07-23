## NAVER OGQ AI

### HWV (Help With Vision)

> **AI 기반 Java 맞춤형 학습 플랫폼**

HWV는 사용자가 업로드한 Java 코드를 AI가 분석하여 핵심 문법을 추출하고, 이를 기반으로 맞춤형 코딩 문제를 생성하는 AI 기반 학습 플랫폼입니다.

생성된 문제를 실제 Java 환경에서 컴파일 및 실행하여 채점하고, 제출 기록과 학습 결과를 관리함으로써 학습자의 지속적인 성장을 지원합니다.

#### 배경

프로그래밍 학습에서는 단순히 문법을 익히는 것보다 직접 코드를 작성하고 피드백을 받는 과정이 중요합니다.

하지만 기존 학습 플랫폼은 모든 사용자에게 동일한 문제를 제공하기 때문에 개인의 학습 수준과 작성한 코드에 맞는 맞춤형 학습이 어렵습니다.

또한 자신이 작성한 코드를 기반으로 부족한 개념을 파악하고 반복적으로 학습할 수 있는 환경이 부족하여 학습 효율이 떨어지는 문제가 있습니다.

#### 문제점

##### 1. 획일적인 학습 환경

기존 플랫폼은 정형화된 문제만 제공하기 때문에 개인의 수준과 학습 내용에 맞는 실습이 어렵습니다.

##### 2. 코드 기반 피드백 부족

사용자가 작성한 Java 코드를 분석하여 어떤 문법을 사용했고 어떤 개념을 더 학습해야 하는지 알려주는 맞춤형 피드백이 부족합니다.

##### 3. 지속적인 학습 관리의 어려움

문제를 해결한 이후에도 제출 기록과 학습 결과를 체계적으로 관리하기 어려워 반복 학습과 성장을 이어가기 어렵습니다.


#### 해결 방안

HWV는 사용자가 업로드한 Java 파일을 AI가 분석하여 핵심 문법을 추출하고, 이를 기반으로 개인 맞춤형 코딩 문제를 생성합니다.

생성된 문제는 실제 Java 컴파일러를 이용하여 실행 및 채점되며, 제출 결과와 학습 기록을 저장하여 학습자의 성장 과정을 지속적으로 관리합니다.

또한 Render 무료 서버의 초기 지연 문제를 해결하기 위해 로그인 전 프로젝트 소개 화면을 제공하고, 백그라운드에서 서버를 미리 활성화하여 보다 자연스러운 사용자 경험을 제공합니다.

#### 주요 기능

- Java 코드 업로드 및 분석
- AI 기반 코드 요약
- 핵심 문법 자동 추출
- 맞춤형 코딩 문제 생성
- 실제 Java 컴파일 및 실행
- 테스트 케이스 기반 자동 채점
- 제출 기록 관리
- 학습 통계 제공
- 프로젝트 소개 및 서버 상태 확인

#### 시스템 아키텍처

##### Client (Frontend)

- Java 파일 업로드
- AI 분석 결과 조회
- 생성된 코딩 문제 풀이
- 코드 제출
- 제출 결과 확인
- 학습 통계 조회

##### Server (Backend)

- Java 코드 분석
- 핵심 문법 추출
- Gemini API 호출
- 맞춤형 문제 생성
- Java 코드 컴파일 및 실행
- 테스트 케이스 자동 채점
- 결과 저장

##### AI Pipeline

- Java 코드 분석
- 코드 요약 생성
- 핵심 문법 생성
- 맞춤형 코딩 문제 생성
- 테스트 케이스 생성

##### Database

- 사용자 정보
- 분석 결과
- 생성된 문제
- 제출 기록
- 학습 통계

---

#### 사용 스택

| 분류 | 기술 |
|------|------|
| Backend | Spring Boot 4.1.0, Java 17, Spring Web, Spring Data JPA, Spring WebFlux |
| Frontend | React 19, JavaScript, CSS |
| Database | MySQL |
| AI | Google Gemini 3.1 Lite |
| Deployment | Render, Vercel |
| API Documentation | Springdoc OpenAPI (Swagger UI) |
| Design | Figma |
| IDE / Tools | IntelliJ IDEA, Visual Studio Code, GitHub, ChatGPT, Claude, Codex |

---

#### 실행 방법

##### Backend

```bash
git clone https://github.com/TheTeamFLux/HWV.git

cd backend

./gradlew bootRun
```

##### Frontend

```bash
cd frontend

npm install

npm run dev
```

---

#### AI 사용 내역

##### 서비스에 사용한 AI 모델

- Google Gemini 3.1 Lite

##### AI 활용 기능

- Java 코드 요약
- 핵심 문법 분석
- 맞춤형 코딩 문제 생성
- 테스트 케이스 생성
- 학습 콘텐츠 생성

##### 개발 과정에서 활용한 AI

- ChatGPT
- Claude
- Codex
- Google Gemini

##### 활용 내용

- 서비스 기획
- 백엔드 개발
- 프론트엔드 개발
- 코드 리팩토링
- UI/UX 디자인
- 테스트 코드 작성
- README 작성
- 발표 자료 제작

---

#### 오픈소스 패키지

#### Backend

| 패키지 | 용도 |
|--------|------|
| Spring Boot | 백엔드 프레임워크 |
| Spring Web | REST API 구현 |
| Spring Data JPA | ORM 및 데이터베이스 관리 |
| Spring Validation | 요청 데이터 검증 |
| Spring Security Crypto | 비밀번호 암호화 |
| Spring WebFlux | 비동기 HTTP 통신 |
| Google API Client | Google API 연동 |
| MySQL Connector/J | MySQL 연결 |
| Apache PDFBox | PDF 텍스트 추출 |
| Springdoc OpenAPI | Swagger API 문서 |
| Spring Boot DevTools | 개발 편의 기능 |
| Gradle | 프로젝트 빌드 |

#### Frontend

| 패키지 | 용도 |
|--------|------|
| React | 사용자 인터페이스 개발 |
| React DOM | 브라우저 렌더링 |
| React Router | 페이지 라우팅 |
| Lucide React | 아이콘 |
| Vite | 개발 및 빌드 |
| ESLint | 코드 품질 검사 |
| React Hooks ESLint Plugin | Hooks 규칙 검사 |
| CSS | UI 스타일링 |

---

#### 라이선스

본 프로젝트는 **NAVER OGQ AI 공모전** 출품을 목적으로 제작되었습니다.

This project is licensed under the **MIT License**.

---

# Team FLUX

> **Building AI-powered educational experiences for everyone.**

HWV는 AI를 활용하여 누구나 자신의 수준에 맞는 학습 기회를 누릴 수 있도록, 교육의 격차를 줄이는 서비스를 만드는 **FLUX** 팀의 프로젝트입니다.
- Vite
