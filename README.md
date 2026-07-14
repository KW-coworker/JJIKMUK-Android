# JJIKMUK

JJIKMUK은 Jetpack Compose 기반으로 개발하는 Android 앱 프로젝트입니다.

## 프로젝트 환경

| 항목 | 버전 / 값 |
| --- | --- |
| 패키지명 | `com.coworker.jjikmuk` |
| 빌드 설정 | Kotlin DSL (`build.gradle.kts`) |
| Kotlin | `2.4.0` |
| Gradle | `9.4.1` |
| Android Gradle Plugin | `9.2.1` |
| compileSdk | `36.1` |
| minSdk | `24` |
| targetSdk | `36` |
| Java | `17` |
| Compose BOM | `2026.02.01` |

## 기술 스택

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- Lifecycle Runtime Compose
- Coroutine / Flow
- Hilt
- Retrofit
- OkHttp
- Gson
- Coil
- DataStore
- Room
- Gradle Version Catalog (`gradle/libs.versions.toml`)

## 실행 방법

1. Android Studio에서 프로젝트를 엽니다.
2. Gradle Sync가 끝날 때까지 기다립니다.
3. 에뮬레이터 또는 실제 Android 기기를 선택합니다.
4. `app` 실행 설정으로 앱을 실행합니다.

## Git Ignore 규칙

이 프로젝트는 개인 개발 환경 파일, IDE 캐시, 자동 생성되는 빌드 결과물을 Git에 올리지 않습니다.

| 무시하는 항목 | 이유 |
| --- | --- |
| `local.properties` | 개인 PC의 Android SDK 경로가 들어 있습니다. 개발자마다 경로가 다릅니다. |
| `.gradle/` | Gradle이 자동으로 생성하는 캐시 폴더입니다. |
| `build/` | 빌드할 때 자동으로 생성되는 결과물 폴더입니다. |
| `.idea/workspace.xml` | Android Studio의 개인 작업 환경 설정 파일입니다. |
| `.idea/caches/` | Android Studio 캐시 파일입니다. |
| `*.iml` | IDE가 생성하는 모듈 설정 파일입니다. |
| `.DS_Store` | macOS가 자동으로 생성하는 시스템 파일입니다. |
| `captures/` | Android Studio 캡처 파일입니다. |
| `.externalNativeBuild/`, `.cxx/` | Native build 사용 시 생성되는 빌드 결과물 폴더입니다. |

`local.properties`는 절대 Git에 올리지 않습니다.

예시:

```properties
sdk.dir=/Users/username/Library/Android/sdk
```

이 파일은 각자 로컬 환경에서만 관리합니다.

## 현재 세팅 상태

- Compose 프로젝트 생성 완료
- 앱 패키지명 `com.coworker.jjikmuk` 설정 완료
- 기본 Compose 화면 실행 확인 완료
- 기본 `.gitignore` 설정 완료
- 기본 앱 의존성 설정 완료
- Hilt Application 클래스 등록 완료

## 다음 단계

1. 기본 패키지 구조를 생성합니다.
2. 앱 테마와 네비게이션 구조를 세팅합니다.
3. `home`, `chat`, `product`, `mypage`, `onboarding` 같은 feature 패키지를 준비합니다.
4. 네트워크, 로컬 저장소, Repository 구조를 세팅합니다.
