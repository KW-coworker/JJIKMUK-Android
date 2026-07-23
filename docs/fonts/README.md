# 폰트 리소스

JJIKMUK 디자인 시스템은 다음 서체를 사용합니다.

- Inter: 본문·영문·숫자용 — https://fonts.google.com/specimen/Inter
- Baloo 2 ExtraBold: 브랜드·강조 문구용 — https://fonts.google.com/specimen/Baloo+2

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

`ui/theme/Font.kt`에서는 Inter를 `Inter`, Baloo 2를 `Baloo2`라는
`FontFamily`로 등록합니다.

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

폰트 파일은 저장소에 포함됩니다. 따라서 별도의 로컬 설치 없이 새 팀원과 CI
환경에서도 동일한 폰트로 빌드할 수 있습니다. 폰트를 교체할 때는 해당 서체의
라이선스와 재배포 조건을 다시 확인하세요.
