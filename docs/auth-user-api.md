# 찍먹 로그인·회원가입·유저 프로필 API 명세

> 정리 기준일: 2026-10-01  
> 출처: 백엔드 팀원이 작성한 Notion API 페이지 본문 및 사용자가 제공한 API 데이터베이스 스크린샷  
> 권장 저장 경로: `docs/auth-user-api.md`

이 문서는 Android 로그인·회원가입 및 관련 API 연동을 위해 제공된 명세를 Markdown으로 정리한 문서다. Method와 URL은 데이터베이스 스크린샷을, 요청·응답·오류는 각 페이지 본문을 기준으로 작성했다. 중복된 이메일 인증번호 확인 설명은 하나로 통합했다. 실제 서버 호출로 검증한 문서는 아니다.

## 목차

- [1. 구현 시 문서 해석 기준](#reading-rules)
- [2. API 목록](#api-list)
- [3. 공통 규칙](#common-rules)
- [4. API 상세](#api-details)
  - [4.1 회원가입](#signup)
  - [4.2 이메일·비밀번호 로그인](#login)
  - [4.3 유저 프로필 조회](#get-user)
  - [4.4 유저 프로필 수정](#update-user)
  - [4.5 구글 로그인](#google-login)
  - [4.6 이메일 인증번호 전송](#send-email-code)
  - [4.7 비밀번호 찾기·재설정](#reset-password)
  - [4.8 이메일 인증번호 확인](#verify-email-code)
  - [4.9 로그인 상태의 비밀번호 변경](#change-password)
- [5. 연동 흐름](#integration-flows)
- [6. 테스트 계정·권한](#test-accounts)
- [7. 백엔드 확인 필요 사항](#open-questions)

<a id="reading-rules"></a>
## 1. 구현 시 문서 해석 기준

1. **`미기재`와 `확인 필요`는 확정된 서버 계약이 아니다.** 누락된 응답 필드, 인증 조건, 상태 코드, 검증 규칙을 임의로 추가하지 않는다.
2. JSON 필드명과 대소문자를 그대로 사용한다. `specialDiet`, `dislikedIngredients`, `idToken`, `currentPassword`, `newPassword`를 다른 이름으로 변환해 전송하지 않는다.
3. `allergies`, `diseases`, `specialDiet`, `dislikedIngredients`는 원문상 **String**이다. 앱에서 여러 항목을 선택하더라도 서버가 배열을 받는다고 가정하지 않는다. 쉼표가 들어간 예시는 있으나 정확한 직렬화 규칙은 미기재다.
4. 필수 여부 `X`는 필드를 생략할 수 있다는 의미로만 읽는다. `null`, 빈 문자열, `"없음"`이 같은 의미인지는 명시되지 않았다.
5. 성공 응답 형태가 API마다 다르다. 로그인은 최상위 `token`, 회원가입은 숫자 `data`, 프로필 조회는 객체 `data`, 비밀번호 변경은 `data: null`, 일부 API는 `message`만 반환한다.
6. 회원가입 응답에는 JWT가 없다. 로그인 응답에는 `userId`가 없다. 구글 로그인 응답에도 `userId`나 신규 가입 여부 필드는 없다.
7. 원문에 HTTP 성공 상태가 없는 API는 `200` 또는 `201`로 단정하지 않는다. 오류 예시는 제공된 범위만 수록했으며 서버의 모든 오류를 나열한 것은 아니다.
8. 프로필 수정의 생략 필드 처리와 인증번호 자릿수처럼 충돌·누락이 있는 항목은 [확인 필요 사항](#open-questions)을 참고한다.

<a id="api-list"></a>
## 2. API 목록

스크린샷에서 아래 9개 API는 모두 담당자 **이성민**, 상태 **완료**로 표시되어 있다. 이 표의 완료 표시는 문서상의 개발 상태이며 실서버 검증 결과를 뜻하지 않는다.

| 번호 | 기능 | Method | URL | Authorization | 성공 HTTP 상태 |
| --- | --- | --- | --- | --- | --- |
| 1 | 유저 프로필 생성·회원가입 | `POST` | `/api/auth/signup` | 원문 헤더에 미기재 | 미기재 |
| 2 | 이메일·비밀번호 로그인 | `POST` | `/api/auth/login` | 원문 헤더에 미기재 | 미기재 |
| 3 | 유저 프로필 조회 | `GET` | `/api/users/{id}` | Bearer JWT 필수 | 미기재 |
| 4 | 유저 프로필 수정 | `PUT` | `/api/users/{id}` | 미기재·확인 필요 | 미기재 |
| 5 | 구글 로그인 | `POST` | `/api/auth/google` | 불필요·비로그인 호출 | `200 OK` |
| 6 | 이메일 인증번호 전송 | `POST` | `/api/auth/email/send` | 생략 | `200 OK` |
| 7 | 비밀번호 찾기·재설정 | `POST` | `/api/auth/password/reset` | 불필요·비로그인 호출 | `200 OK` |
| 8 | 이메일 인증번호 확인 | `POST` | `/api/auth/email/verify` | 불필요·회원가입 전 호출 | `200 OK` |
| 9 | 로그인 상태의 비밀번호 변경 | `PUT` | `/api/users/{id}/password` | Bearer JWT 필수 | `200 OK` |

> 프로필 조회 본문은 `/api/users/{userId}`, 스크린샷은 `/api/users/{id}`로 표기한다. 두 표기는 같은 위치에 유저 ID를 넣는 경로 템플릿이며, 이 문서의 URL은 스크린샷 표기인 `{id}`로 통일했다.

<a id="common-rules"></a>
## 3. 공통 규칙

### 3.1 서버 주소와 요청 형식

- **Base URL:** 제공되지 않음. 개발·운영 서버 주소를 별도로 확인해야 한다.
- 본문의 URL은 서버 주소를 제외한 경로다.
- 요청·응답 본문은 제공된 JSON 예시를 따른다.
- 회원가입과 이메일 로그인 원문은 `Content-Type: application/json`을 명시한다. 그 외 JSON 본문 API에 대해서도 클라이언트가 이 Content-Type을 사용하는 것을 연동 권장사항으로 둔다. 해당 API의 원문 헤더에 모두 명시된 것은 아니다.
- 헤더 `생략` 또는 인증 불필요 표기는 토큰 없이 호출하는 문맥이다. JSON 본문 전송 시 Content-Type을 제거하라는 의미로 해석하지 않는다.

### 3.2 인증

인증이 필요한 요청에는 서버에서 발급받은 서비스 JWT를 사용한다.

```http
Authorization: Bearer {발급받은_JWT_토큰}
```

- 이메일 로그인과 구글 로그인은 성공 시 최상위 `token`을 반환한다.
- 구글에서 받은 `idToken`은 서버에 전달하는 입력값이고, 서버가 응답한 `token`이 서비스 API 인증에 쓰는 JWT다.
- JWT 만료 시간, 갱신 API, 로그아웃 API, JWT claim 구조는 제공되지 않았다.

### 3.3 타입과 오류 형식

| 원문 타입 | JSON 표현 | Android 연동 시 해석 |
| --- | --- | --- |
| `String` | 문자열 | 문자열 그대로 사용 |
| `Long` | 정수 숫자 | 유저 ID는 Kotlin `Long`에 대응 |
| `Object` | 객체 | API별 응답 필드에 맞춰 해석 |
| `null` | `null` | 비밀번호 변경 응답의 `data` 예시에 명시 |

제공된 오류 본문은 다음 형식이다. 프로필 수정과 인증번호 전송 등 오류가 기재되지 않은 API까지 동일 형식이라고 확정하지 않는다.

```json
{
  "status": 400,
  "message": "오류 메시지"
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `status` | Integer | 오류 상태 코드 |
| `message` | String | 오류 설명 |

<a id="api-details"></a>
## 4. API 상세

<a id="signup"></a>
### 4.1 유저 회원가입

**`POST /api/auth/signup`**  
Notion 페이지명: 유저 프로필 생성

#### Request

```http
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | String | O | 유저 이메일. 로그인 ID로 사용 |
| `password` | String | O | 유저 비밀번호. 서버에서 암호화 처리 |
| `nickname` | String | O | 앱에서 표시할 닉네임 |
| `allergies` | String | X | 알레르기 정보 |
| `diseases` | String | X | 기저 질환 정보 |
| `specialDiet` | String | X | 특별 식단. 예: 비건, 할랄 |
| `dislikedIngredients` | String | X | 기피 식재료 |

```json
{
  "email": "test@jjikmuk.com",
  "password": "<테스트용 비밀번호>",
  "nickname": "찍먹테스터",
  "allergies": "땅콩, 밀",
  "diseases": "당뇨",
  "specialDiet": "비건",
  "dislikedIngredients": "오이"
}
```

#### Response

성공 HTTP 상태: **미기재**.

```json
{
  "message": "회원가입 성공",
  "data": 1
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |
| `data` | Long | 생성된 유저의 고유 ID |

#### Error

**400 Bad Request — 이미 가입된 이메일**

```json
{
  "status": 400,
  "message": "이미 가입된 이메일입니다."
}
```

**400 Bad Request — 필수 입력값 누락**

```json
{
  "status": 400,
  "message": "필수 입력값(email, nickname)이 누락되었습니다."
}
```

#### 연동 참고

- `password`는 요청 필드 표에서 필수다. 누락 오류 예시에 `email`, `nickname`만 나오더라도 비밀번호를 선택값으로 해석하지 않는다. 비밀번호 누락 시 정확한 오류는 확인이 필요하다.
- 이메일 인증 완료 여부를 서버가 회원가입 요청에서 어떻게 확인하는지는 미기재다. 회원가입 요청에는 별도 인증번호나 인증 완료 토큰 필드가 없다.
- 성공 응답에는 JWT가 없다. 로그인 상태로 전환하려면 별도 로그인 흐름이 필요하다.
- 예시 이메일은 [관리자 테스트 계정](#test-accounts)과 같다. 이미 등록된 서버에서 그대로 회원가입하면 중복 이메일 오류가 날 수 있다. 예시 `data: 1`은 모든 신규 가입자의 ID가 1이라는 뜻이 아니다.

<a id="login"></a>
### 4.2 유저 로그인 및 토큰 발급

**`POST /api/auth/login`**  
Notion 페이지명: 유저 로그인

#### Request

```http
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | String | O | 가입한 유저의 이메일 |
| `password` | String | O | 가입한 유저의 비밀번호 |

```json
{
  "email": "test@jjikmuk.com",
  "password": "<별도로 공유받은 테스트 계정 비밀번호>"
}
```

#### Response

성공 HTTP 상태: **미기재**.

```json
{
  "message": "로그인 성공",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ0..."
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |
| `token` | String | 서버에서 발급한 JWT. 이후 인증이 필요한 API 요청 헤더에 포함 |

#### Error

**401 Unauthorized — 이메일이 없거나 비밀번호 불일치**

```json
{
  "status": 401,
  "message": "이메일 또는 비밀번호가 일치하지 않습니다."
}
```

#### 연동 참고

응답에 유저 ID가 없다. 프로필 조회·수정·비밀번호 변경에 사용할 본인 ID 획득 방법을 확인해야 한다. JWT 예시를 근거로 특정 claim에 ID가 있다고 확정하지 않는다.

<a id="get-user"></a>
### 4.3 유저 프로필 조회

**`GET /api/users/{id}`**

#### Request

```http
Authorization: Bearer {발급받은_JWT_토큰}
```

| Path 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | Long | O | 조회할 유저의 고유 ID. 본문 표기는 `userId` |

Request Body: **없음**.

권한 규칙:

- `ADMIN`: 모든 유저 정보 조회 가능.
- `USER`: 본인 정보만 조회 가능. 다른 유저 ID 조회 시 `403`.

#### Response

성공 HTTP 상태: **미기재**.

```json
{
  "message": "조회 성공",
  "data": {
    "id": 1,
    "email": "test@jjikmuk.com",
    "nickname": "찍먹테스터",
    "allergies": "땅콩, 밀",
    "diseases": "당뇨",
    "password": "$2a$10$QGVcUB1qb7hwhKxTzjTA8.Vl5UsKq0mfEzfMs...",
    "role": "ADMIN",
    "specialDiet": "비건",
    "dislikedIngredients": "오이"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |
| `data` | Object | 유저 상세 정보 |
| `data.id` | Long | 유저 고유 ID |
| `data.email` | String | 유저 이메일 |
| `data.nickname` | String | 유저 닉네임 |
| `data.allergies` | String | 알레르기 정보 |
| `data.diseases` | String | 기저 질환 정보 |
| `data.password` | String | 암호화된 비밀번호 해시값 |
| `data.role` | String | 유저 권한. `USER` 또는 `ADMIN` |
| `data.specialDiet` | String | 특별 식단 |
| `data.dislikedIngredients` | String | 기피 식재료 |

각 응답 필드의 nullable 여부는 원문에 미기재다.

#### Error

**403 Forbidden — 미로그인 또는 일반 계정으로 다른 사람의 ID 조회**

```json
{
  "status": 403,
  "message": "권한이 없습니다."
}
```

**404 Not Found — 존재하지 않는 유저 ID**

```json
{
  "status": 404,
  "message": "사용자를 찾을 수 없습니다."
}
```

#### 연동 참고

- 미로그인 시 상태 코드는 원문에 적힌 `403`을 보존했다. 잘못된 토큰·만료된 토큰의 상태 코드와 본문은 별도 미기재다.
- 응답에 `password` 해시가 포함된 것은 원문 그대로다. **클라이언트 처리 권장:** 이를 화면 표시나 비밀번호 비교에 사용하지 않는다. 서버 응답에서 해당 필드를 제외할 예정인지 백엔드에 확인한다.

<a id="update-user"></a>
### 4.4 유저 프로필 수정

**`PUT /api/users/{id}`**

#### Request

Request Header 및 인증·권한 조건: **미기재·확인 필요**.

| Path 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | Long | O | 수정할 유저의 고유 ID |

Request Body 예시:

```json
{
  "email": "update@gmail.com",
  "nickname": "수정된테스터",
  "allergies": "우유, 땅콩",
  "diseases": "없음"
}
```

다음 표는 요청 예시에 등장한 필드를 정리한 것이다. 원문에 별도 필드 정의표는 없다.

| 필드 | 예시상 타입 | 필수 여부 | 예시 내용 |
| --- | --- | --- | --- |
| `email` | String | 미기재 | 변경할 이메일 |
| `nickname` | String | 미기재 | 변경할 닉네임 |
| `allergies` | String | 미기재 | 변경할 알레르기 정보 |
| `diseases` | String | 미기재 | 변경할 기저 질환 정보. 예시는 `"없음"` |

#### Response

성공 HTTP 상태: **미기재**.

아래는 주석으로 나머지 필드를 생략한 **원문의 개략 예시**다. 완전한 응답 스키마 또는 그대로 파싱할 JSON으로 사용하지 않는다.

```jsonc
{
  "message": "프로필 수정 완료",
  "data": {
    "id": 1
    // ... 수정된 데이터 반환
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |
| `data` | Object | 수정된 데이터. 전체 필드 미기재 |
| `data.id` | Long | 예시에 포함된 유저 ID |

#### Error

상태 코드 및 오류 본문: **미기재**.

#### 확인 필요

- `specialDiet`, `dislikedIngredients`도 수정할 수 있는지.
- `PUT` 요청에서 생략한 필드가 유지되는지, 초기화되는지.
- 각 필드의 필수 여부와 `null`·빈 문자열·`"없음"` 처리.
- Authorization 필수 여부와 본인·관리자 수정 권한.
- 이메일 변경 시 중복 확인·재인증 여부.
- 정확한 성공 응답 전체 필드와 오류 응답.

<a id="google-login"></a>
### 4.5 구글 소셜 로그인

**`POST /api/auth/google`**  
Notion 페이지명: (구글) 유저 로그인

Android 앱에서 받은 구글 `id_token`을 서버에 전달하면 서버가 유효성을 검증하고 서비스 JWT를 발급한다. DB에 없는 최초 로그인 유저는 임시 닉네임을 부여해 자동 회원가입 처리한다.

#### Request

원문 Request Header: **생략·비로그인 상태에서 호출**.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `idToken` | String | O | Android 구글 로그인 성공 후 받은 JWT 형식의 구글 ID 토큰 |

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZ..."
}
```

#### Response — 200 OK

```json
{
  "message": "구글 로그인 성공",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |
| `token` | String | 서버가 발급한 앱 전용 JWT |

원문은 이 토큰을 이후 모든 API 요청 헤더에 포함하도록 안내한다. 이 문서에서는 비로그인 호출 API의 별도 설명도 함께 보존한다.

#### Error

**400 Bad Request — 구글 계정에서 이메일 정보를 가져올 수 없음**

```json
{
  "status": 400,
  "message": "구글 계정에서 이메일 정보를 가져올 수 없습니다."
}
```

**401 Unauthorized — 조작되었거나 만료된 구글 토큰**

```json
{
  "status": 401,
  "message": "유효하지 않은 구글 토큰입니다."
}
```

#### 연동 참고

- 구글 토큰을 설명할 때의 `id_token`과 실제 요청 키를 구분한다. 서버 요청 키는 정확히 `idToken`이다.
- 응답에는 신규 회원 여부, 유저 ID, 프로필 완성 여부가 없다. 최초 로그인 후 추가 정보 입력 화면으로 이동할 조건은 별도로 확인해야 한다.
- 구글 로그인 설정에 필요한 Client ID 등 환경 설정값은 제공되지 않았다.

<a id="send-email-code"></a>
### 4.6 이메일 인증번호 전송

**`POST /api/auth/email/send`**  
Notion 페이지명: 이메일 인증번호 전송 (회원가입 / 비밀번호 찾기 공통)

사용자의 이메일 주소로 인증번호를 발송한다. 본문에는 **6자리**, 유효시간 **5분**으로 적혀 있다. 페이지명과 스크린샷은 회원가입·비밀번호 찾기 공통 API로 안내한다.

#### Request

원문 Request Header: **생략**.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | String | O | 인증번호를 받을 사용자의 실제 이메일 주소 |

```json
{
  "email": "user@example.com"
}
```

#### Response — 200 OK

```json
{
  "message": "인증번호가 이메일로 발송되었습니다."
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |

#### Error

이 API의 오류 상태 코드와 본문: **미기재**.

#### 연동 참고

- 요청에는 `email`만 있다. 회원가입·비밀번호 찾기 용도를 구분하는 필드는 기재되어 있지 않다.
- 비밀번호 재설정 페이지에는 **4자리**가 적혀 있어 충돌한다. 확인 전 공통 입력 UI의 길이를 확정하지 않는다.
- 재전송 제한, 재발송 시 이전 코드의 유효 여부, 가입·미가입 이메일별 발송 정책은 미기재다.
- 같은 Notion 페이지 안의 `/api/auth/email/verify` 설명은 [4.8](#verify-email-code)에 통합했고, `/api/auth/password/reset` 참조는 [4.7](#reset-password)에 정리했다.

<a id="reset-password"></a>
### 4.7 비밀번호 찾기·재설정

**`POST /api/auth/password/reset`**  
Notion 페이지명: 비밀번호 찾기

비밀번호를 잊은 사용자가 이메일 인증번호와 새 비밀번호를 전달하면, 서버가 **인증번호 검증과 비밀번호 재설정을 동시에 처리**한다. 비로그인 상태에서 호출한다.

> 원문은 이 API의 인증번호를 **4자리**로 명시하며 예시도 `"1234"`다. 공통 발송 API의 **6자리** 설명과 일치하지 않으므로 원문을 유지하고 확인 대상으로 둔다.

#### Request

원문 Request Header: **생략·비로그인 호출**.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | String | O | 인증번호를 발송했던 가입자의 이메일 주소 |
| `code` | String | O | 메일로 수신한 인증번호. 원문은 4자리이며 확인 필요 |
| `newPassword` | String | O | 새로 설정할 비밀번호 |

```json
{
  "email": "user@example.com",
  "code": "1234",
  "newPassword": "newPassword123!"
}
```

#### Response — 200 OK

```json
{
  "message": "비밀번호가 성공적으로 재설정되었습니다."
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |

#### Error

| HTTP 상태 | 발생 조건 | 오류 본문 |
| --- | --- | --- |
| `400 Bad Request` | 인증번호가 틀렸거나 유효시간 5분이 지남 | 정확한 본문 미기재 |
| `404 Not Found` | 가입되지 않은 이메일 | 아래 예시 |

```json
{
  "status": 404,
  "message": "가입되지 않은 이메일입니다."
}
```

#### 연동 참고

- 이 API 자체가 인증번호를 검증한다. 회원가입용 `/api/auth/email/verify`를 먼저 호출해야 한다는 계약은 없다.
- `/api/auth/email/verify`는 성공 시 임시 인증 데이터를 삭제한다. 재설정 전에 같은 코드를 이 API로 소비하는 흐름을 임의로 추가하지 않는다.
- 재설정 성공 응답에는 JWT가 없다. 기존 로그인 세션의 유지·무효화 여부는 미기재다.

<a id="verify-email-code"></a>
### 4.8 이메일 인증번호 확인

**`POST /api/auth/email/verify`**

회원가입을 진행하는 사용자가 이메일로 받은 **6자리 인증번호**를 올바르게 입력했는지 확인한다.

#### Request

- Authorization: **불필요**. 회원가입 전 비로그인 상태에서 호출.
- Query Parameter: **없음**.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | String | O | 인증번호를 발송했던 이메일 주소 |
| `code` | String | O | 사용자가 입력한 6자리 인증번호 |

```json
{
  "email": "test2@jjikmuk.com",
  "code": "123456"
}
```

원문의 공통 발송 페이지에는 같은 요청 구조에 `email: "user@example.com"`을 사용한 예시도 있다.

#### Response — 200 OK

```json
{
  "message": "이메일 인증이 완료되었습니다."
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |

#### Error

**400 Bad Request — 인증번호 불일치**

```json
{
  "status": 400,
  "message": "인증번호가 일치하지 않습니다."
}
```

**400 Bad Request — 유효시간 5분 만료**

```json
{
  "status": 400,
  "message": "인증 시간이 만료되었습니다."
}
```

**400 Bad Request — 인증 요청 내역 없음**

```json
{
  "status": 400,
  "message": "인증 요청 내역이 없습니다."
}
```

**500 Internal Server Error — 서버 내부 DB 조회·검증 중 예상하지 못한 오류**

```json
{
  "status": 500,
  "message": "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
}
```

#### 원문에 명시된 프론트엔드 처리

인증이 성공하면 서버의 임시 검증 테이블 `email_verifications`에서 해당 데이터가 **즉시 삭제**된다. 클라이언트는 `200 OK`를 받으면 화면을 **인증 성공 상태**로 확정하고 **회원가입 완료 버튼**을 활성화한다.

인증 성공 후의 서버 측 증명 방식, 인증 완료 상태의 유효기간, 이메일 변경 시 처리 방식은 미기재다.

<a id="change-password"></a>
### 4.9 로그인 상태의 비밀번호 변경

**`PUT /api/users/{id}/password`**

로그인한 유저가 현재 비밀번호 또는 발급받은 임시 비밀번호를 확인한 후 새 비밀번호로 변경한다.

#### Request

```http
Authorization: Bearer {발급받은_JWT_토큰}
```

| Path 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | Long | O | 비밀번호를 변경할 본인의 유저 ID |

| Body 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `currentPassword` | String | O | 현재 비밀번호 또는 이메일로 받은 임시 비밀번호 |
| `newPassword` | String | O | 새로 사용할 비밀번호 |

```json
{
  "currentPassword": "oldPassword123!",
  "newPassword": "newPassword456!"
}
```

#### Response — 200 OK

```json
{
  "message": "비밀번호가 성공적으로 변경되었습니다.",
  "data": null
}
```

| 필드 | 예시상 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |
| `data` | null | 예시에서 추가 반환 데이터 없음 |

#### Error

**400 Bad Request — 현재 비밀번호 불일치**

```json
{
  "status": 400,
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

**403 Forbidden — 타인의 ID로 비밀번호 변경 시도**

```json
{
  "status": 403,
  "message": "본인의 비밀번호만 변경할 수 있습니다."
}
```

**404 Not Found — 존재하지 않거나 탈퇴한 유저 ID**

```json
{
  "status": 404,
  "message": "사용자를 찾을 수 없습니다."
}
```

#### 연동 참고

- 프로필 조회의 관리자 권한 설명을 이 API의 타인 비밀번호 변경 권한으로 확대하지 않는다. 본문은 본인 비밀번호만 변경할 수 있다고 명시한다.
- 임시 비밀번호를 발급하는 API는 이번 제공 자료에 없다. 비밀번호 재설정 API가 임시 비밀번호를 발급한다고 해석하지 않는다.
- 구글 가입자의 현재 비밀번호 처리와 변경 후 기존 JWT의 유효 여부는 미기재다.

<a id="integration-flows"></a>
## 5. 연동 흐름

아래는 제공된 API를 화면 흐름에 연결하기 위한 정리다. **권장 흐름은 새로운 서버 계약을 뜻하지 않으며**, 별도 확인이 필요한 구간을 함께 표시했다.

### 5.1 이메일 회원가입

1. 이메일 입력 후 `POST /api/auth/email/send` 호출.
2. 6자리 인증번호 입력 후 `POST /api/auth/email/verify` 호출.
3. `200 OK`를 받으면 인증 성공 상태로 바꾸고 회원가입 완료 버튼 활성화. 이 단계는 원문에 명시됨.
4. 필수 정보 및 선택 프로필 정보로 `POST /api/auth/signup` 호출.
5. 생성된 유저 ID를 반환받음. 로그인까지 이어갈 경우 `POST /api/auth/login`을 호출하는 흐름을 구성.

**확인 필요:** 인증 성공 사실을 서버가 회원가입과 연결하는 방식 및 인증 완료 후 이메일 변경 처리.

### 5.2 이메일 로그인·프로필 조회

1. `POST /api/auth/login` 호출.
2. 최상위 `token`을 이후 인증 요청에 사용.
3. 본인 유저 ID를 확보한 뒤 `GET /api/users/{id}` 호출.

**확인 필요:** 로그인 응답에 ID가 없으므로 3단계의 본인 ID 획득 방법.

### 5.3 구글 로그인

1. Android 앱에서 구글 ID 토큰 획득.
2. `POST /api/auth/google`에 `idToken`으로 전달.
3. 응답의 서비스 JWT `token`으로 인증 요청 처리.

**확인 필요:** 구글 로그인 환경 설정값, 본인 유저 ID 획득 방법, 신규 가입·추가 프로필 입력 여부 판단 방식.

### 5.4 비밀번호 찾기

1. 가입 이메일로 `POST /api/auth/email/send` 호출.
2. 받은 코드와 새 비밀번호를 `POST /api/auth/password/reset`에 전달.
3. 재설정 성공 후 로그인 화면으로 안내하는 흐름을 구성할 수 있음.

**확인 필요:** 발송 문서의 6자리와 재설정 문서의 4자리 중 실제 계약. `/email/verify`를 중간 단계로 추가하지 않는다.

### 5.5 로그인 상태의 비밀번호 변경

1. 본인 ID, 서비스 JWT, 현재 비밀번호, 새 비밀번호를 준비.
2. `PUT /api/users/{id}/password` 호출.
3. `200 OK`와 성공 메시지 처리. 응답의 `data`는 예시상 `null`.

**확인 필요:** 변경 후 세션 유지·재로그인 정책.

<a id="test-accounts"></a>
## 6. 테스트 계정·권한

원문의 프론트엔드 API 연동 테스트 가이드다. 일반 계정으로 다른 유저의 프로필을 조회하면 `403 Forbidden`이 발생할 수 있으므로 계정 권한을 구분한다.

GitHub 공유본에서는 테스트 계정 비밀번호를 제거했다. 위 요청 예시의 비밀번호 자리표시는 실제 호출 전에 테스트용 값으로 바꾸고, 기존 계정의 비밀번호는 백엔드 담당자에게 별도 공유받는다.

| 구분 | ID | 이메일 | 비밀번호 | 권한 | 프로필 조회 범위 |
| --- | --- | --- | --- | --- | --- |
| 관리자 테스트 계정 | `1` | `test@jjikmuk.com` | 별도 공유 | `ADMIN` | 모든 유저 |
| 일반 계정 | 원문 기준 `2`번 이후 가입자 | 예: `test2@jjikmuk.com` | 원문 미기재. 회원가입 시 설정한 값 | `USER` | 본인만 |

위 ID 구분은 제공된 테스트 데이터 설명이다. 클라이언트에서 ID 숫자로 권한을 판정하는 규칙으로 사용하지 않는다.

<a id="open-questions"></a>
## 7. 백엔드 확인 필요 사항

아래 항목은 원문 간 불일치 또는 구현에 필요한 누락 정보다. 확인 전에는 서버의 확정된 동작으로 취급하지 않는다.

| 번호 | 항목 | 문서에 있는 내용 | 확인할 내용 |
| --- | --- | --- | --- |
| Q1 | 인증번호 자릿수 | 공통 발송·회원가입 확인은 6자리, 비밀번호 재설정은 4자리 | 재설정에 실제로 발송·허용되는 자릿수와 수정해야 할 명세 |
| Q2 | 로그인 후 본인 ID | 이메일·구글 로그인 모두 `message`, `token`만 반환 | ID 응답 추가, 명시된 JWT claim, 별도 본인 조회 API 중 실제 사용 방식 |
| Q3 | 프로필 수정 범위 | 예시에 `email`, `nickname`, `allergies`, `diseases`만 존재 | `specialDiet`·`dislikedIngredients` 지원, 필수 필드, 생략 필드 유지·초기화, 전체 응답·오류 |
| Q4 | 프로필 수정 인증 | Header·권한 설명 없음 | Bearer JWT 필수 여부와 본인·관리자 권한 |
| Q5 | 이메일 인증과 회원가입 연결 | 인증 성공 시 검증 데이터 삭제. 회원가입 요청에는 인증 증명 필드 없음 | 서버가 인증 완료를 확인하는 방식, 유효기간, 이메일 변경 처리 |
| Q6 | 서버·구글 설정 | Base URL과 구글 Client ID 등 미제공 | 개발·운영 Base URL 및 구글 로그인에 사용할 설정값 |
| Q7 | 입력값 정책 | 비밀번호·닉네임 상세 검증 규칙 미기재. 비밀번호는 필수이나 누락 오류 예시에 빠짐 | 길이·허용 문자·중복 정책, 비밀번호 누락 오류, 이메일 변경 재인증 |
| Q8 | 식이·건강 정보 표현 | String 타입과 쉼표 예시만 존재 | 허용 값, 여러 선택값 결합 규칙, 생략·`null`·빈 문자열·`"없음"`의 의미, 응답 nullable 여부 |
| Q9 | 성공 상태·오류 계약 | 회원가입·이메일 로그인·프로필 조회·수정 성공 상태 미기재. 일부 오류 예시 누락 | 정확한 HTTP 성공 상태, API별 미기재 오류 본문, 무효·만료 JWT 오류 |
| Q10 | 세션 수명 | 로그인 시 JWT 하나만 반환 | 만료 시간, 갱신·로그아웃 지원 여부, 비밀번호 변경·재설정 후 세션 정책 |
| Q11 | 구글 신규 회원 처리 | 자동 가입·임시 닉네임 설명만 존재 | 신규 회원·온보딩 필요 여부 확인 방식, 구글 가입자의 비밀번호 변경 가능 여부 |
| Q12 | 인증번호 재발송 | 유효시간 5분과 이메일만 받는 공통 요청 구조 | 재발송 제한, 이전 코드 무효화, 가입·미가입 이메일별 동작, 용도 구분 방식 |
| Q13 | 프로필 응답의 비밀번호 해시 | 조회 예시에 `data.password` 포함 | 실제 반환 여부와 응답에서 제외할 계획 |
| Q14 | 임시 비밀번호 표현 | 로그인 상태의 변경 API 설명에만 등장 | 별도 임시 비밀번호 발급 기능이 있는지, 남아 있는 이전 설명인지 |

---

문서 편집 범위: 제목·표·코드 블록 정리, 중복 설명 통합, Path 변수 표기 통일, JSON 주석 분리, 명백한 오탈자 수정. 요청·응답의 필드명·값과 오류 메시지는 원문을 기준으로 보존하되, GitHub 공유를 위해 테스트 계정 비밀번호와 이를 재사용한 요청 예시는 자리표시로 교체했다. 프로필 수정의 불완전한 응답은 `jsonc` 개략 예시로 표시했으며, 추가한 해석·권장사항과 확인 필요 사항은 서버 계약과 구분했다.
