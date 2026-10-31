# API 명세 시작하기

> 검토·동기화: 2026-10-31. 백엔드 main `7dd4273ee456329593a82b2b60934ef1b8c20240` 기준. 이번 작업은 문서 갱신이며 서버 배포·앱 API 설정 변경은 하지 않았다.

## 먼저 읽을 문서

| 목적 | 문서 |
| --- | --- |
| 백엔드 원문 전체 API / 요청·응답 찾기 | [전체 API 명세](backend/api-spec/API_REFERENCE.md) |
| 로그인·세션·사진·가족·분석 대상 최신 계약 | [인증·프로필 v2](backend/api-spec/frontend-auth-integration-v2.md) |
| 후속 API·사진 화면 연결 현황 | [계정 리소스 연결](../auth-account-resources.md) |
| 기존 프론트 구현 기준과 주의점 | [인증 API 정리](auth-api.md), [세션·프로필 정리](auth-profile-session-api.md) |
| 가입 알레르기·식단 ID | [전체 매핑표](../auth-signup-condition-mapping.md) |
| 상품 내부 ID·용량·관측 이력 | [상품 ID 계약](backend/catalog-evolution-stage1-3.md) |
| 챗봇 공통 판정과 AI 연동 | [챗봇 계약](backend/api-spec/chat-analysis-contract-v1.md) |
| OCR 서비스 | [기존 OCR 명세](scanner-ocr-api.md) — 별도 서비스, 이번 Backend 명세가 대체하지 않음 |
| 남은 개발·회신·사용자 결정 | [로컬 인수인계](../인수인계/로그인-회원가입.md), [백엔드 답변 정리](../인수인계/인증-백엔드-답변과-요청.md) |

## 최신 여부 확인 결과

- 사용자 제공 `f49478e`는 main에 병합돼 있다. 현재 main은 merge 커밋 1개 앞서며 GitHub compare 결과 파일 차이는 없다. 따라서 전달받은 내용은 현재 저장소 최신 내용과 같다.
- main 커밋 시각은 2026-10-29 11:39 UTC(20:39 KST)다. 문서의 10월 15일/23일/28일은 각 설명의 작성·검증 기준일이며 파일이 저장소 구버전이라는 뜻은 아니다.
- AuthController/UserController 및 사진·가족 컨트롤러, ProfileInputs/AccountInputPolicy, JWT 필터, AuthSessions, UserPreferenceCatalog의 관련 경로·검증을 대조했다. 전체 백엔드 기능을 실행 검증한 것은 아니다.
- [GitHub 고정 버전](https://github.com/KW-coworker/JJIKMUK-Backend/tree/7dd4273ee456329593a82b2b60934ef1b8c20240/docs), [전달 커밋과 비교](https://github.com/KW-coworker/JJIKMUK-Backend/compare/f49478ea2b51ca25bdffd82ad08e112ce8b44e68...7dd4273ee456329593a82b2b60934ef1b8c20240).

## 원문 읽는 우선순위

1. 인증·프로필·사진·가족은 **인증 v2**가 전체 명세의 오래된 설명보다 우선한다.
2. 챗봇은 **chat-analysis-contract-v1**, 상품 ID 추가 계약은 **catalog-evolution-stage1-3**을 함께 읽는다.
3. 나머지 기본 경로·본문은 **API_REFERENCE**를 사용한다. 이 폴더의 프론트 정리 문서는 보조 자료이며 사용자 결정·앱 구현 상태를 별도로 표시한다.

원문에는 과거 설명이 남아 있다. 예: 탈퇴 섹션의 사진·가족 로컬 전제는 최신 v2/실제 UserService의 서버 사진·가족·세션 삭제로 대체한다. 일부 구 예시 시각의 offset 누락, 과거 Google 자동 연결/24시간 설명도 최신 v2보다 우선하지 않는다. 단일 파일의 모든 줄을 같은 시점의 완전한 계약으로 취급하지 않는다.

## 실제 서버 사용 가능 여부

팀 개발 주소는 `https://jjikmuk-backend-dev.tail198441.ts.net`로 명시돼 있다. 그러나 **실행 중인 서버의 커밋, DB 마이그레이션 적용, SMTP/Google 설정, Tailscale 접근은 아직 확인하지 않았다.**

[백엔드의 10월 28일 검증 보고](backend/staging-integration-20261028.md)는 원격 서버 timeout/배포 버전 미확인과 모의 메일·AI 기반 테스트를 기록한다. 이것은 당시 보고이며 현재 서버가 꺼져 있다는 증거는 아니다. 이 보고의 Android 지적도 과거 `feature/product-search` 커밋 대상이며 현재 인증 브랜치의 완료 작업을 취소하는 근거가 아니다.

명세를 기반으로 개발할 수 있지만 실서버 보장은 별도다. 배포 담당에게 실행 커밋·필요 마이그레이션·SMTP/Google 설정·테스트 계정을 확인한 뒤 실제 이메일 가입부터 검증한다.

## 원문 보관·다음 갱신

`backend/`의 5개 MD는 위 커밋의 원문을 수정 없이 복사했다. [출처·SHA-256](backend/source-manifest.json)으로 동일성을 확인할 수 있다. 보조 설명·정책 결정은 원문 밖에 작성한다. 재동기화 시 main SHA와 파일 diff를 확인하고 함께 갱신한다. 자동 동기화 기능은 없다.

인수인계와 주고받은 PDF는 기존대로 로컬 전용이다. 이번에 원격 푸시/PR은 하지 않았다.
