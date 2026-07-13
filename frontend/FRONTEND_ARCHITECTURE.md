# HWV Code Lab 프런트엔드 구조

## 목표 흐름

1. 사용자가 로그인 또는 회원가입
2. 프로젝트 파일이나 폴더 업로드
3. 백엔드가 파일을 안전하게 저장하고 AI에 문제 생성을 요청
4. 사용자가 문제 화면에서 코드 작성
5. 백엔드 실행 샌드박스가 여러 테스트 케이스로 코드 실행
6. 실패 시 AI 힌트, 성공 시 코드 보완점 반환
7. 제출 결과를 사용자 계정에 저장
8. 오답노트와 학습 통계에서 기록 조회

## 폴더 구조

~~~text
src/
├─ components/
│  ├─ common/                 공통 버튼과 인증 화면 컴포넌트
│  └─ lab/
│     ├─ CodeEditor.jsx       코드 입력 영역
│     └─ TestCasePanel.jsx    테스트 통과/실패 결과
├─ data/
│  └─ problemSeed.js          개발용 문제 데이터
├─ layouts/
│  ├─ AppLayout.jsx           로그인 후 공통 헤더와 사이드바
│  └─ AppLayout.css
├─ pages/
│  ├─ LoginPage.jsx           기존 로그인
│  ├─ SignupPage.jsx          기존 회원가입
│  └─ lab/
│     ├─ DashboardPage.jsx
│     ├─ ProblemCreatePage.jsx
│     ├─ ProblemListPage.jsx
│     ├─ ProblemWorkspacePage.jsx
│     ├─ WrongNotesPage.jsx
│     ├─ StatisticsPage.jsx
│     ├─ ProfilePage.jsx
│     └─ LabPages.css
├─ services/
│  ├─ authApi.js              기존 인증 요청
│  ├─ api.js                  기존 공통 요청
│  └─ problemApi.js           문제 생성·조회·제출·통계 요청
└─ App.jsx                    전체 라우트
~~~

## 라우트

| 경로 | 역할 |
|---|---|
| /login | 로그인 |
| /signup | 회원가입 |
| /dashboard | 최근 학습과 정답률 |
| /problems | AI 생성 문제 목록 |
| /problems/new | 파일·폴더 업로드 및 문제 생성 |
| /problems/:problemId | 문제 풀이, 코드 제출, 테스트·힌트 |
| /wrong-notes | 실패한 제출 기록 |
| /statistics | 분야별 정답률과 제출 추이 |
| /profile | 사용자 정보 |

## 필요한 Spring API

### 문제 생성

~~~http
POST /api/projects/problems
Content-Type: multipart/form-data

files: File[]
language: java
difficulty: 보통
count: 3
focus: 자료구조와 예외 처리
~~~

응답:

~~~json
[
  {
    "id": "stack-implementation",
    "title": "스택 구현하기",
    "difficulty": "쉬움"
  }
]
~~~

### 문제 목록과 상세

~~~http
GET /api/problems
GET /api/problems/{problemId}
~~~

### 코드 제출

~~~http
POST /api/submissions
Content-Type: application/json

{
  "problemId": "stack-implementation",
  "language": "java",
  "sourceCode": "class Solution { ... }"
}
~~~

응답:

~~~json
{
  "status": "failed",
  "tests": [
    {
      "id": 1,
      "name": "테스트 1",
      "status": "passed",
      "runtime": "12ms",
      "memory": "3.21MB"
    },
    {
      "id": 3,
      "name": "테스트 3",
      "status": "failed",
      "input": "[1, 2]",
      "expected": "-1",
      "actual": "0"
    }
  ],
  "hint": "빈 스택에서 pop을 호출하는 경우를 확인하세요.",
  "improvement": "경계값 처리와 메서드 분리를 보완하세요."
}
~~~

### 사용자 기록

~~~http
GET /api/me/dashboard
GET /api/me/wrong-notes
GET /api/me/statistics
~~~

## 백엔드 저장 구조

운영 데이터는 localStorage가 아니라 MySQL에 저장해야 합니다.

~~~text
users
projects             업로드 프로젝트 메타데이터
project_files        파일 경로·이름·언어·저장 위치
problems             AI 생성 문제와 설명
test_cases           입력·기대 출력·공개 여부
submissions          사용자 코드·언어·결과·실행 시간
submission_results   테스트별 통과 여부와 실제 출력
ai_feedback          힌트·보완점
~~~

모든 문제·제출·통계 API는 로그인 사용자 ID를 서버에서 확인해야 합니다.

## 백엔드 실행 보안

- 브라우저에서 eval 또는 new Function으로 사용자 코드를 실행하지 않습니다.
- Spring 서버 프로세스에서 직접 Runtime.exec로 코드를 실행하지 않습니다.
- 별도 격리 실행 서비스에서 CPU, 메모리, 시간, 네트워크를 제한합니다.
- 테스트 케이스의 정답과 비공개 입력은 프런트엔드로 미리 보내지 않습니다.
- 업로드 파일의 확장자, 크기, 개수, 압축 해제 경로를 서버에서 검증합니다.
- 제출 코드와 실행 로그에는 개인정보와 API 키를 포함하지 않습니다.

## 개발 모드와 운영 모드

현재 프로토타입은 화면 확인을 위해 기본적으로 모의 API를 사용합니다.

~~~env
VITE_USE_MOCK_API=true
~~~

Spring API가 준비되면 다음처럼 바꿉니다.

~~~env
VITE_API_BASE_URL=https://your-api.example.com
VITE_USE_MOCK_API=false
~~~

개발 모드의 브라우저 저장 데이터는 시연용일 뿐이며 실제 사용자 기록으로
사용하면 안 됩니다.
