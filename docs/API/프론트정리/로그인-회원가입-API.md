# 찍먹 인증 API 명세 — 로그인·회원가입

> 2026-11-01 계약 보완: 로그인 429 LOGIN_RATE_LIMITED는 서버 retryAfterSeconds(우선)/Retry-After로 대기하며 자동 재요청하지 않는다. 닉네임 사전 확인은 가입에서는 공개 조회, 인증된 추가 프로필에서는 Bearer로 본인 제외 조회를 사용한다. 403 PROFILE_INCOMPLETE는 세션을 보존하고 본인 정보를 다시 읽어 추가 입력으로 이동한다. 상세 우선 계약은 ../backend/api-spec/frontend-auth-integration-v2.md.


> 요청·응답 계약을 찾는 프론트 참고 문서다. 구현은 [개발 현황](../../개발/로그인-회원가입/구현현황.md), 정책/요청은 [통합 협의안](../../협의/로그인-회원가입-백엔드-협의안.md), 실제 배포 응답은 [검증 기록](../../검증/2026-10-31-인증-실서버-점검.md)에서 관리한다.

> 최종 검토: **2026-10-31**. 백엔드 main `7dd4273`의 공식 원문·관련 컨트롤러와 대조했다. [명세 시작하기/출처와 우선순위](../README.md), [전체 원문](../backend/api-spec/API_REFERENCE.md), [우선 적용 인증 v2](../backend/api-spec/frontend-auth-integration-v2.md)를 함께 읽는다.
> 이 파일은 프론트용 정리이며 원문 PDF/인수인계는 로컬 전용이다. 실제 자격 정보는 포함하지 않는다.

**저장소 최신 계약 확보와 실서버 배포 검증은 다르다.** Access 15분/Refresh 최초 로그인부터 30일 고정과 갱신 응답 유실 재로그인은 사용자가 이미 수용했다. ASCII 비밀번호는 사용자 확정이며 사진 저장소/가족 세부 정책은 협의안에서 관리한다. 기관 HTTPS 주소는 반영했으며 실제 실행 커밋·메일/Google 설정 확인 및 서버 오류 해결은 후속이다.

이번 문서는 기존 인증 8개 API를 갱신한다. 새 Refresh·Google 연결·본인 프로필·사진·가족 및 상품 분석 대상 계약은 **[세션·프로필 확장 명세](세션-프로필-API.md)**를 함께 읽는다. 충돌하는 구버전의 ‘24시간/갱신 없음’, ‘Google 자동 연결’, ‘PATCH/PUT으로 프로필 완료’, ‘로그아웃 서버 폐기 없음’, ‘시간대 없는 이메일 시각’ 설명은 폐기한다.

## 목차

- [1. Codex 구현 시 핵심 규칙](#implementation-rules)
- [2. API 목록](#api-list)
- [3. 공통 데이터·인증·시간 규칙](#common-rules)
- [4. API 상세](#api-details)
  - [4.1 닉네임 사용 가능 여부 조회](#nickname-availability)
  - [4.2 이메일 인증번호 발송 및 재전송](#email-send)
  - [4.3 이메일 인증번호 검증 및 증명 발급](#email-verify)
  - [4.4 회원가입](#signup)
  - [4.5 이메일 로그인](#login)
  - [4.6 Google 로그인 및 자동 가입](#google-login)
  - [4.7 비밀번호 재설정](#password-reset)
  - [4.8 로그아웃](#logout)
- [5. 화면과 연결할 호출 순서](#flows)
- [6. 추가 확인 항목](#open-questions)

<a id="implementation-rules"></a>
## 1. Codex 구현 시 핵심 규칙

1. **OTP는 앞자리 0이 가능한 4자리 문자열**이다. `"0427"`을 숫자로 변환하지 않는다. OTP 유효시간은 발급 시점부터 5분이다.
2. **발송과 검증 요청에는 `purpose`가 필수**다. 회원가입은 `signup`, 비밀번호 재설정은 `password_reset`을 사용한다.
3. **OTP 검증 성공 후 `verificationToken`을 받아 최종 요청에 제출한다.** 회원가입용 증명은 검증 성공 시점부터 30분, 재설정용 증명은 5분 유효하다.
4. **`verificationToken`은 서비스 JWT가 아니다.** 최종 가입·재설정의 JSON 본문에 넣으며 `Authorization` 헤더에 넣지 않는다.
5. **비밀번호 재설정은 `email`, `verificationToken`, `newPassword`를 보낸다.** 재설정 API에 `code`를 직접 보내는 이전 계약은 사용하지 않는다.
6. **회원가입 성공은 `201 Created`이고 ID는 `data.userId`에 있다.** 이전의 숫자 `data` 응답과 구분한다. 가입 성공 응답에 로그인 토큰은 없으며 이후 이메일 로그인 API를 호출한다.
7. **이메일·Google 로그인 응답은 최상위 필드 구조다.** `token`, `userId`, `profileCompleted` 등을 `data` 안에서 찾지 않는다. 본인 ID를 JWT 예시에서 추측할 필요가 없다.
8. **Google 로그인 후 추가 프로필 입력 여부는 `profileCompleted`로 판단한다.** `isNewUser=false`이면서 `profileCompleted=false`인 사용자도 있다.
9. **같은 이메일·목적의 재전송 성공은 이전 OTP와 이전 인증 완료 증명을 무효화한다.** OTP 검증 성공은 OTP를 소비하며, 최종 가입·재설정 성공은 증명을 소비한다.
10. **닉네임 중복 조회는 중복이어도 `200`과 `available=false`다.** 사전 조회는 예약이 아니므로 최종 가입·수정에서 중복을 다시 검사한다.
11. **로그아웃은 현재 기기 서버 세션의 Access/Refresh를 폐기한다.** 앱도 로컬 세션을 정리한다. 비밀번호 변경·재설정은 모든 기기 세션을 무효화한다. 구형 sid 없는 JWT의 로그아웃은 모든 세션을 폐기하는 호환 예외가 있다.
12. **가입 선택 정보는 쉼표 연결 String 또는 null이다.** 알레르기·식단 ID를 JSON 배열이나 한글 표시명으로 임의 변경하지 않는다.
13. **Google HTTP 200은 로그인 성공을 보장하지 않는다.** `code=GOOGLE_LINK_REQUIRED`, `loginGranted=false` 응답에는 서비스 토큰이 없다. 연결 증명과 서비스 세션을 분리한다.
14. **토큰 수명은 서버 응답을 사용한다.** 새 로그인 모델의 Refresh도 함께 암호화 저장하고 갱신한다. 새 계약의 구조·미확인 사항은 확장 명세와 [6절](#open-questions)을 따른다.

<a id="api-list"></a>
## 2. API 목록

| 번호 | 기능 | Method | URL | 인증 | 성공 상태 |
| --- | --- | --- | --- | --- | --- |
| 1 | 닉네임 사용 가능 여부 조회 | `GET` | `/api/auth/nicknames/availability` | 불필요 | `200 OK` |
| 2 | 이메일 인증번호 발송 및 재전송 | `POST` | `/api/auth/email/send` | 불필요 | `200 OK` |
| 3 | 이메일 인증번호 검증 및 증명 발급 | `POST` | `/api/auth/email/verify` | 불필요 | `200 OK` |
| 4 | 회원가입 | `POST` | `/api/auth/signup` | 불필요 | **`201 Created`** |
| 5 | 이메일 로그인 | `POST` | `/api/auth/login` | 불필요 | `200 OK` |
| 6 | Google 로그인 및 자동 가입 | `POST` | `/api/auth/google` | 불필요 | `200 OK` |
| 7 | 비밀번호 재설정 | `POST` | `/api/auth/password/reset` | 불필요 | `200 OK` |
| 8 | 로그아웃 | `POST` | `/api/auth/logout` | 서비스 JWT 필수 | `200 OK` |

닉네임 조회의 전체 경로는 본문에 명시된 `/api/auth/nicknames/availability`를 사용했다. 스크린샷에서는 경로 끝부분이 UI에 가려져 있다.

<a id="common-rules"></a>
## 3. 공통 데이터·인증·시간 규칙

### 3.1 서버 주소·요청 형식

- **Base URL:** 기관 이전 안내 기준 `https://43.203.42.36/`. 기존 명세의 주소 부재는 해결됐으나 해당 계약의 실제 배포 여부는 검증 기록과 함께 확인한다.
- JSON 본문을 보내는 요청은 `Content-Type: application/json`으로 구성한다. 검증 API에는 이 헤더가 명시되어 있고, 나머지 본문 API도 JSON 요청 예시를 기준으로 정리했다.
- 닉네임 조회는 Query Parameter를 사용하고 Body가 없다.
- 나머지 7개 API는 Query Parameter가 없으며, 로그아웃은 Body도 없다.
- 인증 불필요 API에는 클라이언트가 불필요한 Bearer 토큰을 자동 첨부하지 않도록 구성한다. 특히 이메일 인증 검증 원문은 불필요하게 보낸 Bearer 토큰이 만료·무효이면 공통 `401`이 발생할 수 있다고 명시한다.

### 3.2 토큰과 인증번호 구분

| 값 | 발급 주체·경로 | 용도·전달 위치 | 유효시간 |
| --- | --- | --- | --- |
| OTP `code` | 이메일 발송 API로 발송 | 검증 API Body의 `code` | 발급 시점부터 5분 |
| 가입용 `verificationToken` | `purpose=signup`으로 OTP 검증 성공 | 회원가입 Body의 `verificationToken` | 검증 성공 시점부터 30분, `1800`초 |
| 재설정용 `verificationToken` | `purpose=password_reset`으로 OTP 검증 성공 | 비밀번호 재설정 Body의 `verificationToken` | 검증 성공 시점부터 5분, `300`초 |
| Google `idToken` | Google | Google 로그인 Body의 `idToken` | 이번 문서에 Google 토큰 수명 미기재 |
| 서비스 JWT `token` | 이메일·Google 로그인 성공 | 인증 필요 API의 `Authorization: Bearer {token}` | 현재 서버 기본 `900`초(15분, 정책 합의 대기). 응답 만료 정보 사용 |

추가 서비스 자격 정보는 `refreshToken`(불투명 문자열, JWT 아님)이며 만료는 `refreshExpiresAt`이다. 현재 구현은 최초 로그인부터 30일 고정으로 갱신해도 연장하지 않는다. 회전·유실 처리는 확장 명세를 따른다.

증명 토큰은 **이메일·목적에 묶인 일회용 증명**이다. 다른 이메일이나 다른 목적에 재사용하지 않는다.

### 3.3 시간 필드

| 위치 | 필드 | 회신 기준 형식·의미 |
| --- | --- | --- |
| 이메일 발송·검증 | `expiresAt`, 발송의 `resendAvailableAt` | offset 포함, 예: `2026-10-23T21:05:00+09:00` |
| 발송·검증 | `expiresInSeconds` | 유효시간, 정수 초 |
| 로그인·갱신·연결 | `expiresAt`, `refreshExpiresAt` | UTC `Z` 문자열 |
| Google 연결 필요 | `expiresAt`, `expiresInSeconds` | 연결 증명 만료, 서비스 세션과 구분 |

**2026-11-01 배포 응답 차이:** 이메일 발송 200 응답에서 `expiresAt`/`resendAvailableAt`에 offset이 없는 날짜를 확인했다. 위 표는 백엔드 명세 기준이며 변경하지 않는다. 앱은 이메일 발송·검증에 한해 유효한 offset 없는 날짜를 절대시각으로 추측하지 않고, 필수 `expiresInSeconds`와 요청 시작 시점으로 만료를 관리한다. 로그인/Refresh/Google 증명에는 이 호환을 적용하지 않는다. 발송 지연과 재현 결과는 [실서버 점검 기록](../../검증/2026-10-31-인증-실서버-점검.md) 참고.

- offset 포함 문자열을 Instant로 해석한다. 구형 offset 없는 문자열에 임의로 Z를 붙이지 않는다.
- 서버가 준 마감 시각과 남은 시간을 반영하고, 표시용 타이머는 단조 시계로 관리한다. 서버가 최종 만료를 판정한다.
- OTP 5분, 가입 증명 30분, 재설정 증명 5분은 유지된다. 최종 `VERIFICATION_EXPIRED`는 재인증으로 복구한다.
- 재전송 오류의 `retryAfterSeconds`/`Retry-After`와 OTP 오류의 `remainingAttempts`를 추가 반영한다. 필드 간 우선순위와 서버/단말 시계 오차 기준은 후속 확인한다.

### 3.4 응답 구조·오류 해석

| API | 성공 응답 위치 |
| --- | --- |
| 닉네임 조회 | `message`, `data.nickname`, `data.available` |
| 이메일 발송 | `message`, `data` 안에 이메일·목적·시간 정보 |
| 이메일 검증 | `message`, `data` 안에 이메일·목적·증명·시간 정보 |
| 회원가입 | `message`, `data.userId` |
| 이메일·Google 로그인 | `message`, `token`, `tokenType`, `userId`, `email`, `expiresAt`, `expiresInSeconds`, `refreshToken`, `refreshExpiresAt`, `isNewUser`, `profileCompleted`, `authProvider`, `hasPassword` 모두 최상위. Google 연결 필요는 별도 모델 |
| 비밀번호 재설정 | `message` |
| 로그아웃 | `message`, `data.userId`, `data.serverTokenRevoked` |

제공된 검증 오류 예시는 다음 형태다.

```json
{
  "status": 400,
  "code": "OTP_INVALID",
  "message": "인증번호가 일치하지 않습니다. 남은 시도 횟수: 4회",
  "field": "code",
  "remainingAttempts": 4
}
```

| 필드 | 예시상 타입 | 의미 |
| --- | --- | --- |
| `status` | integer | HTTP 오류 상태 코드 |
| `code` | string | 오류 종류 |
| `message` | string | 오류 안내 문구 |
| `field` | string | 오류 관련 필드 |

각 API의 오류는 **HTTP 상태와 `code`를 함께** 확인한다. 회신은 `field`, `retryAfterSeconds`, `remainingAttempts`가 선택 필드라고 명시한다. 누락/null을 허용하고 message에서 숫자를 파싱하지 않는다. 일부 상품 오류는 message만 있으므로 일반 오류 처리도 유지한다.

`503 AUTH_SERVICE_UNAVAILABLE`, 네트워크 실패, `403 ACCESS_DENIED`는 일괄 로그아웃 사유가 아니다. 단 Refresh를 이미 보낸 뒤 처리 결과가 불명확한 경우는 동일 토큰 재시도가 금지되므로 확장 명세의 유실 규칙을 적용한다.

### 3.5 입력값·프로필 데이터

- **서버 현재 입력 규칙:** 새 비밀번호는 8자 이상, UTF-8 72바이트 이하, 영문 A–Z/a–z·숫자 0–9 각각 포함, ASCII U+0021~U+007E만 허용. 특수문자 필수 아님, trim/소문자화 금지. 기타 Unicode 불허 등 추가 범위는 최종 합의 대기. 기존 로그인은 새 생성 규칙을 강제하지 않는다.
- 닉네임은 NFKC/trim, 완성형 한글·영문·숫자·밑줄 2~20자, 계정 닉네임 대소문자 미구분 중복. 이메일은 NFKC/trim/소문자·형식 검사. 현재 프론트는 NFKC를 아직 반영하지 않았다.
- 닉네임 조회의 `data.nickname`은 정규화된 닉네임이다. 발송의 `data.email`, `data.purpose`도 정규화 문자열이다.
- 회원가입의 `allergies`는 쉼표 연결 알레르기 ID 문자열, `specialDiet`는 쉼표 연결 채식·영양조건 ID 문자열이다.
- 원문 예시: `egg,wheat`, `lactoOvoVegetarian,lowSugar,highProtein`.
- 선택값을 입력하지 않는 회원가입은 4개 선택 필드를 모두 `null`로 보내거나 생략한다. 빈 문자열·공백·ID 순서 처리 규칙은 미기재다.
- 지원하지 않는 알레르기·식단 ID 및 채식 유형 복수 선택은 `400 VALIDATION_FAILED` 대상이다. 회신이 지정한 `GET /api/products/classifications`(알레르기 26개), `GET /api/products/filters`(채식 6개·영양 6개)의 실제 응답으로 전체 정규 key를 확인한다. 이번 회신에는 전체 응답 JSON이 없다.

<a id="api-details"></a>
## 4. API 상세

<a id="nickname-availability"></a>
### 4.1 닉네임 사용 가능 여부 조회

**`GET /api/auth/nicknames/availability`**

인증 불필요. Request Body 없음. 성공 `200 OK`.

#### Query Parameter

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `nickname` | string | 예 | 사용 가능 여부를 확인할 닉네임 |

```http
GET /api/auth/nicknames/availability?nickname=찍먹_user1
```

#### 성공 응답 — 200 OK

```json
{
  "message": "닉네임 사용 가능 여부 조회 성공",
  "data": {
    "nickname": "찍먹_user1",
    "available": true
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | string | 처리 결과 메시지 |
| `data.nickname` | string | 정규화된 닉네임 |
| `data.available` | boolean | 사용 가능 여부 |

#### 동작·오류

- 이미 사용 중이면 **`409`가 아니라 `200`과 `available=false`**를 반환한다.
- 형식이 잘못되면 **`400 NICKNAME_INVALID`**다.
- 닉네임을 예약하지 않는다. 사전 조회가 가능으로 끝나도 최종 가입·수정에서 중복을 다시 검사한다.
- 현재 본인이 사용 중인 닉네임을 이 공개 API로 조회하면 `available=false`가 될 수 있다. **본인 프로필을 동일 닉네임으로 저장하는 것은 가능**하다.

<a id="email-send"></a>
### 4.2 이메일 인증번호 발송 및 재전송

**`POST /api/auth/email/send`**

인증 불필요. Query Parameter 없음. 성공 `200 OK`.

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | string | 예 | 인증번호를 받을 이메일 |
| `purpose` | string | 예 | 회원가입 `signup`, 비밀번호 찾기 `password_reset` |

```json
{
  "email": "user@example.com",
  "purpose": "signup"
}
```

비밀번호 찾기는 같은 URL과 요청 구조에 `purpose=password_reset`을 사용한다. **재전송도 같은 요청을 다시 보낸다.**

#### 성공 응답 — 200 OK

```json
{
  "message": "인증번호가 이메일로 발송되었습니다.",
  "data": {
    "email": "user@example.com",
    "purpose": "signup",
    "expiresAt": "2026-10-23T21:05:00+09:00",
    "expiresInSeconds": 300,
    "resendAvailableAt": "2026-10-23T21:01:00+09:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | string | 처리 결과 메시지 |
| `data.email` | string | 정규화된 이메일 |
| `data.purpose` | string | 정규화된 목적 |
| `data.expiresAt` | string | OTP 만료 시각. offset 포함 서버 시각 |
| `data.expiresInSeconds` | integer | OTP 유효시간, 초. `300` |
| `data.resendAvailableAt` | string | 재전송 가능 시각. offset 포함 서버 시각 |

**OTP 자체는 응답에 포함하지 않는다.**

#### 발송·재전송 정책

- 인증번호는 **앞자리 0이 가능한 4자리 문자열**이며 **5분** 유효하다.
- 최소 재전송 대기는 **60초**다.
- **활성 코드가 유지되는 연속 발송 구간에서 최초 발송 포함 최대 5회**다. 재전송 성공마다 만료가 새로 5분 설정되므로, 이를 **‘고정 5분당 5회’ 제한으로 설명하지 않는다.**
- 재전송 성공 시 같은 이메일·목적의 **이전 코드와 이전 인증 완료 증명 모두 무효화**된다.
- 메일 발송 예외로 트랜잭션이 실패하면 DB의 기존 상태는 롤백된다.
- 실제 메일 수신 시각은 이 응답과 별개다. **네트워크 타임아웃만으로 서버의 발송 실패를 단정하지 않는다.**

#### 오류

| HTTP | `code` | 상황 |
| --- | --- | --- |
| 400 | `EMAIL_INVALID` | 이메일 형식 오류 |
| 400 | `VALIDATION_FAILED` | 미지원 `purpose`. `field=purpose` |
| 409 | `EMAIL_ALREADY_REGISTERED` | 가입용 발송인데 이미 가입된 이메일 |
| 404 | `USER_NOT_FOUND` | 재설정용 발송인데 미가입 이메일 |
| 409 | `AUTH_METHOD_NOT_SUPPORTED` | Google 전용 계정에 재설정용 발송 요청 |
| 429 | `OTP_RATE_LIMITED` | 재전송 대기·발송 횟수 제한 |
| 500 | `INTERNAL_SERVER_ERROR` | 메일 발송 등 서버 오류 |

회신 기준 `429`는 선택 필드 `retryAfterSeconds`와 `Retry-After` 헤더를 제공한다. 정상 발송의 `resendAvailableAt`도 반영한다. 누락은 허용하며 message에서 시간을 추출하지 않는다. 구조화된 값 사이의 우선순위는 추가 확인한다.

<a id="email-verify"></a>
### 4.3 이메일 인증번호 검증 및 증명 발급

**`POST /api/auth/email/verify`**

이메일 OTP를 검증하고 회원가입 또는 비밀번호 재설정용 증명 토큰을 발급한다.

| 항목 | 내용 |
| --- | --- |
| 인증 | 불필요 |
| Content-Type | `application/json` |
| Query Parameter | 없음 |
| 성공 상태 | `200 OK` |
| 원문 명세 기준일 | 2026-10-15 |

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | string | 예 | 인증번호를 받은 이메일 |
| `purpose` | string | 예 | `signup` 또는 `password_reset` |
| `code` | string | 예 | 이메일로 받은 4자리 인증번호 |

인증번호는 앞자리 `0`을 보존해야 하므로 숫자가 아닌 문자열로 전송한다. 발송할 때 사용한 이메일·용도와 동일해야 한다.

회원가입 인증 예시:

```json
{
  "email": "user@example.com",
  "purpose": "signup",
  "code": "0427"
}
```

비밀번호 재설정 인증 예시:

```json
{
  "email": "user@example.com",
  "purpose": "password_reset",
  "code": "0427"
}
```

#### 성공 응답 — 200 OK

회원가입용 인증 성공 예시다. 시각과 토큰은 설명용 값이다.

```json
{
  "message": "이메일 인증이 완료되었습니다.",
  "data": {
    "email": "user@example.com",
    "purpose": "signup",
    "verificationToken": "example-verification-token",
    "expiresAt": "2026-10-23T21:30:00+09:00",
    "expiresInSeconds": 1800
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | string | 처리 결과 메시지 |
| `data.email` | string | 인증된 이메일 |
| `data.purpose` | string | 인증 목적 |
| `data.verificationToken` | string | 최종 가입 또는 재설정에 사용하는 일회용 증명. 로그인 토큰이 아님 |
| `data.expiresAt` | string(offset 포함) | 증명 만료 시각 |
| `data.expiresInSeconds` | integer | 증명 유효시간, 초 |

#### OTP와 증명의 유효시간

| 구분 | 유효시간 | 시작 시점 |
| --- | --- | --- |
| 이메일 OTP | 5분 | 코드 발급 시점 |
| 가입용 `verificationToken` | 30분, `1800`초 | OTP 검증 성공 시점 |
| 재설정용 `verificationToken` | 5분, `300`초 | OTP 검증 성공 시점 |

`purpose=password_reset`으로 검증하면 응답의 `expiresInSeconds`는 `300`이다.

#### 인증 성공 후 처리

- 성공하면 기존 OTP를 삭제하고 **별도 인증 완료 증명**을 저장한다.
- 가입용 증명은 `POST /api/auth/signup`의 Body에 전달한다.
- 재설정용 증명은 `POST /api/auth/password/reset`의 Body에 전달한다.
- 증명은 해당 이메일·목적으로만 사용한다.
- 최종 가입 또는 재설정 성공 시 증명을 소비한다.
- `verificationToken`은 로그인 JWT가 아니므로 `Authorization`에 넣지 않는다.
- 같은 OTP로 검증을 다시 호출하는 동작이 재사용 가능하다고 가정하지 않는다.

**이전 명세의 ‘성공 메시지만 반환하고 인증 데이터를 삭제한다’는 설명으로 구현하지 않는다.** 지금은 OTP를 삭제한 뒤 별도 증명 토큰을 발급한다.

#### 오류

| HTTP | `code` | 발생 조건 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST_BODY` | 필수 본문 필드 누락, JSON 형식·타입 오류 |
| 400 | `EMAIL_INVALID` | 이메일 형식 오류 |
| 400 | `VALIDATION_FAILED` | 지원하지 않는 `purpose` |
| 400 | `OTP_INVALID` | 4자리 형식 오류 또는 인증번호 불일치 |
| 400 | `OTP_NOT_REQUESTED` | 인증 요청 내역 없음 또는 이미 소비·삭제됨 |
| 410 | `OTP_EXPIRED` | OTP 5분 유효시간 만료 |
| 429 | `OTP_RATE_LIMITED` | 인증번호 검증 실패 횟수 초과 |
| 401 | 공통 인증 오류 — 정확한 코드 미제공 | 불필요하게 보낸 Bearer 토큰이 만료·무효 |

```json
{
  "status": 400,
  "code": "OTP_INVALID",
  "message": "인증번호가 일치하지 않습니다. 남은 시도 횟수: 4회",
  "field": "code",
  "remainingAttempts": 4
}
```

#### 실패 횟수·재전송·만료 화면

- 새 회신의 `remainingAttempts`가 있으면 표시하고 0이면 재발송으로 안내한다. 형식 오류·미요청 등 모든 오류에 횟수가 포함되는 것은 아니다.

- **형식이 올바른 오답**은 최대 5회까지 검사한다. 마지막 실패 시 코드를 삭제하고 `429`를 반환한다. 이후 같은 코드는 미요청으로 처리될 수 있다.
- **4자리 형식 자체가 잘못된 요청**도 `OTP_INVALID`지만 실패 횟수에는 포함하지 않는다.
- 같은 이메일·목적으로 재전송이 성공하면 이전 코드와 이전 증명 토큰이 무효화된다.
- 가입용 증명 발급 후에는 `expiresInSeconds`로 30분 타이머를 관리한다. 30분 경과 안내를 위한 별도 API는 없다.
- 최종 가입·재설정에서 `410 VERIFICATION_EXPIRED`가 오면 재인증으로 안내한다.
- 가입 재인증은 [발송](#email-send)과 [검증](#email-verify)을 모두 `purpose=signup`으로 다시 수행한다. 재설정은 두 요청 모두 `purpose=password_reset`을 사용하며, 증명 유효시간을 30분으로 적용하지 않는다.

<a id="signup"></a>
### 4.4 회원가입

**`POST /api/auth/signup`**

인증 불필요. Query Parameter 없음. 성공 **`201 Created`**.

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | string | 예 | 인증한 이메일 |
| `password` | string | 예 | 공통 비밀번호 정책 적용 |
| `nickname` | string | 예 | 공통 닉네임 정책 적용 |
| `verificationToken` | string | 예 | `signup` 인증 완료 증명 |
| `allergies` | string / null | 아니오 | 쉼표 연결 알레르기 ID |
| `diseases` | string / null | 아니오 | 직접 입력한 질환 |
| `specialDiet` | string / null | 아니오 | 쉼표 연결 채식·영양조건 ID |
| `dislikedIngredients` | string / null | 아니오 | 기피 식재료 |

```json
{
  "email": "user@example.com",
  "password": "Samplepass1!",
  "nickname": "찍먹_user1",
  "verificationToken": "example-signup-verification-token",
  "allergies": "egg,wheat",
  "diseases": null,
  "specialDiet": "lactoOvoVegetarian,lowSugar,highProtein",
  "dislikedIngredients": null
}
```

조건 미선택 가입은 같은 요청에서 `allergies`, `diseases`, `specialDiet`, `dislikedIngredients`를 모두 `null`로 보내거나 생략한다.

#### 성공 응답 — 201 Created

```json
{
  "message": "회원가입 성공",
  "data": {
    "userId": 42
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | string | 처리 결과 메시지 |
| `data.userId` | integer | 생성된 유저 ID |

- 가입 성공 응답에는 로그인 토큰이 없다. 이후 [이메일 로그인](#login) API를 호출한다.
- **일반 가입은 조건 미선택이어도 `profileCompleted=true`**다. 이는 가입 후 상태에 대한 설명이며 위 가입 응답에 해당 필드가 추가된다는 뜻은 아니다.
- 가입 성공 시 증명을 소비한다.

#### 오류

| HTTP | `code` | 상황 |
| --- | --- | --- |
| 400 | `EMAIL_INVALID` / `NICKNAME_INVALID` / `PASSWORD_INVALID` | 입력 정책 위반. `password` 누락 기본값도 `PASSWORD_INVALID`로 검사 |
| 400 | `VALIDATION_FAILED` | 알레르기·식단 미지원, 채식 유형 복수 선택 등 |
| 400 | `VERIFICATION_REQUIRED` | 빈 증명 또는 인증 완료 내역 없음 |
| 400 | `VERIFICATION_TOKEN_INVALID` | 저장된 증명과 토큰 불일치 |
| 410 | `VERIFICATION_EXPIRED` | 가입용 인증 완료 증명 30분 만료 |
| 409 | `EMAIL_ALREADY_REGISTERED` | 기존 이메일 |
| 409 | `NICKNAME_ALREADY_EXISTS` | 중복 닉네임 |
| 400 | `INVALID_REQUEST_BODY` | 필수 `verificationToken` 필드 자체 누락 |

#### 실패·재시도 규칙

- 실패 종류는 검증 순서에 따라 결정된다. 같은 요청에 여러 문제가 있으면 한 오류만 먼저 반환된다.
- 입력 오류로 가입 트랜잭션이 실패하면 유효한 증명을 보존하므로, 입력을 수정한 뒤 재시도할 수 있다.
- 응답만 유실되었는데 서버에서 이미 가입된 경우 재시도는 `EMAIL_ALREADY_REGISTERED`이며 로그인으로 진행한다.
- 증명은 이메일과 목적에 묶여 있다. 이메일을 바꾸거나 다른 목적의 증명을 쓰면 해당 이메일·목적의 완료 내역이 없어 `VERIFICATION_REQUIRED`가 될 수 있다.
- 따라서 모든 잘못된 증명 사용이 같은 오류 코드로 반환되는 것은 아니다.

<a id="login"></a>
### 4.5 이메일 로그인

**`POST /api/auth/login`**

인증 불필요. Query Parameter 없음. 성공 `200 OK`.

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | string | 예 | 로그인할 이메일 |
| `password` | string | 예 | 기존 계정 비밀번호 |

```json
{
  "email": "user@example.com",
  "password": "Samplepass1!"
}
```

#### 성공 응답 — 200 OK

```json
{
  "message": "로그인 성공",
  "token": "example-service-jwt",
  "tokenType": "Bearer",
  "userId": 42,
  "email": "user@example.com",
  "expiresAt": "2026-10-23T12:15:00Z",
  "expiresInSeconds": 900,
  "refreshToken": "example-refresh-token",
  "refreshExpiresAt": "2026-11-22T12:00:00Z",
  "isNewUser": false,
  "profileCompleted": true,
  "authProvider": "LOCAL",
  "hasPassword": true
}
```

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| `message` | string | 처리 메시지 |
| `token` | string | 서비스 JWT |
| `tokenType` | string | `Bearer` |
| `userId` | integer(Long) | 본인 ID |
| `email` | string | 저장 이메일 |
| `expiresAt` | string(Instant) | 로그인 토큰 만료 시각, UTC |
| `expiresInSeconds` | integer | Access 유효시간, 현재 기본 `900`초(정책 합의 대기) |
| `refreshToken` | string | 회전하는 불투명 Refresh Token, JWT 아님 |
| `refreshExpiresAt` | string(Instant) | Refresh 만료 시각, UTC |
| `authProvider` | string | `LOCAL` / `GOOGLE` / `LOCAL_AND_GOOGLE` |
| `hasPassword` | boolean | 비밀번호 로그인·변경 지원 여부 |
| `isNewUser` | boolean | 이메일 로그인에서는 `false` |
| `profileCompleted` | boolean | 추가 프로필 입력 완료 여부 |

#### 오류·비밀번호 처리

| HTTP | `code` | 상황 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST_BODY` | 요청 본문 오류 |
| 401 | `LOGIN_FAILED` | 미가입, 비밀번호 불일치, 잘못된 이메일 |
| 409 | `AUTH_METHOD_NOT_SUPPORTED` | Google 전용 계정 |

위 오류 등이 가능하다. 로그인 `password`는 **과거 계정 호환을 위해 신규 가입 규칙으로 다시 검사하지 않고 저장 해시와 비교**한다. 신규 가입 비밀번호 정책을 로그인 입력에 그대로 강제하지 않는다.

<a id="google-login"></a>
### 4.6 Google 로그인 및 자동 가입

**`POST /api/auth/google`**

인증 불필요. Query Parameter 없음. 성공 `200 OK`.

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `idToken` | string | 예 | Google에서 발급한 ID 토큰 |

```json
{
  "idToken": "example-google-id-token"
}
```

#### 성공 응답 — 200 OK

응답 필드와 타입은 [이메일 로그인](#login)과 같으며, `message`는 `구글 로그인 성공`이다. 다음은 새 계정 예시다.

```json
{
  "message": "구글 로그인 성공",
  "token": "example-service-jwt",
  "tokenType": "Bearer",
  "userId": 43,
  "email": "google-user@example.com",
  "expiresAt": "2026-10-23T12:15:00Z",
  "expiresInSeconds": 900,
  "refreshToken": "example-refresh-token",
  "refreshExpiresAt": "2026-11-22T12:00:00Z",
  "isNewUser": true,
  "profileCompleted": false,
  "authProvider": "GOOGLE",
  "hasPassword": false
}
```

#### 계정·프로필 정책

- 서버는 issuer, audience, 만료, 이메일 검증 여부와 Google subject를 검사한다. 앱/서버 OAuth Client ID 및 앱 등록 정보를 배포 전 맞춘다.
- 신규 사용자는 임시 닉네임으로 자동 가입한다.
- **`POST /api/users/me/profile/complete`만** `profileCompleted=true`로 바꾼다. PATCH/PUT은 완료 여부를 바꾸지 않는다. 상세는 확장 명세 참조.
- Google 가입자는 일반 가입 OTP를 반복할 필요가 없다.
- 기존 일반 가입 이메일과 같으면 **HTTP 200 + `GOOGLE_LINK_REQUIRED`**와 5분짜리 `linkToken`을 반환한다. 서비스 Access/Refresh는 없다. 사용자가 기존 비밀번호로 `POST /api/auth/google/link`를 완료해야 연결·로그인한다. 5분/오입력 5회는 현재 임시 구현값이다.
- 최초 가입 후 다시 로그인한 미완료 사용자는 `isNewUser=false`, `profileCompleted=false`일 수 있다. **화면 이동은 `profileCompleted`로 판단**한다.
- Google 전용 계정의 일반 비밀번호 로그인·찾기·변경은 지원하지 않는다.
- 두 방식이 연결된 계정은 기존 비밀번호 로그인이 가능하다.

#### 오류

| HTTP | `code` | 상황 |
| --- | --- | --- |
| 401 | `GOOGLE_TOKEN_INVALID` | Google 토큰 검증 실패. 현재 Google 검증 요청 예외도 이 코드 |
| 503 | `GOOGLE_LOGIN_UNAVAILABLE` | 서버 Client ID 미설정 |
| 400 / 500 | 공통 오류 — 상세 미제공 | 공통 요청·서버 오류 등이 가능 |

Google `idToken`과 응답의 서비스 JWT `token`을 구분한다. 이후 서비스 인증에는 서버가 발급한 `token`을 사용한다.

<a id="password-reset"></a>
### 4.7 비밀번호 재설정

**`POST /api/auth/password/reset`**

인증 불필요. Query Parameter 없음. 성공 `200 OK`.

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | string | 예 | 인증한 계정 이메일 |
| `verificationToken` | string | 예 | `password_reset`으로 검증한 일회용 증명 |
| `newPassword` | string | 예 | 새 비밀번호. 공통 비밀번호 정책 적용 |

```json
{
  "email": "user@example.com",
  "verificationToken": "example-reset-verification-token",
  "newPassword": "Newpassword2!"
}
```

#### 성공 응답 — 200 OK

```json
{
  "message": "비밀번호가 성공적으로 재설정되었습니다."
}
```

#### 호출 순서·처리 정책

1. [이메일 발송](#email-send): `purpose=password_reset`.
2. [이메일 검증](#email-verify): 동일 이메일·목적과 OTP를 보내 증명 수신.
3. 이 API: `email`, `verificationToken`, `newPassword` 제출.

- **`code`를 재설정 API에 직접 보내는 이전 계약은 사용하지 않는다.**
- 임시 비밀번호를 이메일로 발급하지 않는다.
- 서버는 새 비밀번호 정책을 검사한 후 사용자와 증명을 검증하고 변경한다.
- 증명은 검증 완료 후 5분 유효하며 한 번만 사용한다.
- **성공 시 모든 기존 JWT가 무효화된다.** 새 로그인 토큰은 반환하지 않는다.

#### 오류

| HTTP | `code` | 의미 |
| --- | --- | --- |
| 400 | `EMAIL_INVALID` | 이메일 오류 |
| 400 | `PASSWORD_INVALID` | 새 비밀번호 정책 위반 |
| 400 | `VERIFICATION_REQUIRED` | 인증 완료 증명 필요 |
| 400 | `VERIFICATION_TOKEN_INVALID` | 증명 토큰 오류 |
| 404 | `USER_NOT_FOUND` | 사용자 없음 |
| 409 | `AUTH_METHOD_NOT_SUPPORTED` | 지원하지 않는 인증 방식 |
| 410 | `VERIFICATION_EXPIRED` | 재설정용 증명 만료 |

공통 오류도 가능하다. 각 오류의 정확한 메시지·전체 본문은 미제공이다.

<a id="logout"></a>
### 4.8 로그아웃

**`POST /api/auth/logout`**

로그인 필수. Query Parameter와 Request Body 없음. 성공 `200 OK`.

```http
Authorization: Bearer {token}
```

```json
{
  "message": "로그아웃 처리되었습니다.",
  "data": { "userId": 42, "serverTokenRevoked": true }
}
```

메시지 내용은 동작 분기 기준이 아니다. **현재 기기 서버 세션의 Access/Refresh를 폐기**한다. 사진·가족은 유지한다. 구형 sid 없는 JWT는 기기를 식별할 수 없어 모든 세션을 폐기한다.

- 앱도 저장 토큰·로그인 상태를 삭제한다. 통신 실패여도 로컬 로그아웃은 가능하지만 서버 폐기 완료로 보고하지 않는다.
- Access 만료 상태에서 로그아웃하려는 경우의 정확한 처리(만료 Bearer 허용 여부/갱신 필요 여부/오프라인 서버 폐기)는 추가 확인한다.
- 프론트 2-3 구현: 양수 userId + serverTokenRevoked=true 및 로그아웃 대상 ID 일치를 확인해야 서버 폐기 완료로 안내한다. 로컬 정리는 먼저 수행하고 캡처한 Bearer로 한 번 요청한다. 만료 응답/통신 실패/503/손상 본문에서는 서버 폐기를 단정하지 않는다. 자동 갱신·재전송은 하지 않는다.
- 갱신과 logout이 동시에 처리될 때 logout 성공 이후 새 토큰도 사용할 수 없는지 서버의 경합 처리 보장을 확인한다. 프론트는 로그아웃 후 늦은 갱신 응답을 저장하거나 새 계정에 적용하지 않는다.
- 비밀번호 변경·재설정은 모든 서버 세션 폐기, 탈퇴는 세션·사진·가족 등 계정 자료 삭제로 구분한다.

<a id="flows"></a>
## 5. 화면과 연결할 호출 순서

### 5.1 이메일 회원가입

1. 닉네임 사전 확인이 필요한 화면에서 `GET /api/auth/nicknames/availability` 호출. 결과는 예약이 아니다.
2. `POST /api/auth/email/send`에 `email`, `purpose=signup` 전송.
3. 이메일로 받은 4자리 문자열을 `POST /api/auth/email/verify`에 동일 이메일·목적과 함께 전송.
4. 검증 응답의 `verificationToken`과 유효시간 `1800`초를 관리.
5. `POST /api/auth/signup`에 인증한 이메일·증명과 가입 입력값 전송.
6. **`201 Created`와 `data.userId`** 확인. 증명은 소비됨.
7. 이메일 로그인 API를 호출해 서비스 JWT와 본인 ID·프로필 상태 확보.

닉네임 사전 조회를 어느 입력 단계에 둘지는 화면 구성에 따른다. 입력 오류로 가입이 실패하면 유효한 증명으로 수정 후 재시도할 수 있다. 증명 만료나 재전송으로 무효화된 경우에는 유효한 새 증명을 사용한다.

### 5.2 비밀번호 찾기·재설정

1. `POST /api/auth/email/send`에 `purpose=password_reset` 전송.
2. `POST /api/auth/email/verify`에 동일 이메일·목적과 OTP 전송.
3. 검증 응답의 재설정용 `verificationToken`과 유효시간 `300`초 관리.
4. `POST /api/auth/password/reset`에 이메일·증명·새 비밀번호 전송.
5. 성공 시 증명이 소비되고 모든 기존 JWT가 무효화됨. 새 로그인 토큰은 반환되지 않으므로 이후 서비스 사용을 위해 다시 로그인.

**이 흐름에서는 `/email/verify`를 거친 뒤 재설정한다.** OTP 자체를 재설정에 다시 사용하는 구조가 아니며, 검증 결과로 받은 별도 증명을 전달한다.

### 5.3 Google 로그인·추가 프로필

1. Android에서 Google ID 토큰 획득.
2. `POST /api/auth/google`에 `idToken`으로 전송.
3. 서비스 JWT와 `userId`, `profileCompleted` 확인.
4. `profileCompleted=false`이면 추가 프로필 입력 흐름으로 연결. `isNewUser`만으로 분기하지 않음.
5. `POST /api/users/me/profile/complete` 최종 제출 성공 또는 GET me로 완료를 확인한 뒤 홈으로 이동. PATCH 중간 저장은 완료가 아니다.
6. 동일 이메일 일반 계정 충돌은 연결 필요 상태 → 기존 비밀번호 확인 → 연결 성공 후 서비스 세션 저장으로 분기한다.

일반 가입용 OTP 인증을 반복하지 않는다. 조건 미선택이라는 이유만으로 일반 가입자의 프로필을 미완료로 변경하지 않는다.

### 5.4 로그아웃

1. 서비스 JWT로 `POST /api/auth/logout` 호출.
2. 앱의 저장된 토큰·로그인 상태 삭제. 요청 실패 시에도 로컬 로그아웃 가능.

서버 성공 전 로컬 삭제만으로 서버 폐기 완료라고 해석하지 않는다. Refresh API와 새 세션 저장 규칙은 확장 명세를 따른다.

### 5.5 화면 분기에 영향을 주는 오류

| 응답·상황 | 문서 기준 처리 의미 |
| --- | --- |
| 닉네임 조회 `200`, `available=false` | 조회 성공이지만 해당 닉네임 사용 불가. 본인의 동일 닉네임 저장 예외 구분 |
| OTP `400 OTP_INVALID` | 형식 오류 또는 오답. 형식 오류는 실패 횟수에 미포함 |
| OTP `410 OTP_EXPIRED` | 이메일 인증번호 만료 |
| 최종 가입·재설정 `410 VERIFICATION_EXPIRED` | 인증 완료 증명 만료. 해당 목적의 발송·검증부터 재인증 |
| 발송·검증 `429 OTP_RATE_LIMITED` | 호출 API에 따라 발송 제한 또는 검증 실패 횟수 초과. 제공되지 않은 재시도 필드를 가정하지 않음 |
| `409 AUTH_METHOD_NOT_SUPPORTED` | 해당 계정이 요청한 인증 방식을 지원하지 않음. Google 전용 계정 정책 확인 |
| Google `profileCompleted=false` | 신규 여부와 별개로 추가 프로필 입력 필요 |
| 가입 응답 유실 후 재시도에서 `EMAIL_ALREADY_REGISTERED` | 서버 가입 완료 가능 상황에 대한 원문 지침은 로그인으로 진행 |

<a id="open-questions"></a>
## 6. 정책·요청 목록의 관리 위치

[통합 협의안](../../협의/로그인-회원가입-백엔드-협의안.md)에서 확정/미결정/백엔드 확인을 관리한다. 실제 응답 차이는 [실서버 점검](../../검증/2026-10-31-인증-실서버-점검.md), 코드 구현 상태는 [개발 현황](../../개발/로그인-회원가입/구현현황.md)을 따른다. 이 문서에는 별도 회신 초안을 추가하지 않는다.

## 7. 추가 확보한 계정 관리 계약

- 비밀번호 변경: `PUT /api/users/{id}/password`, Bearer + `{currentPassword,newPassword}`. 본인만 가능. 200 `{message,data:null}`. 현재 비밀번호 오류는 `400 LOGIN_FAILED`, field=currentPassword, 새 비밀번호 오류는 `PASSWORD_INVALID`, field=password이므로 화면의 newPassword 입력란에 연결한다. 성공은 모든 기기 세션 폐기 후 재로그인. Google 전용은 `AUTH_METHOD_NOT_SUPPORTED`.
- 탈퇴: `DELETE /api/users/{id}`, Bearer, Body 없음. 본인 또는 관리자, 앱에서는 본인만 사용. 200 `{message,data:null}`. 최신 v2/UserService 기준 계정·세션·서버 사진·가족 등 삭제. 기기의 계정별 데이터 정리는 프론트가 별도로 처리한다. 실제 재확인 UX는 사용자 디자인과 연결한다.
- 만료 Access 로그아웃: 현재 JWT 필터는 `/logout`을 만료 검사 예외로 두지 않아 `401 AUTH_TOKEN_EXPIRED`로 거절한다. ‘허용하는지 모름’은 해소됐으며, **만료 시 서버 세션을 폐기할 공식 대체 절차**를 백엔드와 정해야 한다. 프론트의 로컬 로그아웃은 가능하지만 서버 폐기 성공으로 표시하지 않는다.
- Refresh/logout은 코드상 동일 사용자→세션 잠금 순서를 사용한다. 경합 보장 문서화와 실제 DB/배포 검증은 별도이며, 코드 확인만으로 실서버 경합 검증 완료를 주장하지 않는다.
