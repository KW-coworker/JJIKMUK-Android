# 알레르기·식이조건 공통 카탈로그

## 목적

알레르기와 식이조건의 ID, Kotlin 코드값, 표시 이름, 아이콘을 한 곳에서 관리한다.
회원가입, 프로필, 상품 목록 등에서 항목을 임의로 다시 정의하지 않고
`ui/catalog/FoodPreferenceCatalog.kt`의 `FoodAllergy`와 `DietaryCondition`을 사용한다.

아이콘은 별도 이미지 파일이 아닌 Unicode 이모지 문자열이다. 같은 이모지를 사용하는
항목도 이름과 함께 표시되므로 중복을 허용한다.

## ID와 코드값

- **ID**: 저장 또는 API 연동에 사용하는 안정적인 문자열이다. 예: `"wheat"`.
- **코드값**: Kotlin에서 참조하는 enum 값이다. 예: `FoodAllergy.WHEAT`.
- **아이콘 변수**: enum 값의 `icon` 프로퍼티다. 예: `FoodAllergy.WHEAT.icon`.
- **문자열 변수**: 사용자에게 표시할 이름이 정의된 Android 문자열 리소스다.

```kotlin
val allergy = FoodAllergy.WHEAT
val id = allergy.id                         // "wheat"
val icon = allergy.icon                     // "🌾"
val label = stringResource(allergy.labelRes) // "밀"

val restored = FoodAllergy.fromId("wheat")
```

## 알레르기

밀가루 항목은 두지 않는다. 밀은 `FoodAllergy.WHEAT` 하나로 관리한다.

| 표시 이름 | ID | Kotlin 코드값 | 현재 아이콘 | 문자열 변수명 |
| --- | --- | --- | --- | --- |
| 계란 | `egg` | `FoodAllergy.EGG` | 🥚 | `R.string.allergy_egg` |
| 우유 | `milk` | `FoodAllergy.MILK` | 🥛 | `R.string.allergy_milk` |
| 대두 | `soy` | `FoodAllergy.SOY` | 🫘 | `R.string.allergy_soy` |
| 밀 | `wheat` | `FoodAllergy.WHEAT` | 🌾 | `R.string.allergy_wheat` |
| 돼지고기 | `pork` | `FoodAllergy.PORK` | 🥓 | `R.string.allergy_pork` |
| 닭고기 | `chicken` | `FoodAllergy.CHICKEN` | 🍗 | `R.string.allergy_chicken` |
| 새우 | `shrimp` | `FoodAllergy.SHRIMP` | 🦐 | `R.string.allergy_shrimp` |
| 게 | `crab` | `FoodAllergy.CRAB` | 🦀 | `R.string.allergy_crab` |
| 오징어 | `squid` | `FoodAllergy.SQUID` | 🦑 | `R.string.allergy_squid` |
| 고등어 | `mackerel` | `FoodAllergy.MACKEREL` | 🐟 | `R.string.allergy_mackerel` |
| 조개류 | `shellfish` | `FoodAllergy.SHELLFISH` | 🐚 | `R.string.allergy_shellfish` |
| 굴 | `oyster` | `FoodAllergy.OYSTER` | 🦪 | `R.string.allergy_oyster` |
| 홍합 | `mussel` | `FoodAllergy.MUSSEL` | 🦪 | `R.string.allergy_mussel` |
| 전복 | `abalone` | `FoodAllergy.ABALONE` | 🐚 | `R.string.allergy_abalone` |
| 복숭아 | `peach` | `FoodAllergy.PEACH` | 🍑 | `R.string.allergy_peach` |
| 토마토 | `tomato` | `FoodAllergy.TOMATO` | 🍅 | `R.string.allergy_tomato` |
| 땅콩 | `peanut` | `FoodAllergy.PEANUT` | 🥜 | `R.string.allergy_peanut` |
| 호두 | `walnut` | `FoodAllergy.WALNUT` | 🌰 | `R.string.allergy_walnut` |
| 메밀 | `buckwheat` | `FoodAllergy.BUCKWHEAT` | 🍜 | `R.string.allergy_buckwheat` |
| 잣 | `pine_nut` | `FoodAllergy.PINE_NUT` | 🫘 | `R.string.allergy_pine_nut` |
| 아황산류 | `sulfites` | `FoodAllergy.SULFITES` | 🧪 | `R.string.allergy_sulfites` |
| 참깨 | `sesame` | `FoodAllergy.SESAME` | 🧂 | `R.string.allergy_sesame` |
| 아몬드 | `almond` | `FoodAllergy.ALMOND` | 🫘 | `R.string.allergy_almond` |
| 머스타드 | `mustard` | `FoodAllergy.MUSTARD` | 🍯 | `R.string.allergy_mustard` |
| 셀러리 | `celery` | `FoodAllergy.CELERY` | 🥒 | `R.string.allergy_celery` |
| 소고기 | `beef` | `FoodAllergy.BEEF` | 🥩 | `R.string.allergy_beef` |

## 대표 식이조건

| 표시 이름 | ID | Kotlin 코드값 | 현재 아이콘 | 문자열 변수명 |
| --- | --- | --- | --- | --- |
| 저당 | `low_sugar` | `DietaryCondition.LOW_SUGAR` | 📉 | `R.string.dietary_condition_low_sugar` |
| 저염 | `low_sodium` | `DietaryCondition.LOW_SODIUM` | 🧂 | `R.string.dietary_condition_low_sodium` |
| 비건 | `vegan` | `DietaryCondition.VEGAN` | 🥗 | `R.string.dietary_condition_vegan` |
| 저칼로리 | `low_calorie` | `DietaryCondition.LOW_CALORIE` | 🏃 | `R.string.dietary_condition_low_calorie` |
| 글루텐프리 | `gluten_free` | `DietaryCondition.GLUTEN_FREE` | 🍞 | `R.string.dietary_condition_gluten_free` |
| 저지방 | `low_fat` | `DietaryCondition.LOW_FAT` | 🥑 | `R.string.dietary_condition_low_fat` |
| 고단백 | `high_protein` | `DietaryCondition.HIGH_PROTEIN` | 💪 | `R.string.dietary_condition_high_protein` |

회원가입의 `SignUpCondition.Allergy`와 `SignUpCondition.Vegetarian`은 각각
알레르기 목록과 채식 세부유형 화면으로 이동시키는 상위 선택값이다. 개별 표시 항목의
ID와 아이콘은 이 문서의 공통 카탈로그를 사용한다.

## 채식 세부유형

모든 채식 세부유형은 현재 🥗 아이콘을 함께 사용한다.

| 표시 이름 | ID | Kotlin 코드값 | 현재 아이콘 | 문자열 변수명 |
| --- | --- | --- | --- | --- |
| 비건 | `vegan` | `DietaryCondition.VEGAN` | 🥗 | `R.string.dietary_condition_vegan` |
| 락토 | `lacto` | `DietaryCondition.LACTO` | 🥗 | `R.string.dietary_condition_lacto` |
| 오보 | `ovo` | `DietaryCondition.OVO` | 🥗 | `R.string.dietary_condition_ovo` |
| 락토 오보 | `lacto_ovo` | `DietaryCondition.LACTO_OVO` | 🥗 | `R.string.dietary_condition_lacto_ovo` |
| 페스코 | `pesco` | `DietaryCondition.PESCO` | 🥗 | `R.string.dietary_condition_pesco` |
| 폴로 | `pollo` | `DietaryCondition.POLLO` | 🥗 | `R.string.dietary_condition_pollo` |

## 사용 기준

- 화면에서 알레르기 전체 목록이 필요하면 `FoodAllergy.entries`를 사용한다.
- 저장된 알레르기 ID를 복원할 때는 `FoodAllergy.fromId(id)`를 사용한다.
- 식이조건 ID를 복원할 때는 `DietaryCondition.fromId(id)`를 사용한다.
- 이름은 직접 하드코딩하지 않고 각 항목의 `labelRes`를 사용한다.
- 아이콘은 직접 다시 매핑하지 않고 각 항목의 `icon`을 사용한다.
- 항목을 추가하거나 이름·아이콘·ID를 변경하면 카탈로그 코드, 문자열 리소스와 이 문서를 함께 수정한다.

현재 카탈로그 사용처는 회원가입 알레르기 선택, 회원가입 프로필 요약,
회원가입 맞춤 조건 이름, 상품 목록 알레르기 칩이다.
