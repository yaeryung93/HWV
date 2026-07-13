# HWV Code Lab

기존 로그인·회원가입 화면을 유지하면서 프로젝트 코드 기반 AI 문제 생성과
코드 풀이 화면을 추가한 React 프런트엔드 프로토타입입니다.

## 실행

~~~powershell
npm.cmd install
npm.cmd run dev
~~~

브라우저에서 로그인한 뒤 대시보드로 이동합니다. 현재 개발용 문제·제출
기능은 모의 API로 동작합니다.

## 주요 화면

- 대시보드
- 파일·폴더 업로드 및 문제 생성
- AI 생성 문제 목록
- 코드 작성 및 테스트 결과
- AI 힌트와 코드 보완점
- 오답노트
- 학습 통계
- 마이페이지

## Spring API 연결

.env 파일을 만들고 다음 값을 지정합니다.

~~~env
VITE_API_BASE_URL=https://your-api.example.com
VITE_USE_MOCK_API=false
~~~

필요한 엔드포인트와 응답 형태는 FRONTEND_ARCHITECTURE.md를 참고하세요.

## 중요

사용자 코드는 브라우저나 Spring 서버 프로세스에서 직접 실행하면 안 됩니다.
CPU·메모리·시간·네트워크가 제한된 별도 실행 샌드박스가 필요합니다.
