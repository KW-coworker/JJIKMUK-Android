# 폰트 리소스

JJIKMUK 디자인 시스템은 다음 서체를 사용합니다.

- Inter: 본문·영문·숫자용 — https://fonts.google.com/specimen/Inter
- Baloo 2: 브랜드·강조 문구용 — 실제 사용 폰트의 다운로드 및 라이선스 링크를 확인하여 추가합니다.

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

- `baloo_regular.ttf`

`ui/theme/Font.kt`에서는 Inter를 `Inter`, Baloo 2를 `Baloo2`라는
`FontFamily`로 등록합니다.

```kotlin
val Inter = FontFamily(
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

val Baloo2 = FontFamily(
    Font(resId = R.font.baloo_regular, weight = FontWeight.Normal),
)
```

`.ttf`, `.otf`, `.ttc` 파일은 현재 `.gitignore` 대상이므로 Git에 포함되지
않습니다. 폰트 파일이 없는 환경에서는 위 `R.font.*` 리소스를 생성할 수 없어
빌드가 실패합니다. 새 팀원과 CI 환경에도 동일한 파일을 별도로 제공해야 합니다.

폰트 파일을 배포하거나 팀에 제공하기 전에 각 폰트의 라이선스와 재배포 조건을
반드시 확인하세요.
