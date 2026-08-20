# JJIKMUK 공통 컴포넌트 명세

## 문서 목적

이 문서는 `app/src/main/java/com/coworker/jjikmuk/ui/component`에 있는 공개 Compose 컴포넌트를 팀원이 재사용할 수 있도록 정리한 명세다. 노션에 옮길 때도 아래 표를 기준으로 사용한다.

- **컴포넌트명**은 Kotlin의 `@Composable` 함수명이다.
- 화면 전용 조합은 `feature/**/presentation`에 두고, 여러 화면에서 재사용하는 UI만 `ui/component`에 둔다.
- 모든 컴포넌트는 가능한 한 `JjikmukTheme`의 색상·타이포그래피를 사용한다.
- `Modifier`는 호출 화면에서 크기, 여백, 정렬을 조절할 수 있도록 전달한다.

## 인증·회원가입 공통 컴포넌트

| 파일 위치 | 컴포넌트명 | 주요 입력값 | 역할 및 사용처 | 비고 |
| --- | --- | --- | --- | --- |
| `ui/component/JjikmukAuthButton.kt` | `JjikmukPrimaryButton` | `text`, `onClick`, `enabled`, `modifier` | 브랜드 색상의 주요 CTA 버튼. 로그인, 이메일 전송, OTP 인증, 비밀번호·닉네임 완료, 조건 선택, 프로필, 완료 화면에서 사용한다. | 활성·비활성·눌림 상태를 포함하며 기본 높이는 56dp다. |
| `ui/component/JjikmukAuthButton.kt` | `JjikmukSecondaryButton` | `text`, `onClick`, `enabled`, `modifier` | 테두리가 있는 보조 CTA 버튼. 시작 선택 화면의 `회원가입` 버튼에서 사용한다. | Primary와 크기는 같고 시각적 우선순위가 낮다. |
| `ui/component/JjikmukAuthButton.kt` | `JjikmukSecondaryIconButton` | `icon`, `contentDescription`, `onClick`, `enabled`, `modifier` | 텍스트 대신 아이콘을 표시하는 보조 버튼. 로그인 화면의 Google 로그인 버튼에서 사용한다. | `Painter`를 받아 공급자 아이콘을 교체할 수 있다. |
| `ui/component/JjikmukAuthTextField.kt` | `JjikmukEmailTextField` | `value`, `onValueChange`, `placeholder`, `isError`, `imeAction`, `onImeAction` | 이메일 키보드와 영문 텍스트 스타일을 적용한 입력창. 로그인, 비밀번호 찾기 이메일, 회원가입 이메일 화면에서 사용한다. | 오류 시 테두리가 오류 색상으로 바뀐다. |
| `ui/component/JjikmukAuthTextField.kt` | `JjikmukNicknameTextField` | `value`, `onValueChange`, `placeholder`, `isError`, `imeAction`, `onImeAction` | 닉네임 입력용 한 줄 입력창. 회원가입 닉네임 생성 및 프로필 최종 확인 화면에서 사용한다. | 한글 기본 타이포그래피를 사용한다. |
| `ui/component/JjikmukAuthTextField.kt` | `JjikmukPasswordTextField` | `value`, `onValueChange`, `placeholder`, `isError`, `showVisibilityToggle`, `imeAction`, `onImeAction` | 비밀번호 마스킹과 보기/숨기기 기능을 제공한다. 로그인, 회원가입 비밀번호, 새 비밀번호 화면에서 사용한다. | `showVisibilityToggle=false`로 눈 아이콘을 숨길 수 있다. |
| `ui/component/JjikmukAuthTopBar.kt` | `JjikmukAuthTopBar` | `onBackClick`, `modifier` | 인증 화면 상단의 뒤로가기 영역. 로그인·이메일·OTP·비밀번호·닉네임·조건·프로필 화면에서 사용한다. | 현재 인증 화면용 구현이다. 앱 공통 상단바와 통합 시 교체 여부를 검토한다. |
| `ui/component/JjikmukOtpTextField.kt` | `JjikmukOtpTextField` | `value`, `onValueChange`, `isError`, `digitCount`, `modifier` | 숫자 OTP를 자릿수별 칸으로 보여주는 입력 컴포넌트. 회원가입과 비밀번호 찾기 OTP 화면에서 공통 사용한다. | 숫자만 허용하며 기본값은 4자리다. |
| `ui/component/JjikmukAuthCompleteScreen.kt` | `JjikmukAuthCompleteScreen` | `title`, `description`, `buttonText`, `onButtonClick`, `modifier` | 인증 완료 화면의 성공 이미지, 문구, CTA를 묶은 화면형 공통 컴포넌트. 비밀번호 재설정 완료와 회원가입 완료에서 사용한다. | 완료 화면에서 이전 입력 화면으로 돌아가지 않도록 시스템 뒤로가기를 차단한다. |
| `ui/component/JjikmukConditionCard.kt` | `JjikmukConditionCard` | `title`, `description`, `selected`, `onSelectedChange`, `modifier` | 저당·저염·채식·알레르기 등 맞춤 조건을 다중 선택하는 카드. `SignUpConditions`에서 사용한다. | 체크박스 의미의 `toggleable` 접근성을 적용한다. |
| `ui/component/JjikmukVegetarianDietCard.kt` | `JjikmukVegetarianDietCard` | `title`, `description`, `selected`, `onClick`, `modifier` | 비건·락토·오보 등 채식 유형 하나를 고르는 카드. `SignUpVegetarian`에서 사용한다. | 단일 선택이므로 RadioButton 의미의 `selectable`을 사용한다. |
| `ui/component/JjikmukAllergyChip.kt` | `JjikmukAllergyChip` | `emoji`, `label`, `selected`, `onSelectedChange`, `modifier` | Unicode 이모지와 알레르기 이름을 표시하는 다중 선택 칩. `SignUpAllergies`에서 사용한다. | 선택 상태에 따라 배경·테두리 두께가 바뀐다. |
| `ui/component/JjikmukVerticalScrollIndicator.kt` | `JjikmukVerticalScrollIndicator` | `scrollValue`, `scrollMaxValue`, `modifier` | Compose `ScrollState`와 연동되는 세로 스크롤 위치 표시기. 채식 및 알레르기 목록에서 사용한다. | 스크롤 자체를 만들지는 않고 현재값과 최댓값을 받아 위치만 표시한다. |
| `ui/component/JjikmukProfileImagePicker.kt` | `JjikmukProfileImagePicker` | `imageModel`, `onClick`, `modifier` | 기본 프로필 또는 선택 사진과 카메라 배지를 표시하는 원형 선택 영역. `SignUpProfile`에서 사용한다. | Coil `AsyncImage`가 URI·리소스 등 이미지 모델을 표시한다. 실제 촬영/갤러리 실행은 호출 화면이 담당한다. |
| `ui/component/JjikmukSelectedItemChip.kt` | `JjikmukSelectedItemChip` | `emoji`, `label`, `modifier` | 가입 중 선택한 알레르기·식단·맞춤 조건을 읽기 전용으로 요약하는 칩. `SignUpProfile`에서 사용한다. | 선택/삭제 동작이 없는 표시 전용 컴포넌트다. |

## 홈·채팅 공통 컴포넌트

| 파일 위치 | 컴포넌트명 | 주요 입력값 | 역할 및 사용처 | 비고 |
| --- | --- | --- | --- | --- |
| `ui/component/ImageSourceBottomSheet.kt` | `ImageSourceBottomSheet` | `onDismissRequest`, `onCameraClick`, `onGalleryClick`, `sheetState`, `modifier` | 카메라 촬영 또는 갤러리 업로드 방법을 선택하는 바텀시트. 홈과 채팅 화면에서 사용한다. | 현재 문구는 상품 이미지 용도다. 프로필 사진에서 재사용하려면 문구 외부 주입을 먼저 고려한다. |
| `ui/component/JjikmukBottomNavigationBar.kt` | `JjikmukBottomNavigationBar` | `selectedTab`, `onTabClick`, `modifier` | Home, Diet, Product, History, MY 탭을 제공하는 하단 내비게이션. 홈 화면에서 사용한다. | 탭 정의는 같은 파일의 `MainTab` enum이다. |
| `ui/component/JjikmukTopAppBar.kt` | `JjikmukTopAppBar` | `selectedProfiles`, `onChatHistoryClick`, `onScanTargetClick`, `modifier` | 채팅 기록 버튼과 스캔 대상 프로필 버튼을 조합한 홈 상단바. 홈 화면에서 사용한다. | 내부에서 `ChatHistoryButton`, `ScanTargetButton`을 조합한다. |
| `ui/component/ChatHistoryButton.kt` | `ChatHistoryButton` | `onClick`, `modifier` | 채팅 기록 화면을 여는 아이콘 버튼. `JjikmukTopAppBar` 내부에서 사용한다. | 40dp 터치 영역을 제공한다. |
| `ui/component/ScanTargetButton.kt` | `ScanTargetButton` | `selectedProfiles`, `onClick`, `modifier` | 현재 스캔 대상 프로필을 겹친 원형 이미지로 보여주는 버튼. `JjikmukTopAppBar`에서 사용한다. | 데이터 모델은 `ScanTargetProfileUiModel`, 최대 5명을 표시한다. |
| `ui/component/ScanTargetPopup.kt` | `ScanTargetPopup` | `members`, `onMemberCheckedChange`, `onDismissRequest`, `modifier` | 음식 스캔 대상 구성원을 선택하는 팝업. 홈 화면에서 사용한다. | 데이터 모델은 `ScanTargetMemberUiModel`이다. |
| `ui/component/JjikmukDraggableScannerFab.kt` | `JjikmukDraggableScannerFab` | `onClick`, `modifier` | 탭하면 촬영을 실행하고 길게 누르면 위치를 옮길 수 있는 스캐너 플로팅 버튼. 홈 화면에서 사용한다. | 드래그 위치를 `rememberSaveable`로 유지한다. |
| `ui/component/JjikmukMessageInputBar.kt` | `JjikmukMessageInputBar` | `text`, `placeholder`, `onTextChange`, `onAddClick`, `onSendClick`, `modifier` | 메시지 입력, 첨부 추가, 전송을 한 줄로 제공한다. 홈과 채팅 화면에서 사용한다. | 키보드 Send 액션과 전송 아이콘이 같은 콜백을 호출한다. |

## 재사용 시 기준

1. 동일한 역할과 상태를 가진 컴포넌트가 위 표에 있으면 새로 만들지 않고 재사용한다.
2. 화면별 여백과 배치는 호출부의 `Modifier`에서 결정한다.
3. 문구나 아이콘만 다른 경우 먼저 파라미터화 가능성을 검토한다.
4. 기존 컴포넌트의 의미가 달라지는 경우 억지로 확장하지 않고 새 컴포넌트를 만든다.
5. 새 공통 컴포넌트를 추가하면 이 문서의 파일 위치, 함수명, 입력값, 사용처와 비고를 함께 갱신한다.
