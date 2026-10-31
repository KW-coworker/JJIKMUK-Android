# Google 로그인 연결 및 설정 요청

> 2026-10-31 · `feature/auth-contract-models`. **SDK·화면·API 연결 구현 완료, 실제 Google 인증은 설정 확보 후 검증.** 사용자는 Web OAuth Client ID와 Android 등록 정보가 아직 없으며 구현·요청 항목 정리를 먼저 진행하도록 요청했다. 이 문서는 요청 초안이며 백엔드에 전송하지 않았다.

## 구현한 흐름

1. 로그인 화면의 Google 버튼 → Android Credential Manager의 명시적 Google 계정 선택 → ID token을 `POST /api/auth/google`에 전송한다.
2. 로그인 성공이면 서비스 토큰 쌍을 저장하고 기존 `GET /api/users/me` 시작 확인으로 이동한다. `profileCompleted=false`는 닉네임·선택적 조건 입력을 완료해야 홈에 들어간다.
3. `GOOGLE_LINK_REQUIRED`이면 로그인하지 않고 **기존 계정 연결** 화면에서 비밀번호를 받는다. 명시적으로 연결 버튼을 눌러야 `POST /api/auth/google/link`를 호출한다. 기존 계정 비밀번호에는 신규 비밀번호 생성 규칙을 강제하지 않는다.
4. 연결 성공은 안내 후 ‘계속하기’ → 시작 확인으로 이동한다. 화면 재생성으로 연결 안내가 사라지거나 다시 Google 선택기를 열지 않는다.
5. 취소/뒤로가기/비밀번호 찾기는 연결 증명을 버리고 `/api/auth/google/link/cancel`을 한 번 호출한다. 비밀번호 찾기는 기존 재설정 화면을 재사용하며 이후 Google 로그인을 새로 시작한다.

기존 버튼·비밀번호 입력·색상/글꼴 토큰을 재사용했다. 새 연결 화면의 최종 디자인은 사용자 검토 대상이다. 이메일 로그인과 debug 홈·스캐너 진입은 유지한다.

## 오류·보관 규칙

- ID token과 linkToken은 서비스 Bearer가 아니다. 비밀번호와 함께 메모리에서만 처리하며 저장 상태·디스크·로그에 남기지 않는다. 서비스 Access/Refresh만 기존 암호화 세션 저장소에 보관한다.
- SDK 선택 취소/설정 누락은 서버를 호출하지 않는다. 설정이 없어도 빌드하고 다른 화면을 테스트할 수 있다.
- 연결 시간은 서버의 절대 만료와 TTL 중 더 이른 값으로 정하고, 이후 단조 시계로 센다. 남은 횟수는 서버 응답 값만 표시한다. 5분/5회는 현재 서버 계약이며 제품 정책 최종 수용 여부는 기존 검토 문서에서 별도로 관리한다.
- 명확한 `400 GOOGLE_LINK_PASSWORD_INVALID`만 유효 증명·입력을 유지한다. 만료/횟수 소진은 비밀번호·증명을 정리하고 Google 로그인부터 다시 시작한다.
- 연결 응답 유실·서버 오류는 같은 증명으로 자동 재전송하지 않는다. 서버에서 연결됐을 수도 있으므로 새 Google 로그인으로 상태를 확인한다. 취소 요청 실패도 ‘서버 취소 완료’로 표시하지 않는다. **연결 완료 뒤 도착한 취소는 이미 성립한 계정 연결을 되돌리는 기능이 아니다.**
- 연타, 취소 후 늦은 결과, 계정 변경 뒤 도착한 SDK/서버 응답을 차단한다. 화면 회전은 ViewModel의 진행 상태를 유지하고, 프로세스 종료 후에는 새 Google 로그인으로 재개한다.
- 로그아웃 시 서비스 세션 정리와 함께 Credential Manager 상태를 해제한다. provider가 응답하지 않아도 로컬 로그아웃은 유지한다. Google 계정 자체를 기기에서 삭제하는 기능은 아니다.

## 확보할 설정

| 항목 | 필요한 내용 | 담당/확인점 |
| --- | --- | --- |
| Web OAuth Client ID | `…apps.googleusercontent.com` | Android 타입 Client ID가 아닌 **Web/server 타입**. 백엔드 ID token 검증 audience와 일치해야 함 |
| Android OAuth 등록 | 패키지 `com.coworker.jjikmuk` + 해당 빌드의 서명 SHA-1 | 같은 Google Cloud 프로젝트에 등록. 팀원별 debug 키와 release/Play App Signing 키를 구분 |
| Google 동의 화면 | 테스트/게시 상태, 사용할 테스트 Google 계정 | 테스트 상태라면 해당 계정 허용 여부 확인 |
| 백엔드 실행 환경 | 실제 HTTPS 주소·실행 커밋·Google 허용 Client ID 설정 여부 | 저장소에 코드가 있는 것과 배포 완료는 다름. Client secret/토큰을 전달하거나 Git에 저장하지 않음 |
| 시험 계정 | 신규 Google, 기존 연결 Google, 동일 이메일 일반 계정 | 실제 계정 연결은 명시적으로 승인된 시험 계정 사용 |

SHA-1은 이 작업 폴더에서 `./gradlew :app:signingReport`로 확인한다. 다른 팀원의 debug 서명이나 release 서명을 임의로 재사용하지 않는다. Google Play 서비스가 지원되는 AVD/실기기와 Google 계정이 필요하다.

### Android에 Client ID 적용

빌드 설정은 로컬 Gradle 속성 `jjikmuk.googleWebClientId`를 읽는다. 예를 들어 개인 `~/.gradle/gradle.properties` 또는 빌드 옵션 `-Pjjikmuk.googleWebClientId=받은_Web_Client_ID`로 전달한다. 기본값은 빈 값이며, 개인 설정을 저장소에 추가하지 않는다.

debug에서는 기존 앱 내부 `no_backup/auth-api.properties`에 `googleWebClientId=받은_Web_Client_ID`를 추가할 수도 있다. **기존 `baseUrl` 등 설정을 보존하여 병합**한다. debug 파일 값이 빌드 설정보다 우선하며 release는 이 파일을 읽지 않는다. Client ID는 식별자지만 OAuth Client secret, 계정 비밀번호, 서비스/Google 토큰은 설정 문서나 커밋에 넣지 않는다.

Client ID만 넣는다고 서버까지 연결되는 것은 아니다. 기존 인증 API `baseUrl` 설정과 접근 가능 여부도 필요하며, 현재 release 인증 서버 설정은 여전히 미설정이다. 다른 상품/OCR 서버 주소를 추정해서 넣지 않는다.

## 백엔드·Google 프로젝트 담당자에게 보낼 내용

> Android Google 로그인, 동일 이메일 기존 계정의 명시적 연결·취소·비밀번호 찾기 흐름을 구현했습니다. 실연결 시험을 위해 다음을 부탁드립니다.
>
> 1. Android가 ID token 발급에 사용할 **Web OAuth Client ID**와 백엔드 audience 일치 여부.
> 2. 같은 프로젝트의 Android OAuth 등록 확인: `com.coworker.jjikmuk` 및 개발자 debug/배포 서명 SHA-1. 실제 SHA-1은 해당 빌드에서 추출해 전달하겠습니다.
> 3. OAuth 동의 화면의 테스트/게시 상태와 테스트 계정 허용 여부.
> 4. 현재 개발 인증 서버의 배포 커밋, Google 설정 반영 여부 및 접근 환경.
> 5. 신규/기존 Google 로그인 및 같은 이메일 일반 계정 연결을 시험할 계정 준비 방법.
>
> Client secret이나 실제 사용자 토큰은 Android에 전달할 필요가 없습니다. Refresh 안전 재시도·Access 만료 시 logout 대안 및 기존 미결정 정책은 별도 회신 목록에 유지합니다.

## 실제 설정을 받은 뒤 확인할 순서

1. Google 선택기 열기·취소·계정 재선택. 잘못된 Client ID/서명 설정은 로그인 성공으로 처리되지 않아야 한다.
2. 신규 Google → 추가 프로필 → 조건 없이 완료 → GET me → 홈.
3. 기존 Google → GET me → 홈. 로그아웃 후 재선택과 앱 재시작도 확인.
4. 동일 이메일 일반 계정 → 연결 전 홈 차단 → 오답/정답/만료/횟수 소진/취소/비밀번호 찾기.
5. 연결 응답 유실·취소와 서버 응답 경합·두 계정 전환. 연결된 계정의 비밀번호 변경 메뉴가 올바른지 확인.

모의 SDK/저장소/서버 테스트는 실제 Google 프로젝트 설정, ID token audience 검증 또는 서버 배포 검증을 대신하지 않는다.

## 이번 검증 결과

단위 테스트 220개, New_Device 인증·보안·프로필·Google 화면 테스트 22개 통과. debug APK 및 release Kotlin 컴파일 성공. Google 화면 4개는 SDK 대역으로 연결 동의/오답/취소/비밀번호 찾기/재진입/설정 누락을 검증했다. 실제 Google 인증·프로젝트 설정은 아직 검증하지 않았다.

## 구현 근거

- [Android Credential Manager의 Google 로그인 구현](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation): 명시적 버튼의 `GetSignInWithGoogleOption`, Web Client ID와 로그아웃 처리.
- [AndroidX Credentials 릴리스](https://developer.android.com/jetpack/androidx/releases/credentials): 안정 버전 `1.6.0` 사용.
- [Google ID 라이브러리 릴리스](https://developers.google.com/identity/android-credential-manager/releases): `googleid:1.2.1` 사용.
- API 계약 우선순위는 [로컬 명세 시작점](API/README.md), 연결 세부 계약은 [백엔드 통합 명세 v2](API/backend/api-spec/frontend-auth-integration-v2.md)를 따른다.
