# 상품 추천과 인증 통합 기준

## 현재 앱 기준 — PR #24 통합

- 기본 주소는 공통 `BackendServer.BASE_URL`의 기관 HTTPS를 사용한다. debug 전용 localhost 테스트 주소 설정은 유지한다.
- `AccessTokenStore`는 별도 토큰을 저장하지 않고 `AuthSessionManager`의 현재 유효한 세션을 읽는다. 로그인·갱신·로그아웃·계정 변경은 인증 모듈이 소유한다. 로그인 API는 별도 인증 클라이언트를 사용해 이전 JWT를 자동 첨부하지 않는다.
- 상품 추천/상세 진입 시 서버 프로필을 자동 PUT하지 않는다. 가입·프로필 수정에서 저장한 서버 조건을 그대로 조회에 사용한다. 기기 로컬의 빈 조건이나 다른 계정 조건으로 덮어쓰는 경로를 제거했다.
- 상품 상세 디자인·용량 선택·대체 상품·추천 페이지네이션은 #23 구현을 유지한다. 가족/none 대상 계약과 로컬 가족 데이터의 계정별 정리는 후속 작업이다.
- 현재 검증은 병합 PR의 테스트 기록을 따른다. 아래는 #23 개발 당시 기록으로, 토큰 공급/자동 조건 저장 절차를 현재 앱에 다시 적용하지 않는다.

## 이전 #23 개발 기록 (과거 기준)

# 나 기준 추천: 로컬 테스트와 인증 연결

## 데이터 흐름

앱 Room의 `FamilyProfile.isMe == true` 한 명만 사용한다. 가족 선택은 추천 기준에 영향을 주지 않는다.

1. `GET /api/users/me`로 현재 서버 사용자 확인.
2. `PUT /api/users/me`로 앱에 저장된 나의 알레르기를 `allergies`, 채식 유형+선호 조건을 `specialDiet`에 저장.
   기존 서버 닉네임, 기저질환, 기피 식재료는 유지한다. 프로필 저장 실패 시 추천 요청을 하지 않는다.
3. `GET /api/products/safe-recommendations?limit=6` 호출.
4. `data.returnedBarcodes` 순서대로 `data.categories[].items[].product`를 중복 제거해 표시.
5. `hasNext`, `nextCursor`로 다음 페이지 요청. 이미 표시한 바코드는 `excludeBarcodes`로 전달한다.

앱 프로필은 아직 기기 로컬 데이터다. 새 설치는 빈 나 프로필만 생성하며, 예전 설치에 저장된 샘플 조건은 자동으로 삭제하지 않는다. 마이페이지에서 실제 조건으로 수정한 뒤 테스트한다. 이 작업은 회원가입 입력을 저장하는 인증 기능을 새로 구현하지 않는다.

추천은 백엔드의 실제 추천 로직을 사용한다. 서버는 알레르기 안전 조건을 유지하면서 선호 조건을 완화할 수 있으며, 앱은 완화 안내를 표시한다. 조회한 개수는 전체 DB의 총 상품 수가 아니다.

## 로컬 서버

운영 DB 대신 로컬 개발 DB를 준비하고 기존 방식대로 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`을 설정한다. 토큰 없는 테스트여도 기존 JWT 컴포넌트 생성에 `JWT_SECRET` 설정은 필요하다. 실제 토큰을 발급받을 필요는 없다.

로컬 DB의 테스트 사용자 ID를 `LOCAL_RECOMMENDATION_USER_ID`로 지정한다. 이미 존재하는 사용자여야 하며, 코드가 사용자·상품을 자동 생성하거나 운영 데이터를 변경하지 않는다.

```sh
export LOCAL_RECOMMENDATION_USER_ID=<로컬_DB의_테스트_사용자_ID>
bash gradlew bootRun --args='--spring.profiles.active=local-recommendations'
```

- 서버는 기본 `127.0.0.1:8080`에 바인딩된다.
- `local-recommendations` 프로필을 명시한 경우만 적용된다. `prod` 또는 `production` 프로필과 함께 실행하면 비활성화된다.
- Authorization 헤더가 없는 `GET /api/products/safe-recommendations`, `GET/PUT /api/users/me`만 지정 사용자로 처리한다.
- 다른 사용자 ID 선택이나 관리자 권한을 허용하지 않는다. 토큰이 있으면 정상 JWT 인증을 사용하고 잘못된 토큰은 401을 반환한다.
- 다른 계정·삭제·비밀번호 변경 API에는 테스트 인증을 적용하지 않는다.

## Android 서버 연결

디버그 앱에서만 앱 전용 `noBackupFilesDir/backend.properties`의 `baseUrl`을 읽는다. 토큰이나 비밀키를 이 파일에 쓰지 않는다.

에뮬레이터 또는 USB 연결 폰에서 다음처럼 포트를 연결한다. 아래 `adb`는 Android SDK의 platform-tools 경로를 사용해도 된다.

```sh
adb reverse tcp:8080 tcp:8080
adb shell run-as com.coworker.jjikmuk mkdir -p no_backup
printf 'baseUrl=http://127.0.0.1:8080/\n' | adb shell run-as com.coworker.jjikmuk sh -c '"cat > no_backup/backend.properties"'
```

앱을 완전히 종료하고 다시 실행하면 서버 설정이 적용된다. 로컬 설정을 제거하고 다시 실행하면 기존 원격 서버로 돌아간다.

```sh
adb shell run-as com.coworker.jjikmuk rm no_backup/backend.properties
adb reverse --remove tcp:8080
```

릴리스 앱은 해당 파일을 읽지 않는다. 디버그 허용 주소는 `http://127.0.0.1:8080/`, `http://10.0.2.2:8080/` 두 개다.

## 팀원의 인증 구현 연결점

Hilt로 동일 `AccessTokenStore` 인스턴스를 주입하고 로그인 성공 시 다음을 호출한다.

```kotlin
accessTokenStore.setAccessToken(response.accessToken) // Bearer 접두사 없는 실제 토큰
```

`BackendAuthInterceptor`가 백엔드 요청에 Bearer 헤더를 붙인다. 토큰 변경 시 추천을 초기화하고 다시 불러온다. AI/OCR/이미지 클라이언트에 토큰을 추가하지 않는다. 백엔드 클라이언트는 리다이렉트로 인증 헤더를 다른 호스트에 전달하지 않는다.

로그아웃은 `accessTokenStore.clear()`를 호출한다. 저장소는 메모리 전용이므로 앱 재시작 시 인증 담당자가 기존 영속 세션에서 복원해야 한다. 자동 토큰 갱신·로그인·발급은 이번 구현 범위에 포함하지 않는다.

앱의 나 프로필을 요청 전에 서버에 동기화하므로, 계정 전환 기능을 추가할 때는 **해당 계정의 나 프로필을 로컬에 복원한 다음 토큰을 공급**해야 한다. 이전 계정의 로컬 조건을 새 계정에 저장하지 않도록 인증 담당자가 이 순서를 지켜야 한다. 서버 주소를 원격으로 되돌리고 로컬 인증 프로필을 끄면 추천 화면·요청·응답 코드는 그대로 사용한다.

## 검증

- Android MockWebServer: 내 조건 저장, 기존 서버 필드 보존, 토큰 없음/있음/제거, 커서와 바코드 순서·중복, 인증 실패, 잘못된 페이지 응답.
- 백엔드 실제 HTTP + 격리 H2 DB: 토큰 없는 내 프로필 저장 후 우유 알레르기 상품 제외, 다른 사용자 선택 차단, 잘못된 토큰 차단, 유효한 JWT가 로컬 사용자보다 우선함.
- 기본 프로필 HTTP 테스트: 로컬 설정 없는 익명 추천은 401 유지.
- 기존 추천 서비스 테스트: 알레르기 안전 조건, 선호 완화, 커서 중복 방지.

실제 앱에 저장된 개인 데이터와 개발 MySQL을 사용한 종단 간 테스트는 개발 DB 연결 설정 및 로컬 사용자 ID를 준비한 후 실행한다. 격리 테스트의 프로필·상품은 테스트 픽스처이며 실제 개인 데이터나 운영 상품 검증을 대체하지 않는다.

## 현재 Mac의 실행 준비 상태 (2026-10-23)

- 전용 MySQL: `127.0.0.1:3307`, DB `jjikmuk_local`. 기존 3306 MySQL은 변경하지 않았다.
- 테스트 사용자: ID 1, 닉네임 `로컬테스트`. 앱의 나 조건을 기존 `/api/users/me` API로 저장한다.
- 상품: `Product.csv` 1,348,436행을 검증하고 중복 바코드 11건을 제외한 1,348,425건 적재.
- 추천 후보: `ProductNeighbors.csv` 233,658행 적재.
- 원본 CSV는 Downloads에 보존하고 DB에 전체 적재했다. API 서버 재실행 때 CSV를 다시 읽지 않는다.
- 이 로컬 전용 DB의 스키마는 Hibernate와 저장소의 추가 테이블 SQL로 초기화했고, 로컬 설정에서 Flyway는 비활성화했다. 운영 DB 설정으로 재사용하지 않는다.
- 런타임 DB 파일·접속 설정·로그는 `/private/tmp/jjikmuk-local-db/`에 있으며 로컬 테스트용이다. 임시 폴더가 제거되면 재구축이 필요하다.
- 서버를 종료한 뒤 다시 실행하려면 `bash /private/tmp/jjikmuk-local-db/start-backend.sh`를 실행한다.
- 에뮬레이터 앱에는 최신 APK와 `http://127.0.0.1:8080/` 개발 설정을 적용했다. 에뮬레이터를 다시 켜면 `adb reverse tcp:8080 tcp:8080`으로 연결한다.
- 실제 에뮬레이터에서 내 조건 저장과 추천 상품 표시를 확인했다. CSV에 이미지 URL이 없는 상품은 이미지 준비중으로 표시된다.
