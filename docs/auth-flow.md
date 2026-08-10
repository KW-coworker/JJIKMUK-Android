# 로그인 및 회원가입 화면 흐름

## 1. 문서 목적

이 문서는 로그인, 비밀번호 찾기, 회원가입 화면의 이동 규칙과 화면 간에 보존해야 할 상태를 정의한다.

현재 구현 범위는 UI, 입력 검증, 화면 이동이며 실제 인증 API 연결과 토큰 저장은 포함하지 않는다. API 연결 전까지 성공 및 오류 상태는 UI 확인을 위한 임시 검증 규칙으로 재현한다.

## 2. 화면과 UI 상태 구분

Figma에 정의된 미입력, 입력 중, 입력 완료, 오류 화면은 각각 별도의 Navigation route로 만들지 않는다. 하나의 화면에서 입력값과 오류 상태에 따라 UI를 변경한다.

예를 들어 회원가입 이메일 화면은 `SignUpEmail` route 하나를 사용한다.

- 이메일 미입력: 입력값이 비어 있고 다음 버튼이 비활성화된 상태
- 이메일 입력: 입력값이 있고 다음 버튼이 활성화된 상태
- 잘못된 이메일: 이메일 오류 문구가 표시된 상태

OTP, 비밀번호, 닉네임, 맞춤 조건, 채식 식단, 알레르기 화면도 같은 원칙을 적용한다.

## 3. Route 목록

| 영역 | Route | 역할 |
| --- | --- | --- |
| 시작 | `Splash` | 앱 시작 화면 |
| 시작 | `AuthChoice` | 로그인 또는 회원가입 선택 |
| 로그인 | `Login` | 이메일과 비밀번호 입력 |
| 비밀번호 찾기 | `PasswordResetEmail` | 가입 이메일 입력 및 코드 전송 |
| 비밀번호 찾기 | `PasswordResetOtp` | OTP 입력 및 인증 |
| 비밀번호 찾기 | `PasswordResetNewPassword` | 새 비밀번호 입력 |
| 비밀번호 찾기 | `PasswordResetComplete` | 비밀번호 변경 완료 |
| 회원가입 | `SignUpEmail` | 가입 이메일 입력 및 코드 전송 |
| 회원가입 | `SignUpOtp` | OTP 입력 및 인증 |
| 회원가입 | `SignUpPassword` | 비밀번호 생성 |
| 회원가입 | `SignUpNickname` | 닉네임 생성 |
| 회원가입 | `SignUpConditions` | 맞춤 조건 선택 |
| 회원가입 | `SignUpVegetarian` | 채식 식단 선택 |
| 회원가입 | `SignUpAllergies` | 알레르기 항목 선택 |
| 회원가입 | `SignUpProfile` | 가입 정보 최종 확인 |
| 회원가입 | `SignUpComplete` | 회원가입 완료 |
| 앱 | `Home` | 로그인 성공 후 진입하는 기존 홈 |

## 4. 전체 화면 이동

### 4.1 시작 및 로그인

```text
Splash
└─ 일정 시간 후 → AuthChoice

AuthChoice
├─ 로그인 → Login
└─ 회원가입 → SignUpEmail

Login
├─ 비밀번호 찾기 → PasswordResetEmail
├─ 회원가입하기 → SignUpEmail
├─ 로그인 성공 → Home
└─ 뒤로가기 → AuthChoice
```

`Splash`에서 `AuthChoice`로 이동할 때 `Splash`는 back stack에서 제거한다. `AuthChoice` 화면에는 UI 뒤로가기 버튼이 없으며 시스템 뒤로가기는 앱 종료라는 Android 기본 동작을 따른다.

로그인은 API 연결 전까지 유효한 형식의 입력값이면 성공한 것으로 간주하고 `Home`으로 이동한다. 이는 UI 개발용 임시 정책이며 API 연결 시 교체한다.

### 4.2 비밀번호 찾기

```text
PasswordResetEmail
├─ 코드 전송하기 → PasswordResetOtp
└─ 뒤로가기 → Login

PasswordResetOtp
├─ 인증 성공 → PasswordResetNewPassword
└─ 뒤로가기 → PasswordResetEmail

PasswordResetNewPassword
├─ 완료 → PasswordResetComplete
└─ 뒤로가기 → PasswordResetEmail

PasswordResetComplete
└─ 로그인하기 → Login
```

OTP 인증이 완료된 후에는 OTP 화면으로 다시 돌아갈 수 없다. `PasswordResetOtp`에서 `PasswordResetNewPassword`로 이동할 때 인증된 OTP 화면을 back stack에서 제거한다.

새 비밀번호 화면에서 뒤로가기를 실행하면 이메일을 다시 입력하고 새로운 OTP 인증을 시작해야 한다. 이때 기존 OTP 값과 OTP 오류 상태는 초기화한다.

`PasswordResetComplete`에는 UI 뒤로가기 버튼이 없다. 로그인하기를 누르면 비밀번호 찾기 화면 전체를 back stack에서 제거하고 `Login`으로 이동한다. 완료 화면에서 시스템 뒤로가기로 이전 입력 화면에 진입할 수 없도록 한다.

### 4.3 회원가입 기본 흐름

```text
SignUpEmail
├─ 다음 → SignUpOtp
└─ 뒤로가기 → 실제 진입 직전 화면
   ├─ Login에서 진입 → Login
   └─ AuthChoice에서 진입 → AuthChoice

SignUpOtp
├─ 인증 성공 → SignUpPassword
└─ 뒤로가기 → SignUpEmail

SignUpPassword
├─ 완료 → SignUpNickname
└─ 뒤로가기 → SignUpEmail

SignUpNickname
├─ 완료 → SignUpConditions
└─ 뒤로가기 → SignUpPassword
```

`SignUpEmail`의 뒤로가기는 별도의 진입 경로 값을 전달하지 않고 Navigation의 `popBackStack()`을 사용한다. 따라서 `Login`에서 진입하면 `Login`으로, `AuthChoice`에서 진입하면 `AuthChoice`로 돌아간다.

OTP 인증 후에는 `SignUpOtp` 화면으로 다시 돌아갈 수 없다. `SignUpOtp`에서 `SignUpPassword`로 이동할 때 인증된 OTP 화면을 back stack에서 제거한다. `SignUpPassword`에서 이메일 화면으로 돌아가면 새 OTP 인증을 시작해야 하며 기존 OTP 값과 OTP 오류 상태를 초기화한다.

### 4.4 회원가입 조건부 흐름

```text
SignUpConditions
├─ 알레르기 선택 → SignUpAllergies
├─ 알레르기 미선택 + 채식 선택 → SignUpVegetarian
├─ 둘 다 미선택 → SignUpProfile
└─ 뒤로가기 → SignUpNickname

SignUpAllergies
├─ 채식 선택 → SignUpVegetarian
├─ 채식 미선택 → SignUpProfile
└─ 뒤로가기 → SignUpConditions

SignUpVegetarian
├─ 선택 완료 → SignUpProfile
└─ 뒤로가기
   ├─ 알레르기 선택 → SignUpAllergies
   └─ 알레르기 미선택 → SignUpConditions

SignUpProfile
├─ 다음 → SignUpComplete
└─ 뒤로가기
   ├─ 채식 선택 + 알레르기 선택 → SignUpVegetarian
   ├─ 채식 선택 + 알레르기 미선택 → SignUpVegetarian
   ├─ 채식 미선택 + 알레르기 선택 → SignUpAllergies
   └─ 둘 다 미선택 → SignUpConditions

SignUpComplete
└─ 확인 → Login
```

조건부 화면의 뒤로가기는 사용자가 실제로 통과한 직전 단계와 일치해야 한다. 화면 route만 보고 추측하지 않고 `SignUpUiState`에 보존된 맞춤 조건 선택값을 기준으로 목적지를 결정한다.

`SignUpComplete`에는 UI 뒤로가기 버튼이 없다. 확인을 누르면 회원가입 화면 전체를 back stack에서 제거하고 `Login`으로 이동한다. 완료 화면에서 시스템 뒤로가기로 프로필이나 입력 화면에 재진입할 수 없도록 한다.

## 5. 입력값 및 상태 보존 정책

회원가입은 여러 화면에서 하나의 가입 정보를 완성하므로 가입 플로우 범위에서 공유하는 `SignUpViewModel`과 `SignUpUiState`를 사용한다. 단순 화면 이동만으로 입력값이 초기화되지 않아야 한다.

### 5.1 회원가입에서 보존할 값

| 값 | 보존 범위 | 초기화 시점 |
| --- | --- | --- |
| 이메일 | 회원가입 전체 | 가입 완료, 회원가입 플로우 이탈 또는 이메일 변경 |
| 이메일 형식 오류 | 이메일 화면 | 이메일 수정 또는 화면 검증 재실행 |
| OTP | OTP 인증 전까지 | 인증 완료, 이메일 변경 또는 이메일 화면으로 강제 복귀 |
| OTP 오류 | OTP 인증 전까지 | OTP 수정, 재전송, 이메일 변경 또는 인증 완료 |
| OTP 인증 여부 | 비밀번호 화면 이후 | 이메일 변경, 회원가입 플로우 이탈 또는 가입 완료 |
| 비밀번호와 비밀번호 확인 | 가입 완료 전까지 | 회원가입 플로우 이탈 또는 가입 완료 |
| 비밀번호 오류 | 비밀번호 화면 | 입력값 수정 또는 검증 성공 |
| 닉네임 | 가입 완료 전까지 | 회원가입 플로우 이탈 또는 가입 완료 |
| 닉네임 오류·중복 상태 | 닉네임 화면 | 닉네임 수정 또는 검증 성공 |
| 맞춤 조건 선택 | 프로필 확인까지 | 사용자가 선택을 변경하거나 가입 플로우 이탈·완료 |
| 채식 식단 선택 | 프로필 확인까지 | 채식 조건 해제, 가입 플로우 이탈 또는 가입 완료 |
| 알레르기 항목 선택 | 프로필 확인까지 | 알레르기 조건 해제, 가입 플로우 이탈 또는 가입 완료 |

다음 규칙을 추가로 적용한다.

- `SignUpConditions`로 돌아와 채식 조건을 해제하면 기존 채식 식단 선택값도 제거한다.
- `SignUpConditions`로 돌아와 알레르기 조건을 해제하면 기존 알레르기 선택값도 제거한다.
- 조건 화면을 앞뒤로 이동해도 아직 활성화된 조건의 세부 선택값은 유지한다.
- 화면 회전이나 Compose 재구성으로 입력값이 사라지지 않아야 한다.
- 회원가입을 취소하고 `Login` 또는 `AuthChoice`까지 완전히 이탈한 경우, 다음 회원가입 시작 시 이전 사용자의 입력값이 남지 않도록 전체 가입 상태를 초기화한다.

### 5.2 비밀번호 찾기에서 보존할 값

비밀번호 찾기는 `PasswordResetViewModel`과 별도의 상태를 사용하고 회원가입 상태와 섞지 않는다.

| 값 | 보존 범위 | 초기화 시점 |
| --- | --- | --- |
| 이메일 | 비밀번호 찾기 전체 | 플로우 이탈, 완료 또는 이메일 변경 |
| OTP | OTP 인증 전까지 | 인증 완료, 이메일 변경 또는 새 비밀번호 화면에서 이메일 화면으로 복귀 |
| OTP 인증 여부 | 새 비밀번호 설정까지 | 이메일 화면 복귀, 플로우 이탈 또는 완료 |
| 새 비밀번호와 비밀번호 확인 | 변경 완료 전까지 | 이메일 화면 복귀, 플로우 이탈 또는 완료 |
| 각 입력 오류 | 해당 입력을 수정하기 전까지 | 입력 수정 또는 검증 성공 |

### 5.3 로그인에서 보존할 값

- 로그인 화면 내 재구성이나 일시적인 화면 회전에서는 이메일과 비밀번호 입력값을 유지한다.
- 비밀번호 찾기에서 로그인으로 정상 복귀했을 때 기존 로그인 이메일을 유지할지는 UI 기획에 맞추되, 비밀번호 값은 보안을 위해 초기화하는 것을 기본값으로 한다.
- 로그인 성공 후에는 로그인 입력값과 인증 플로우 상태를 모두 초기화한다.

## 6. 뒤로가기 및 back stack 정책

| 상황 | 정책 |
| --- | --- |
| `Splash → AuthChoice` | `Splash` 제거 |
| `AuthChoice` 시스템 뒤로가기 | 앱 종료 |
| OTP 인증 성공 | 인증 완료된 OTP 화면 제거 |
| 새 비밀번호 화면에서 뒤로가기 | 이메일 화면으로 이동하고 OTP 관련 상태 초기화 |
| 회원가입 비밀번호 화면에서 뒤로가기 | 이메일 화면으로 이동하고 OTP 관련 상태 초기화 |
| `PasswordResetComplete → Login` | 비밀번호 찾기 스택 전체 제거 |
| `SignUpComplete → Login` | 회원가입 스택 전체 제거 |
| `Login → Home` | 인증 스택 전체 제거 |
| 완료 화면 시스템 뒤로가기 | 이전 입력 화면으로 이동하지 않도록 차단하거나 안전한 목적지로 이동 |

완료 화면에서 시스템 뒤로가기를 단순히 무시할지, `Login`으로 이동시킬지는 구현 방식에 따라 선택할 수 있다. 어느 방식을 사용하더라도 완료된 입력 화면으로 돌아가서는 안 된다.

## 7. API 연결 전 임시 검증 정책

UI 상태와 화면 이동을 확인하기 위해 로컬 검증 또는 Fake 검증기를 사용한다.

- 이메일: 공백 여부와 이메일 형식 검사
- OTP: 정해진 자릿수를 모두 입력하면 인증 버튼 활성화
- 비밀번호: 기획된 길이와 문자 조합 규칙 검사
- 비밀번호 확인: 새 비밀번호와 일치 여부 검사
- 닉네임: 공백과 길이 검사
- OTP 오류와 닉네임 중복 오류: 미리 정한 테스트 입력값으로 재현

임시 성공·실패 값은 UI 코드에 흩어 놓지 않고 `FakeAuthValidator` 등 한곳에 모은다. 실제 API 연결 시 해당 구현만 교체할 수 있도록 한다.

## 8. 구현 시 확인 사항

- UI 상태별로 별도 route나 Composable 화면을 중복 생성하지 않는다.
- 입력값에서 계산할 수 있는 `isButtonEnabled`, `isTyping` 등의 값은 가능하면 원본 상태로 중복 저장하지 않는다.
- `SignUpUiState`의 조건 선택값을 화면 분기와 뒤로가기의 단일 기준으로 사용한다.
- 시스템 뒤로가기와 화면의 뒤로가기 버튼이 동일한 정책으로 동작하는지 확인한다.
- 각 화면에 미입력, 입력, 오류 상태 Preview를 작성한다.
- 긴 문구, 키보드 노출, 작은 화면에서 레이아웃이 잘리지 않는지 확인한다.
- API 연결 전 임시 로직에는 교체 지점을 알 수 있도록 TODO를 남긴다.
