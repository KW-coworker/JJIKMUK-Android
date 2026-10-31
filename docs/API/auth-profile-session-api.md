# 인증 확장 계약 — 세션·프로필·사진·가족·상품 분석 대상

> 갱신: **2026-10-31**. 백엔드 main `7dd4273`의 [인증 v2 원문](backend/api-spec/frontend-auth-integration-v2.md)과 관련 구현을 대조했다. [출처/우선순위](README.md), [기본 인증](auth-api.md) 참조.
> 공식 원문 확보·일부 코드 대조 완료이며 실서버 배포 검증은 아니다. 사용자 합의와 서버 현재 구현값을 구분한다.

## 1. 변경 목록과 상태

| API | 인증 | 요청/성공 응답 핵심 |
| --- | --- | --- |
| POST `/api/auth/refresh` | Body Refresh, Bearer 불필요 | `{refreshToken}` → 최상위 로그인 모델 |
| POST `/api/auth/google/link` | 연결 증명 + 기존 비밀번호 | `{linkToken,currentPassword}` → 최상위 로그인 모델 |
| POST `/api/auth/google/link/cancel` | 연결 증명 | `{linkToken}` → 200 message |
| GET `/api/users/me` | Bearer | 200 `data: Profile` |
| PATCH `/api/users/me` | Bearer | 부분 수정 → 200 `data: Profile` |
| PUT `/api/users/me` | Bearer | 기존 전체 교체 호환 유지, 새 화면은 PATCH 권장 |
| POST `/api/users/me/profile/complete` | Bearer | 최종 전체 프로필 제출 → `data: Profile`, 완료 true |
| GET `/api/users/me/photo` | Bearer | JPEG/PNG **바이너리**, JSON 아님 |
| PUT `/api/users/me/photo` | Bearer | multipart `file` → 200 `data.photoUrl`, `authenticationRequired=true` |
| DELETE `/api/users/me/photo` | Bearer | Body 없음 → 200 message |
| GET `/api/users/me/family-profiles` | Bearer | 200 `data: FamilyProfile[]` |
| POST `/api/users/me/family-profiles` | Bearer | 가족 프로필 → 201 `data: FamilyProfile` |
| GET `/api/users/me/family-profiles/{id}` | Bearer | 200 `data: FamilyProfile` |
| PATCH `/api/users/me/family-profiles/{id}` | Bearer | 부분 필드 + `version` → 최신 FamilyProfile/version |
| DELETE `/api/users/me/family-profiles/{id}?version={version}` | Bearer | version 필수 → 200 message |

기존 login/google/logout의 주소는 같지만 응답·동작이 바뀌었다. 회신의 ‘신규 13개, 전체 40개’는 서버 전체 API 기준이며 이 표는 인증 프론트에 필요한 기존/추가 계약을 함께 나열한다. 비밀번호 변경·탈퇴는 전체 원문에서 확보했다. [기본 인증 7절](auth-api.md#7-추가-확보한-계정-관리-계약) 참조.

## 2. 새 로그인 모델·Refresh·로그아웃 (회신 1~4쪽)

이메일 로그인, 직접 Google 로그인, Google 연결 성공, Refresh 성공은 `data` 래퍼 없이 다음 필드를 반환한다.

```json
{
  "message": "로그인 성공",
  "token": "example-access-token",
  "tokenType": "Bearer",
  "userId": 17,
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

- `authProvider`: `LOCAL`, `GOOGLE`, `LOCAL_AND_GOOGLE`. Google 전용은 `hasPassword=false`이므로 비밀번호 메뉴를 설명으로 대체한다.
- 현재 서버 기본값: Access 15분, Refresh 최초 로그인부터 30일 고정. 갱신으로 Refresh 만료일을 연장하지 않는다. **2026-10-29 사용자 확정 정책**이며 앱은 서버의 만료 응답을 사용한다. 프론트에서 매번 30일을 더해 만료를 연장하지 않는다.
- Refresh는 32바이트 난수의 43자 base64url 문자열이다. JWT로 파싱하지 않는다. 서버에는 SHA-256 해시만 저장한다고 회신했다.
- `POST /api/auth/refresh` Body는 `{"refreshToken":"현재 값"}`. 성공 때 이전 Refresh는 소비되고 새 Access/Refresh를 함께 반환한다.
- 앱은 두 토큰을 원자적으로 교체 저장한다. 동시에 여러 요청에서 만료를 만나도 **갱신은 한 번만 수행**하고 결과를 공유한다. 일반 API 요청과 Refresh의 재시도 규칙을 분리한다.
- `401 REFRESH_TOKEN_REUSED`: 같은 Refresh 재사용. 해당 기기 세션 전체를 폐기하므로 방금 발급된 새 토큰도 무효. 다른 기기는 유지.
- `401 REFRESH_TOKEN_INVALID`: 만료/폐기된 Refresh. 재로그인 필요.
- **Refresh 요청 처리 여부 불명 또는 성공 응답 유실:** 기존 Refresh 재전송 금지, 현재 서버에는 중복 요청 복구 계약 없음. **2026-10-29 사용자가 재로그인 정책 수용.** 일반 GET me의 네트워크 실패는 세션 보존·재시도라는 규칙과 구분한다.
- 회신은 네트워크/503의 세션 보존도 설명한다. Refresh의 경우 **회전 전 확정 실패와 회전 후 유실을 어떻게 구별할지**는 추가 질문 대상이다. 모든 IOException을 안전한 재시도로 취급하지 않는다.
- login/google/refresh/google-link는 보통 Bearer를 첨부하지 않는다. 오래된 Bearer 때문에 자체 증명 검증이 차단되지 않는다고 회신했다.

로그아웃은 Body 없는 `POST /api/auth/logout`, Bearer 사용, 200 `data.userId`/`serverTokenRevoked=true`. 현재 기기 Access/Refresh를 폐기하며 사진·가족은 남는다. 구형 sid 없는 JWT는 모든 세션 폐기 예외가 있다. 비밀번호 변경·재설정은 tokenVersion 증가 + 모든 세션 폐기, 탈퇴는 계정 세션·사진·가족 삭제다. 변경은 PUT /api/users/{id}/password, 탈퇴는 DELETE /api/users/{id}이며 전체 원문과 UserController에서 확인했다.

프론트 2-3: 로그아웃 응답의 userId/true 폐기를 검증하고, 기기 정리 성공과 서버 폐기 확인을 별도 결과로 관리한다. 만료 Access도 캡처한 토큰으로 한 번 시도하되, 실패 시 서버 폐기 완료로 안내하지 않는다. 자동 갱신/재전송은 하지 않는다. 만료 Access 계약과 동시 갱신 후 폐기 보장은 실서버/백엔드 확인 대기다.

### 프론트 구현 지침 — 서버 구현 사실과 구분

- 2-2 구현: 저장 포맷 v3에 토큰 쌍/만료/인증 방식과 refreshPending을 한 암호문으로 저장한다. v1/v2 읽기는 호환하며 구형 정보에서 Refresh를 만들지 않는다.
- 2-2 구현: 로컬 Access 만료 또는 서버의 명시적 만료 시 한 번의 갱신을 공유한다. 갱신만으로 앱 시작 본인 검증을 통과시키지 않는다. GET me 기반 시작 복원은 3단계에서 구현했다. 로컬·모의 서버 검증과 실제 배포 서버 검증은 구분한다.
- 2-2 구현: HTTP 전 refreshPending 저장, 성공 토큰 쌍 저장 후 해제. pending 재시작/응답 유실/저장 실패는 재로그인하며, 계정 전환 후 늦은 성공은 버린다. 503/429 등은 소비 여부를 확인할 때까지 저장 정보는 유지하되 사용/재전송을 차단하는 임시 처리다. 이 차단은 서버가 승인한 재시도 규칙이 아니다.
- 인증 토큰 유효성과 `profileCompleted`를 별도 상태로 둔다. 미완료 사용자도 GET me/최종 완료/사진 등 필요한 요청을 할 수 있어야 한다. 홈 진입 제한과 보호 요청 가능 여부를 같은 boolean으로 묶지 않는다.
- 실패한 임의 POST를 갱신 후 일괄 재실행하지 않는다. GET 조회와 소비성 요청의 재실행 조건을 별도로 검토한다.
- 서버·세션 generation이 바뀐 후 늦게 도착한 응답은 버린다. 로그아웃 이후 갱신 성공으로 세션이 복원되면 안 된다.

## 3. Google 연결·최종 입력 (회신 4~6쪽)

`POST /api/auth/google`의 Body는 기존 `{idToken}`이다. 서버는 issuer/audience/만료/검증 이메일/Google subject를 검사한다. Android와 서버의 OAuth 설정은 별도 확보한다.

같은 이메일의 일반 계정이 있으면 **200이지만 로그인 미허가**다.

```json
{
  "message": "기존 계정 소유 확인 후 Google 계정을 연결해주세요.",
  "code": "GOOGLE_LINK_REQUIRED",
  "linkToken": "example-link-proof",
  "expiresAt": "2026-10-23T12:05:00Z",
  "expiresInSeconds": 300,
  "loginGranted": false
}
```

- Access/Refresh 없음. `linkToken`은 Bearer가 아니며 임시 메모리에 보관한다.
- 사용자의 명시적 연결 선택 후 `POST /api/auth/google/link`에 `linkToken`, `currentPassword` 전송. 성공 시 `LOCAL_AND_GOOGLE`과 로그인 모델 반환.
- 오입력: `400 GOOGLE_LINK_PASSWORD_INVALID`, `field=currentPassword`. 현재 최대 5회.
- 만료/취소/소비/횟수 소진: `410 GOOGLE_LINK_EXPIRED`. Google 로그인부터 다시 진행.
- 충돌: `409 GOOGLE_ACCOUNT_ALREADY_LINKED` 또는 Google 검증 단계 `401 GOOGLE_TOKEN_INVALID`.
- 취소는 `POST /api/auth/google/link/cancel`. 유효 형식의 이미 없어진 증명도 200 message.
- 비밀번호를 모르면 기존 재설정 흐름 후 Google 연결을 새로 시작한다. 연결 응답 유실 시 같은 증명을 무한 재사용하지 않고 Google 로그인으로 상태 확인.
- 신규 Google 사용자에게 서비스 토큰은 발급되지만 완료 전 `profileCompleted=false`. API 전체의 서버 접근 제한은 아직 추가되지 않았다. 서버 제한 여부는 정책 협의 대상.
- 기존 Google 전용 계정의 subject가 비어 있을 때 검증 이메일로 1회 등록하는 호환 처리는 일반 계정 자동 연결과 다르다.

최종 입력은 `POST /api/users/me/profile/complete`이며 nickname 필수, 선택 조건은 없어도 된다.

```json
{"nickname":"찍먹사용자","allergies":"egg,wheat","specialDiet":"lactoOvoVegetarian,lowSugar,highProtein"}
```

이 API는 **전체 제출**이므로 생략된 선택 항목은 비운다. 성공은 `data`의 최신 Profile과 `profileCompleted=true`. PATCH/PUT은 완료 상태를 바꾸지 않는다. 완료 응답 유실은 GET me로 확인하며 동일 최종 프로필 재제출 후에도 완료 true다. 다중 기기에서 최신 값을 덮어쓰지 않도록 무조건 재제출하지 않는다.

## 4. 본인 프로필·부분 수정·카탈로그 (회신 5~6쪽)

```json
{
  "id": 17,
  "email": "user@example.com",
  "nickname": "찍먹사용자",
  "allergies": "egg,wheat",
  "diseases": null,
  "specialDiet": "lactoOvoVegetarian,lowSugar,highProtein",
  "dislikedIngredients": null,
  "role": "USER",
  "authProvider": "LOCAL",
  "profileCompleted": true,
  "hasPassword": true,
  "photoUrl": "/api/users/me/photo"
}
```

GET/PATCH/PUT/완료 응답의 `data` 모델이다. 사진이 없으면 `photoUrl=null`. 로그인은 `userId`, 프로필은 `id`로 이름이 다르다.

PATCH 허용: `nickname`, `allergies`, `diseases`, `specialDiet`, `dislikedIngredients`만.

| 전송 | 의미 |
| --- | --- |
| 필드 생략 | 기존 값 유지 |
| 선택 필드 `null` 또는 `""` | 전체 해제 |
| `{}` | 변경 없음, 최신 프로필 반환 |
| nickname null/빈 값 | 400 |
| allergies `[]` | 400, CSV 문자열/null만 허용 |
| profileCompleted 직접 전송 | 400 |

본인 nickname 유지/대소문자 변경은 허용하지만 공개 중복 조회는 available=false일 수 있다. 본인 프로필은 필드별 마지막 저장 우선이고 가족처럼 version이 없다. 응답 유실 시 GET me로 확인한다.

프론트 3단계: GET me로 세션 복원·본인 ID/이메일 확인, 마이페이지 표시 및 닉네임 PATCH 연결. 통신 실패는 세션/입력 보존, 수정 응답 유실은 최신 조회 후 사용자 재제출. `profileCompleted=false`는 홈 차단·안내까지이며 실제 추가 입력/완료와 가족·조건 편집 연결은 후속이다. 서버 계약 변경 없이 위 계약을 구현했다.

**PATCH 직렬화 주의:** 프론트 모델에서 미전송/명시적 null/실제 값을 구분한다. Gson의 기본 null 생략으로 ‘모두 해제’가 사라지거나, 전역 serializeNulls로 다른 필드까지 비워지는 일이 없어야 한다.

카탈로그는 `GET /api/products/classifications`(알레르기 26), `GET /api/products/filters`(채식 6·영양 6)를 따른다. **전체 JSON/정규 key 및 공개 조회 계약 확보·가입 매핑 완료.** [매핑표](../auth-signup-condition-mapping.md) 참조. low_sugar → lowSugar를 명시적으로 매핑하며 pine_nut은 그대로 사용한다. diseases/dislikedIngredients 미입력은 null이며 저당을 질환으로 변환하지 않는다.

## 5. 사진 (회신 6~7쪽)

- PUT multipart `file` 한 개. JSON/base64/photoUrl 전송 금지, boundary는 클라이언트 라이브러리가 생성.
- 현재 제한: 실제 JPEG/PNG, MIME 일치, 2MiB 이하, 최대 4096×4096. **제한은 합의 대기 값**. 서버는 재인코딩해 EXIF/GPS/뒤에 붙은 데이터 제거.
- PUT 성공: `data:{"photoUrl":"/api/users/me/photo","authenticationRequired":true}`.
- GET은 image/jpeg 또는 image/png 바이트, Bearer 필수, `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`.
- 사진 없음 `404 PHOTO_NOT_FOUND`, 형식/해상도 `400 PHOTO_INVALID`, 크기 초과 `413 PHOTO_TOO_LARGE`. 잘못된 교체는 기존 사진 보존.
- DELETE는 Body 없이 200 message, 이미 없으면 성공. 이후 GET 404, me.photoUrl null.
- 로그아웃/다른 기기 로그인으로 삭제하지 않는다. 서버 DB `profile_photos` 저장이며 객체 저장소/CDN 구현은 아님. 운영 백업·보존은 서버 담당 협의.

프론트는 신뢰한 API 원점의 상대 경로에만 인증을 붙인다. 전역 이미지 로더로 외부 상품 이미지까지 Bearer를 보내지 않는다. no-store를 존중하고 계정 전환 시 이전 사진/요청을 지운다. 같은 상대 URL이어도 계정마다 사진이 다르다. 관리자 응답에서 이 URL을 타인의 사진으로 해석하지 않는다.

**가입 화면과의 미결정:** 기존 이메일 가입은 JWT를 주지 않고 현재 사진 선택은 가입 전이다. 따라서 사진은 가입 성공만으로 업로드할 수 없다. 최초 로그인 후 업로드·재시도 또는 사진 설정 시점 변경을 사용자와 정한다. 임의 자동 로그인/비밀번호 영구 저장으로 해결하지 않는다.

## 6. 가족 (회신 7쪽)

생성 예시:

```json
{"nickname":"어머니","allergies":"soy","specialDiet":"lowSodium","diseases":null,"dislikedIngredients":null}
```

201 응답 예시:

```json
{"message":"가족 프로필 생성 성공","data":{"id":41,"nickname":"어머니","allergies":"soy","specialDiet":"lowSodium","diseases":null,"dislikedIngredients":null,"version":0}}
```

- 목록 data 배열, 단건 data 객체. 비밀번호/ownerId를 보내지 않고 Bearer로 소유자 제한.
- ‘나’는 GET me이며 가족으로 복제하지 않는다. 가족 이름의 전역 중복 검사는 없음. 코드상 가족 행 최대 20개(본인은 별도 me이므로 제외), 이름은 계정과 같은 NFKC·trim 후 2~20자 한글/영문/숫자/밑줄이다. 인원/이름 제약을 제품 정책으로 수용할지 결정한다.
- PATCH: 부분 수정 규칙 + 마지막 `version` 필수. DELETE: query version 필수.
- 타인/삭제된 ID: `404 FAMILY_NOT_FOUND`; 오래된 버전: `409 PROFILE_VERSION_CONFLICT`; 누락/잘못된 버전: 400.
- 충돌 시 GET 재조회 후 사용자 확인을 거쳐 최신 version 제출. 무조건 덮어쓰기/자동 재시도 금지.
- 생성 응답 유실은 목록 재조회로 확인. 생성 idempotency key가 없고 가족 이름 중복도 가능하므로 이름만으로 동일 생성이라고 단정하지 않는다.
- 삭제 유실 후 404는 이미 없는 상태로 처리. 선택했던 가족도 제거하고 유효한 분석 대상을 다시 계산.
- 소유자 없는 기존 로컬 가족은 자동 업로드·삭제하지 않는다. 선택 상태는 이번 서버 저장 범위에 없음.
- 프론트의 `emoji`/`relation`, UUID 문자열 ID와 서버 Long ID 사이의 연결·기기 동기화는 별도 협의. 회신 모델에 없는 필드를 임의 전송하지 않는다.

## 7. 상품·추천 분석 대상 (회신 7~9쪽)

적용되는 GET 6개: `/api/products/{barcode}`, `/api/products/{barcode}/variants`, `/api/products/search`, `/api/products/filter`, `/api/products/safe-recommendations`, `/api/recommendations/products/{barcode}`.

| Query | 의미 |
| --- | --- |
| analysisMode 생략 | 로그인 self, 공개 비로그인 조회 none |
| analysisMode=self | 본인만. includeSelf/familyProfileIds 보내지 않음 |
| analysisMode=selected | 선택 가족 + 선택적으로 본인 |
| includeSelf | selected에서만, 기본 false |
| familyProfileIds | selected에서만, Long 목록(쉼표 또는 반복 키), 최대 20명 |
| analysisMode=none | Bearer 유지, 개인 조건 누구에게도 적용하지 않음 |

selected인데 대상이 없으면 `400 ANALYSIS_SELECTION_INVALID`. 모두 해제는 명시적으로 none을 보낸다. self/selected는 로그인 필수이고 두 추천 API는 none도 로그인 필수다. 서비스 UI는 여전히 홈 진입 로그인 필수다.

```text
GET /api/products/search?keyword=두유&analysisMode=none
GET /api/products/8800000000000?analysisMode=selected&includeSelf=true&familyProfileIds=41,42
```

- 서버가 실제 가족 조건을 읽는다. 계정 식별은 Bearer, 가족 식별은 서버 가족 ID. GET Body로 임의 조건을 전송하지 않는다. 구형 allergies query가 본인 조건을 바꾸지 않는다는 백엔드 격리 테스트 기록이 있다. 가족 선택은 반드시 서버 가족 ID/analysisMode 계약으로 전환한다.
- 알레르기·기피 성분은 합집합, 식단·질환 제한은 모두 충족. 필수 조건은 후보 부족 때문에 완화하지 않는다.
- none: `analysis.status=NOT_EVALUATED`, `analysis.analysisApplied=false`, `analysis.selection.mode=none`. `isDangerous=false`만 보고 안전 표시 금지.
- 상품의 기존 analysis.status는 알레르기 판정. 새 `analysis.dietConstraints`는 식단 판정이며 PASS라도 식단 MISMATCH/UNKNOWN일 수 있다. 전체 DTO/enum/사유는 확보한 전체 명세·v2를 기준으로 프론트 모델에 대조·반영한다.
- 추천 대상 정보 `data.analysisSelection`. none은 홈 recommendationMode/대체 resultMode GENERAL, safety/dietConstraints NOT_EVALUATED, 대체 recommendationTier GENERAL. ‘맞춤 안심 추천’으로 표시하지 않는다.
- 가족/none에서는 본인 행동 이력으로 개인화하지 않는다. 단건 자동 SCAN은 self에서만. 기존 POST histories/events도 가족/none을 본인 선호로 보내지 않는다.
- 계정·선택 대상·조건·카테고리 변경 시 cursor와 이전 요청 결과를 버리고 첫 페이지 조회. 구형 cursor 재사용 금지, excludeBarcodes 중복 제외는 유지.
- 같은 용량의 여러 행은 알레르기·식단 모두 보수적으로 합침. 충돌을 대표 행만 보고 무시하지 않는다.
- 최신 `/api/chat`은 같은 분석 대상 query와 공통 판정을 사용한다. [챗봇 계약](backend/api-spec/chat-analysis-contract-v1.md) 확보 완료. 별도 AI의 실제 수신·헤더 확인 및 공동 배포는 후속이며 계약만으로 전체 연동 완료는 아니다.
- 이것은 복수 **사람**에 대한 판정 계약이다. 여러 **상품**의 비교 레포트/순위 API가 확보됐다는 뜻은 아니다.

## 8. 오류·응답 유실·실서버 검증

공통 선택 필드: field/retryAfterSeconds/remainingAttempts. OTP 오입력 remainingAttempts=0이면 재발송, 없는 경우 횟수 추측 금지. 재전송 대기 Retry-After도 제공. 이메일 시간은 offset 포함, Access/Refresh/연결 시각은 Z로 전달한다.

| 응답 유실 요청 | 회신의 복구 방향 |
| --- | --- |
| 인증 메일 발송 | 실패 단정 금지. 도착한 코드/서버 제한 확인, 성공 전 타이머 초기화 금지 |
| OTP 검증 | 소비됐다면 OTP_NOT_REQUESTED 가능. 증명이 없으면 재발송·재검증 |
| 가입 | 입력 보존 후 로그인으로 확인. 중복 이메일을 단순 실패로 해석하지 않음 |
| 비밀번호 변경/재설정 | 성공 단정·자동 재변경 금지. 세션 무효면 새 비밀번호 로그인/재설정으로 복구 |
| Google 연결 | Google 로그인부터 다시 하여 연결 상태 확인 |
| Refresh | 같은 토큰 재사용 금지, 재로그인 |
| 프로필 PATCH/완료 | GET me로 최신값·완료 확인 |
| 가족 생성/수정/삭제 | 목록/단건 재조회, version 갱신 및 사용자 확인 |
| 사진 변경 | GET me/photo로 최종 상태 확인 |

백엔드 보고: 2026-10-23 테스트 144개 중 140개 통과, 실패 0, 조건부 CSV 4개 제외. H2/모의 외부 인증 기반이며 운영 MySQL·실제 Google·메일 검증은 아니다. V14 파일 추가는 실제 DB 적용 완료를 뜻하지 않는다.

실서버 검증 시작 조건: 배포 담당자의 버전·V14 적용 확인, 정확한 주소, 테스트 계정, 실제 메일/Google 설정. 두 기기 로그인·회전·기기별 로그아웃·비밀번호 전체 폐기·가족 충돌·사진 격리를 단계적으로 검증한다. 백엔드 배포/DB 적용은 프론트 작업에서 실행하지 않는다.


## 9. 이번 갱신에서 확인한 범위

AuthController/UserController/FamilyProfiles/ProfilePhotos/ProfileInputs/AccountInputPolicy/UserPreferenceCatalog 및 JwtAuthenticationFilter/AuthSessions의 관련 경로·검증을 읽었다. 가족 relation/emoji는 허용 필드에 없고 추가하면 현재 400이다. 가족 중복 이름은 허용되지만 이름 규칙은 계정과 동일하다.

만료 Access는 JWT 필터가 logout 전에 거절한다. 만료 시 서버 세션 폐기 대안 및 Refresh의 오류별 안전 재시도 정책은 여전히 협의 대상이다. 사진·가족 API는 실제 구현이 있지만 원격 배포·SMTP·Google·DB는 확인하지 않았다.
