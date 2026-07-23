# 폰트 리소스

JJIKMUK 디자인 시스템은 다음 서체를 사용합니다.

- Pretendard: 한글 UI용 — https://github.com/orioncactus/pretendard
- Inter: 영문 UI·날짜·시간·숫자용 — https://fonts.google.com/specimen/Inter
- Baloo 2 ExtraBold: 로고·영문 Section Title용 — https://fonts.google.com/specimen/Baloo+2

## 추가 방법

아래 파일들을 `app/src/main/res/font` 디렉토리에 배치합니다.

파일명은 영문 소문자와 snake_case만 사용합니다.

### Inter

- `inter_thin.ttf`
- `inter_extra_light.ttf`
- `inter_light.ttf`
- `inter_regular.ttf`
- `inter_medium.ttf`
- `inter_semi_bold.ttf`
- `inter_bold.ttf`
- `inter_extra_bold.ttf`
- `inter_black.ttf`

### Baloo 2

- `baloo2_extra_bold.ttf`

### Pretendard

- `pretendard_thin.ttf`
- `pretendard_extra_light.ttf`
- `pretendard_light.ttf`
- `pretendard_regular.ttf`
- `pretendard_medium.ttf`
- `pretendard_semi_bold.ttf`
- `pretendard_bold.ttf`
- `pretendard_extra_bold.ttf`
- `pretendard_black.ttf`

`ui/theme/Font.kt`에서는 Inter, Pretendard, Baloo 2를 각각의 `FontFamily`로
등록합니다.

```kotlin
val InterFontFamily = FontFamily(
    Font(resId = R.font.inter_thin, weight = FontWeight.Thin),
    Font(resId = R.font.inter_extra_light, weight = FontWeight.ExtraLight),
    Font(resId = R.font.inter_light, weight = FontWeight.Light),
    Font(resId = R.font.inter_regular, weight = FontWeight.Normal),
    Font(resId = R.font.inter_medium, weight = FontWeight.Medium),
    Font(resId = R.font.inter_semi_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.inter_bold, weight = FontWeight.Bold),
    Font(resId = R.font.inter_extra_bold, weight = FontWeight.ExtraBold),
    Font(resId = R.font.inter_black, weight = FontWeight.Black),
)

val Baloo2FontFamily = FontFamily(
    Font(resId = R.font.baloo2_extra_bold, weight = FontWeight.ExtraBold),
)
```

한글 UI 토큰은 Pretendard가 기본입니다. 영문 UI·날짜·시간·숫자는 같은 의미
토큰에 `asEnglish()`를 호출해 Inter를 적용합니다.

폰트 파일은 저장소에 포함됩니다. 따라서 별도의 로컬 설치 없이 새 팀원과 CI
환경에서도 동일한 폰트로 빌드할 수 있습니다. 폰트를 교체할 때는 해당 서체의
라이선스와 재배포 조건을 다시 확인하세요.
