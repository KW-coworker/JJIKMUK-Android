# 회원가입 조건 API 매핑

> 2026-10-31. 일반 이메일 가입 4단계 구현 기준. 기존 UI ID/아이콘은 변경하지 않는다. 백엔드 커밋 `f49478ea2b51ca25bdffd82ad08e112ce8b44e68`의 전체 API 명세를 확인했다. 계약 확인과 실서버 배포·검증은 구분한다.

## 현재 연결 범위

- 조건 미선택 가입 및 알레르기 26개·채식 6개·영양조건 6개를 선택한 가입은 `POST /api/auth/signup`에 연결했다. `allergies`와 `specialDiet`는 CSV이며 선택이 없으면 생략한다. diseases/dislikedIngredients는 입력 UI가 없으므로 생략한다.
- **명세에 없는 알 수 없는 항목이 입력되면 요청을 보내지 않는다.** 선택값을 지우거나, 모르는 값만 빼거나, underscore를 자동 camelCase로 변환하지 않는다. 목록은 기존대로 표시하고 선택도 유지한다.
- 채식 조건을 선택했다면 세부 유형 1개가 필요하다. Allergy/Vegetarian은 UI 그룹이며 그 이름 자체를 서버 식단 ID로 보내지 않는다.
- 구현 위치: `feature/auth/presentation/signup/SignUpConditionMapping.kt`. 기존 공통 카탈로그의 표시명·이모지를 재사용하고 API 경계에서만 변환한다.

## 알레르기

| 기존 UI ID | 가입 API 전송값 | 상태 |
| --- | --- | --- |
| `egg` | `egg` | 문서 확인 |
| `milk` | `milk` | 문서 확인 |
| `soy` | `soy` | 문서 확인 |
| `wheat` | `wheat` | 문서 확인 |
| `pork` | `pork` | 문서 확인 |
| `chicken` | `chicken` | 문서 확인 |
| `shrimp` | `shrimp` | 문서 확인 |
| `crab` | `crab` | 문서 확인 |
| `squid` | `squid` | 문서 확인 |
| `mackerel` | `mackerel` | 문서 확인 |
| `shellfish` | `shellfish` | 문서 확인 |
| `oyster` | `oyster` | 문서 확인 |
| `mussel` | `mussel` | 문서 확인 |
| `abalone` | `abalone` | 문서 확인 |
| `peach` | `peach` | 문서 확인 |
| `tomato` | `tomato` | 문서 확인 |
| `peanut` | `peanut` | 문서 확인 |
| `walnut` | `walnut` | 문서 확인 |
| `buckwheat` | `buckwheat` | 문서 확인 |
| `pine_nut` | `pine_nut` | 문서 확인 |
| `sulfites` | `sulfites` | 문서 확인 |
| `sesame` | `sesame` | 문서 확인 |
| `almond` | `almond` | 문서 확인 |
| `mustard` | `mustard` | 문서 확인 |
| `celery` | `celery` | 문서 확인 |
| `beef` | `beef` | 문서 확인 |

## 식이조건·채식 유형

| 기존 UI ID | 가입 API 전송값 | 상태 |
| --- | --- | --- |
| `low_sugar` | `lowSugar` | 문서 확인 |
| `low_sodium` | `lowSodium` | 문서 확인 |
| `vegan` | `vegan` | 문서 확인 |
| `low_calorie` | `lowCalorie` | 문서 확인 |
| `gluten_free` | `glutenFree` | 문서 확인 |
| `low_fat` | `lowFat` | 문서 확인 |
| `high_protein` | `highProtein` | 문서 확인 |
| `lacto` | `lactoVegetarian` | 문서 확인 |
| `ovo` | `ovoVegetarian` | 문서 확인 |
| `lacto_ovo` | `lactoOvoVegetarian` | 문서 확인 |
| `pesco` | `pescatarian` | 문서 확인 |
| `pollo` | `pollotarian` | 문서 확인 |

## 계약 근거와 남은 확인

- [전체 API 명세 — 고정 커밋](https://github.com/KW-coworker/JJIKMUK-Backend/blob/f49478ea2b51ca25bdffd82ad08e112ce8b44e68/docs/api-spec/API_REFERENCE.md): `GET /api/products/classifications`의 `data.allergens[].id`와 `GET /api/products/filters`의 `data[].key` 전체 응답을 확인했다.
- 두 API는 인증 불필요이며 가입 전에도 조회 가능하다. 가입 `allergies`/`specialDiet`는 같은 ID의 CSV다. 채식 최대 1개와 영양조건 복수 선택, 전체 해제는 null/생략이다. `pine_nut`은 그대로 사용하며 밀가루 별도 ID는 없다.
- 앱의 현재 선택지는 고정 카탈로그를 재사용한다. 이번 단계는 전체 코드의 명시적 매핑이며, 가입 때마다 메타데이터 GET을 추가 호출하는 동적 카탈로그 기능은 넣지 않았다.
- [최신 인증 v2](https://github.com/KW-coworker/JJIKMUK-Backend/blob/f49478ea2b51ca25bdffd82ad08e112ce8b44e68/docs/api-spec/frontend-auth-integration-v2.md)가 기존 설명보다 우선한다.
- 팀 개발 HTTPS 주소가 문서에 명시됐다. 실제 배포 커밋·DB 마이그레이션·메일 설정을 확인한 뒤 실서버 테스트한다. 명세 확보만으로 배포 완료를 가정하거나 기기 인증 설정을 바꾸지 않는다.

## 가입 제출 및 복구

| 결과 | 처리 |
| --- | --- |
| 201 + 유효한 userId | 완료 화면 → 로그인. 가입 응답으로 로그인 상태를 만들지 않음. 증명/비밀번호/입력 상태 정리 |
| 닉네임 중복·입력 오류 | 입력·유효한 증명 유지, 수정 후 재제출. 사전 사용 가능 표시가 있어도 서버 최종 검사 우선 |
| 인증 만료/무효 | 증명 폐기, 입력 유지하고 이메일 재인증 |
| 이미 가입한 이메일 | 성공으로 간주하지 않고 로그인 버튼 제공 |
| 통신 실패·500·응답 손상·취소 | 성공 여부 불명 안내, 증명 재사용/자동 재전송 차단, 로그인 확인 또는 재인증 |
| 서버 주소 미설정 | 성공 화면으로 가지 않음. 입력 보존 |

연타와 제출 중 수정은 막고, 흐름 초기화 뒤 늦은 응답은 무시한다. 인증 증명은 TTL과 서버 절대 만료 중 먼저 도래하는 시각까지만 사용한다. 완료·로그인 이동 후 가입 입력 화면이 뒤로가기로 다시 열리지 않도록 정리한다.

## 사진과 남은 범위

가입 응답에는 서비스 토큰이 없으므로 인증이 필요한 사진 API에 바로 업로드할 수 없다. 사진을 선택한 채 제출하면 준비 중 안내를 표시하고, 사진 선택 메뉴에 ‘사진 선택 해제’를 제공한다. 사용자가 명시적으로 해제하기 전에는 사진을 버리고 가입하지 않는다.

사진은 5단계에서 최초 로그인 후 업로드/서버 조회와 연결할 예정이다. 가입 전 선택 사진의 보관·업로드 시점은 아직 결정해야 한다. Google 추가 입력 완료, 가족/본인 조건 편집, 서버 배포 검증은 이번 이메일 가입 연결과 별도다.
