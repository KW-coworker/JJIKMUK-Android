# 인증 실서버 검증과 백엔드 확인 요청 — 2026-10-31

## 현재 결론

기관 서버의 HTTPS와 일부 API는 정상이나, **이메일 발송 및 인증 v2 일부 경로에서 서버 오류/명세 차이가 확인되어 가입부터 끝까지 검증하지 못했다.** 프론트 연결 코드 구현 완료와 운영 서버에서 동작 확인을 구분한다. 아래 오류가 해결되기 전에는 로그인·회원가입 전체 완료로 처리하지 않는다.

## 재현 결과

주소는 모두 `https://43.203.42.36/`다. HTTPS 인증서 검증을 켠 상태에서 확인했다. 로그인 토큰을 붙이지 않는 공개 인증 API는 명세대로 Authorization 없이 호출했다.

| 요청 | 실제 결과 | 해석·다음 확인 |
| --- | --- | --- |
| POST /api/auth/email/send, 본인 테스트 이메일 + purpose=signup | 500 INTERNAL_SERVER_ERROR | 앱에서 실패, 동일 소문자 purpose 요청으로 직접 재현. SMTP/메일 설정 문제인지 다른 예외인지는 서버 로그 확인 필요 |
| GET /api/auth/nicknames/availability, 유효 닉네임 | 200, 앱 Repository Boolean 변환 성공 | 조회 정상. 닉네임 예약이나 가입은 수행하지 않음 |
| 같은 닉네임 조회, nickname=! | 400 NICKNAME_INVALID, field=nickname | 앱의 입력 오류 해석 정상 |
| GET /api/users/me 및 /photo, 유효하지 않은 Bearer | 401 AUTH_TOKEN_INVALID | 앱 오류 변환 정상. 실제 회원/사진 조회 성공은 미검증 |
| POST /api/auth/refresh, 유효하지 않은 테스트 Refresh | 401 AUTHENTICATION_REQUIRED | v2 예상 REFRESH_TOKEN_INVALID와 다름. 인증 필터·경로 매핑·배포 버전 확인 필요 |
| POST /api/auth/google, 빈 idToken | 503 GOOGLE_LOGIN_UNAVAILABLE | 명세상 서버 Client ID 미설정 시 코드. 실제 설정 상태를 백엔드에서 확인 |
| POST /api/auth/google/link, 빈 linkToken/currentPassword | 401 AUTHENTICATION_REQUIRED | 명세상 Link Token으로 진행하는 공개 경로. 필터·배포 확인 필요 |
| POST /api/auth/google/link/cancel, 빈 linkToken | 401 AUTHENTICATION_REQUIRED | 명세상 Link Token으로 취소하는 공개 경로. 필터·배포 확인 필요 |
| POST /api/auth/login, 빈 email/password | 401 LOGIN_FAILED | 입력값에 대한 오류 응답만 확인. 실제 로그인 성공 검증은 아님 |

발송 재현 응답의 서버 Date: **2026-10-31 11:29:15 GMT / 20:29:15 KST**. JSON은 아래와 같다. 실제 테스트 이메일은 로컬 인수인계에만 기록하고 공유 문서에는 넣지 않는다.

```json
{"status":500,"code":"INTERNAL_SERVER_ERROR","message":"서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."}
```

갱신/연결 경로의 응답만으로 API가 없거나 모든 유효 요청이 실패한다고 단정하지 않는다. 다만 명세와 배포의 불일치가 있으며 Access 만료 이후의 정상 갱신을 보장할 수 없다. 프론트에서 Access를 임의로 추가해 우회하지 않았다.

## 이번 프론트 보완

- 인증 서버 5xx와 일반 통신 결과 불명 안내를 구분했다. 발송 성공으로 처리하거나 OTP 단계로 넘어가지 않는다.
- 이메일 가입·비밀번호 찾기 화면에 재요청 대기 초를 기존 버튼에 표시한다. 버튼과 키보드 제출 모두 대기 중 요청을 막는다. 위치·색상·컴포넌트는 유지한다.
- 60초가 지나도 자동으로 인증번호를 요청하지 않는다. 사용자가 다시 눌러야 요청한다. 기존 결과 불명·증명 폐기 규칙 유지.
- Google 서버 이용 불가와 잘못된 Google 인증 정보의 안내를 추가했다. 자동 반복·임의 세션 생성 없음.
- 실제 HttpAuthRepository로 호출하는 선택 실행형 계약 테스트를 추가했다. 명세와 다른 Refresh 응답은 실패로 남기며 성공으로 완화하지 않았다.

## 백엔드에 보낼 요청 초안

> 기관 HTTPS 주소로 인증 연동을 검증했습니다. 아래를 확인 부탁드립니다.
>
> 1. 10월 31일 20:29:15 KST의 POST /api/auth/email/send 요청이 purpose=signup에서 500 INTERNAL_SERVER_ERROR를 반환했습니다. 동일 요청은 Android에서도 실패합니다. 서버 로그로 원인을 확인하고 기관 서버 SMTP/메일 발송 설정도 점검 부탁드립니다. 테스트 이메일은 필요하면 별도로 전달하겠습니다.
> 2. v2 명세상 Authorization 없이 Refresh Token/Link Token으로 호출하는 /api/auth/refresh, /api/auth/google/link, /api/auth/google/link/cancel에서 AUTHENTICATION_REQUIRED가 반환됩니다. 실제 배포 커밋, 컨트롤러 경로와 공개 경로 허용 설정을 확인 부탁드립니다. Access가 만료된 상태에서도 Refresh 요청은 가능해야 합니다.
> 3. /api/auth/google은 GOOGLE_LOGIN_UNAVAILABLE를 반환합니다. 서버 Google Client ID/audience 설정 여부와 Android용 Web OAuth Client ID, 같은 프로젝트의 Android 패키지·서명 등록을 확인 부탁드립니다.
> 4. 기존 질문인 Refresh 실패별 안전 재시도 조건, 만료 Access의 서버 로그아웃 절차, Google 미완료 계정 허용 경로, OTP 타이머 기준도 함께 확인 부탁드립니다.
>
> 프론트는 Access 15분·최초 로그인부터 Refresh 30일 고정, 갱신 응답 유실 시 재로그인 정책을 유지합니다. 서버 오류를 우회하거나 처리 요청을 자동 재전송하지 않습니다.

이 문서는 전달 초안이며 백엔드에게 메시지를 보내지 않았다.

## 서버 수정 후 재개 순서

1. 아래 계약 테스트 재실행. Refresh 응답 차이가 해결됐는지 확인한다.
2. 테스트 이메일로 발송 → 사용자 직접 OTP 입력 → 비밀번호/닉네임/조건 → 가입 완료 → 로그인 → GET me → 홈 확인.
3. 앱 종료/재실행, Access 만료 후 갱신, 로그아웃, 네트워크 실패 재시도 확인. 서버 동작 검증을 위해 앱의 기기 시각을 임의 변경하지 않는다.
4. 가입 사진을 서버 저장하고 같은 계정 다른 기기의 복원, 다른 계정의 분리 확인. 임시 파일만 존재하는 상태와 서버 저장 성공을 구분한다.
5. Google 설정 확보 후 신규·기존·명시적 연결/취소·미완료 프로필 완료 흐름 검증.

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.coworker.jjikmuk.feature.auth.InstitutionAuthContractTest \
  -Pandroid.testInstrumentationRunnerArguments.liveBackend=true
```

이 테스트는 이메일 발송/계정 생성/정상 자격증명 사용을 하지 않는다. 일반 테스트 실행에서는 opt-in이 없으면 건너뛴다. 실서버 테스트 실행에는 네트워크와 AVD가 필요하다. 테스트 실행이 앱 데이터를 제거할 수 있으므로 실제 가입 중간이나 보관할 세션이 있는 기기에서 실행하지 않는다.

## 이번 검증 결과

- 단위 테스트 **255개 통과**, 이번 변경 관련 에뮬레이터 UI 테스트 **6개 통과**, release Kotlin 컴파일 통과.
- 별도 실서버 계약 테스트 **4개 중 3개 통과·1개 실패**. 실패는 Refresh 오류 코드가 v2 계약과 다르기 때문이다. 해당 opt-in 테스트는 기대 계약을 그대로 유지했다.
- 서버 수정 전 실제 가입/로그인/토큰 갱신/사진 복원까지 성공했다고 간주하지 않는다.
