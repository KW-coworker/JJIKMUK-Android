# 찍먹 스캐너 API 명세 — 상품 사진 OCR 및 상품명 정보 추출

> 문서 정리일: 2026-10-08\
> 원문 명세 기준: 2026-10-01 배포된 팀 테스트용 API\
> 출처: 사용자가 제공한 백엔드 Notion API 명세\
> 권장 저장 경로: `docs/API/scanner-ocr-api.md`

상품 사진 1장을 업로드하면 이미지의 글자를 인식하고, 상품명·브랜드·맛·용량·검색어를 구조화된 JSON으로 반환한다.

**이 API는 OCR 결과만 반환한다.** DB 상품 조회, 상품 식별 확정, 영양정보 조회, 알레르기 판정은 수행하지 않는다. DB 검색 API 명세는 별도 문서를 따른다.

이 문서는 제공된 명세의 계약·예시·제한을 유지하고 Codex 및 Android 연동 작업에서 찾기 쉽도록 재구성했다. 배포 상태·한도·서버 설정은 원문 기준이며, 문서 정리 과정에서 실서버를 호출해 재검증한 것은 아니다.

## 목차

- [1. 구현 전 핵심 규칙](#implementation-rules)
- [2. 기본 정보](#basic-info)
- [3. Request](#request)
- [4. Response](#response)
- [5. HTTP 200 응답 상태 구분](#result-status)
- [6. Error](#errors)
- [7. 프론트엔드 연동 시 주의사항](#frontend-notes)

<a id="implementation-rules"></a>
## 1. 구현 전 핵심 규칙

아래는 원문에 명시된 연동 규칙의 요약이다. 세부 계약과 예시는 각 절을 따른다.

1. **`POST /api/ocr`로 실제 이미지 파일 1개를 전송한다.** multipart 파일 필드명은 정확히 `image`다. Query Parameter와 추가 폼 필드는 보내지 않는다.
2. **인증은 `X-OCR-Test-Token` 헤더 하나로 처리한다.** `Bearer`를 붙이지 않으며 회원 로그인·JWT는 필요하지 않다. `Authorization`으로 대체하거나 같은 헤더를 중복 전송하지 않는다.
3. **요청 기기에서 팀 Tailscale 연결 및 서버 접근 권한이 필요하다.** 내부 OCR 서버를 직접 호출하지 않는다.
4. **JPEG 또는 PNG만 허용한다.** 파일 10MiB, 이미지 20,000,000픽셀, 전체 요청 10MiB + 64KiB 제한을 각각 지킨다. HEIC·WebP는 실제 파일 형식을 변환한다.
5. **사진 선택과 스캔 실행을 분리한다.** 사용자 실행 동작에 따라 요청하고 연속 클릭·화면 회전·재진입으로 중복 호출하지 않는다.
6. **자동 재시도·자동 재전송을 사용하지 않는다.** 타임아웃·연결 종료 이후에도 서버에서 이미 처리했을 수 있다. `Retry-After: 3`도 자동 재시도 지시가 아니다.
7. **HTTP 200과 `data.status`를 함께 확인한다.** `OK`, `NO_TEXT`, `NO_SEARCH_KEYWORD`를 구분한다. `OK`도 정확한 DB 상품 식별이나 안전 판정 성공을 의미하지 않는다.
8. **응답의 `data`는 객체이며 배열이 아니다.** nullable 필드, 빈 배열 `[]`, 빈 객체 `{}`를 처리한다. 용량과 요청 ID는 문자열로 유지한다.
9. **오류는 `error` 객체 형식을 사용한다.** 로그인 API 등의 최상위 `status`·`message` 형식으로 파싱하지 않는다. 다른 형식이나 본문 없는 오류도 처리한다.
10. **백엔드 중계 ID와 OCR 작업 ID를 구분한다.** 응답 헤더의 `X-Request-ID`와 정상 본문의 `data.requestId`는 별개다.

<a id="basic-info"></a>
## 2. 기본 정보

| 항목 | 내용 |
| --- | --- |
| 기능 | 상품 사진의 텍스트 인식 및 상품명 정보 추출 |
| Method | `POST` |
| URL | `/api/ocr` |
| Base URL | `https://jjikmuk-backend-dev.tail198441.ts.net` |
| 전체 요청 URL | `https://jjikmuk-backend-dev.tail198441.ts.net/api/ocr` |
| 인증 | 팀 테스트 토큰 필수. 회원 로그인 및 JWT 불필요 |
| Request Body | `multipart/form-data` 이미지 파일 1개 |
| Response | `application/json` |
| 접속 조건 | 요청하는 기기에서 팀 Tailscale 연결 및 서버 접근 권한 필요 |
| 명세 기준 | 2026-10-01 배포된 팀 테스트용 API |

<a id="request"></a>
## 3. Request

### 3.1 Request Header

| 필드명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `X-OCR-Test-Token` | String | O | 담당자에게 전달받은 팀 테스트 토큰. `Bearer`를 붙이지 않고 값 자체를 전달 |
| `Content-Type` | String | O | `multipart/form-data; boundary=...`. boundary는 HTTP 라이브러리가 자동 생성하도록 설정 |

#### 보안 규칙 — 원문 명시

- 팀 토큰은 요청 헤더에만 전달한다. URL, Query Parameter, 사진 폼 필드에 넣지 않는다.
- `Authorization: Bearer ...`는 팀 토큰 헤더를 대체할 수 없다. 회원 JWT는 이 API에 필요하지 않다.
- `X-OCR-Test-Token` 헤더를 중복으로 보내지 않는다.
- OpenAI API 키나 내부 OCR 서비스 토큰을 앱에 넣지 않는다.
- 실제 팀 토큰을 노션 문서·소스·Git·로그·공개 배포용 APK에 기록하지 않는다. 공용 토큰은 팀 내부 테스트용이며 공개 운영용 사용자 인증을 대신하지 않는다.

### 3.2 Query Parameter

**없음.** `keyword`, `userId` 등 별도 파라미터를 보내지 않는다.

### 3.3 Request Body

`multipart/form-data` 형식으로 **실제 이미지 파일 1개만** 전송한다.

| 필드명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `image` | File (Binary) | O | 상품 사진. JPEG 또는 PNG 파일 1개 |

#### 이미지·요청 제한

| 제한 항목 | 허용 범위 |
| --- | --- |
| 파일 형식 | JPEG (`image/jpeg`), PNG (`image/png`) |
| 파일 크기 | 최대 10MiB, 즉 10,485,760바이트 |
| 이미지 해상도 | 가로 × 세로 최대 20,000,000픽셀 |
| 파일 수 | 정확히 1개 |
| 추가 폼 필드 | 허용하지 않음 |
| 여러 프레임·애니메이션 | 허용하지 않음 |
| 전체 요청 크기 | 최대 10MiB + 64KiB. multipart 구분자·헤더 공간 포함 |

- 파일 필드 이름은 정확히 `image`여야 한다.
- JSON의 Base64 문자열, 이미지 URL, PC 파일 경로 문자열을 보내는 방식이 아니다.
- MIME과 실제 파일 형식이 일치해야 한다. HEIC·WebP는 실제 JPEG/PNG로 변환해야 하며 확장자만 바꾸면 안 된다.
- EXIF 회전 정보를 반영하여 인식한다. 반사·흔들림이 적고 상품 하나의 전면이 잘 보이는 사진을 권장한다.

### 3.4 요청 예시

```http
POST /api/ocr HTTP/1.1
Host: jjikmuk-backend-dev.tail198441.ts.net
X-OCR-Test-Token: <팀 테스트 토큰>
Content-Type: multipart/form-data; boundary=example-boundary

--example-boundary
Content-Disposition: form-data; name="image"; filename="product.jpg"
Content-Type: image/jpeg

<JPEG 파일의 실제 바이너리 데이터>
--example-boundary--
```

위 요청은 구조 설명용이다. 실제 앱에서는 Retrofit/OkHttp 등의 multipart 기능을 사용하고, boundary나 전체 `Content-Type`을 임의로 고정하지 않는다.

<a id="response"></a>
## 4. Response

### 4.1 HTTP 상태

```http
200 OK
```

### 4.2 Response Header

| 필드명 | 타입 | 설명 |
| --- | --- | --- |
| `Content-Type` | String | `application/json` |
| `X-Request-ID` | String | 백엔드 중계 요청의 추적 ID |
| `Cache-Control` | String | `no-store` |

### 4.3 Response Body — 검색 가능한 상품명을 추출한 경우

원문은 실제 음료 사진의 검증 응답을 바탕으로 작성되었다고 설명한다. `requestId`는 예시 값이다.

```json
{
  "message": "OCR 분석 완료",
  "data": {
    "requestId": "0123456789abcdef0123456789abcdef",
    "status": "OK",
    "productName": "어성초",
    "brand": "광동",
    "flavor": null,
    "totalWeight": "500ml",
    "searchKeyword": "어성초",
    "warnings": [],
    "evidence": {
      "productName": [
        { "sourceId": "t4", "text": "어성초" }
      ],
      "brand": [
        { "sourceId": "t3", "text": "광동" }
      ],
      "flavor": [],
      "totalWeight": [
        { "sourceId": "t8", "text": "500ml" }
      ]
    }
  }
}
```

### 4.4 Response Body 설명

| 필드명 | 타입 | 설명 |
| --- | --- | --- |
| `message` | String | 처리 결과 메시지 |
| `data` | Object | OCR 및 상품명 정보 추출 결과. 배열이 아님 |
| `data.requestId` | String | OCR 작업의 추적 ID. 응답 헤더의 백엔드 요청 ID와 별개 |
| `data.status` | String | `OK`, `NO_TEXT`, `NO_SEARCH_KEYWORD` 중 하나 |
| `data.productName` | String / null | 인식한 문구 중 선택·조합한 상품명. DB의 정식 상품명이나 상품 식별 ID를 의미하지 않음 |
| `data.brand` | String / null | 선택된 브랜드명. 제조사와 동일하다고 단정하지 않음 |
| `data.flavor` | String / null | 선택된 맛·유형 정보 |
| `data.totalWeight` | String / null | 단위가 포함된 중량·용량 문자열. `77g`, `500ml` 등이며 숫자형이 아님 |
| `data.searchKeyword` | String / null | 후속 검색에 사용할 상품명 문자열. 현재는 `productName`을 기준으로 생성하며 별도의 브랜드·용량을 자동으로 덧붙이지 않음 |
| `data.warnings` | String[] | 경고 목록. 현재 버전에서는 빈 배열 `[]` |
| `data.evidence` | Object | 각 추출 필드의 OCR 출처 정보. 읽은 문구가 없으면 빈 객체 `{}`일 수 있음 |
| `data.evidence.productName` | Object[] | 상품명 구성에 사용한 OCR 부분 문자열 목록 |
| `data.evidence.brand` | Object[] | 브랜드 구성에 사용한 OCR 부분 문자열 목록 |
| `data.evidence.flavor` | Object[] | 맛·유형 구성에 사용한 OCR 부분 문자열 목록 |
| `data.evidence.totalWeight` | Object[] | 중량·용량 구성에 사용한 OCR 부분 문자열 목록 |
| 각 출처 항목의 `sourceId` | String | 해당 요청 내 OCR 문구의 식별자. 상품 ID가 아니며 요청 간 동일성을 보장하지 않음 |
| 각 출처 항목의 `text` | String | 해당 필드에 실제로 사용한 OCR 부분 문자열 |

읽지 못하거나 선택하지 못한 정보는 `null`로 반환한다. `evidence`는 원문 선택 근거이며 인식 정확도나 상품 식별을 보증하지 않는다. 전체 OCR 문구 목록, bbox 좌표, 종합 confidence 점수는 이 응답에 포함하지 않는다.

### 4.5 검색어 생성 규칙

- `searchKeyword`는 길이 **2~100자**다.
- 한글·영문·숫자 등 **글자/숫자 문자가 2개 이상** 있어야 한다.
- 조건을 만족하지 않으면 `null`로 반환한다.
- 현재는 `productName`을 기준으로 생성하며, 별도의 브랜드·용량을 자동으로 덧붙이지 않는다.
- OCR 원문에 없는 철자 교정·번역·상품명 보충은 하지 않는다.

<a id="result-status"></a>
## 5. HTTP 200 응답 상태 구분

| `data.status` | 의미 | 프론트엔드 처리 |
| --- | --- | --- |
| `OK` | 검색 가능한 상품명을 추출함 | `searchKeyword`를 확인하여 후속 처리. 정확한 상품 식별 성공으로 간주하지 않음 |
| `NO_TEXT` | 인식된 글자 없음 | 재촬영 안내. 이 경우 LLM을 호출하지 않음 |
| `NO_SEARCH_KEYWORD` | 글자는 인식했지만 유효한 검색어를 만들지 못함 | 재촬영 또는 수동 입력 안내. 다른 추출 필드는 남아 있을 수 있음 |

**HTTP 200 여부만으로 성공 화면을 표시하지 말고 `data.status`도 확인한다.** `OK`인데 검색어가 없거나 비어 있다면 응답 이상으로 처리하고 이름을 임의로 생성하지 않는다.

### 5.1 글자를 찾지 못한 경우의 응답 예시

오류 HTTP 상태가 아니라 **`200 OK`와 `NO_TEXT`**를 반환한다.

```json
{
  "message": "OCR 분석 완료",
  "data": {
    "requestId": "0123456789abcdef0123456789abcdef",
    "status": "NO_TEXT",
    "productName": null,
    "brand": null,
    "flavor": null,
    "totalWeight": null,
    "searchKeyword": null,
    "warnings": [],
    "evidence": {}
  }
}
```

<a id="errors"></a>
## 6. Error

### 6.1 Error Response Body

아래는 **OCR 중계 코드가 생성하는 오류**의 공통 형식이다. 다른 API의 최상위 `status`·`message` 형식과 혼동하지 않는다.

```json
{
  "error": {
    "requestId": "01234567-89ab-cdef-0123-456789abcdef",
    "code": "UNAUTHORIZED",
    "message": "OCR request could not be completed; do not automatically retry."
  }
}
```

| 필드명 | 타입 | 설명 |
| --- | --- | --- |
| `error.requestId` | String | 백엔드 중계 요청의 추적 ID |
| `error.code` | String | 오류 종류. HTTP 상태와 함께 판단 |
| `error.message` | String | 공통 오류 안내. 현재 중계 오류에는 위 영문 메시지를 사용 |

### 6.2 HTTP 상태 및 오류 코드

| HTTP 상태 | 대표 `error.code` | 발생 조건 및 처리 |
| --- | --- | --- |
| `400 Bad Request` | `INVALID_REQUEST`, `OCR_UPSTREAM_REJECTED` | 잘못된 요청 길이 또는 OCR 서버가 거부한 요청 형식 |
| `401 Unauthorized` | `UNAUTHORIZED` | 팀 토큰 누락·불일치·헤더 중복. 회원 로그인 오류가 아님 |
| `405 Method Not Allowed` | `METHOD_NOT_ALLOWED` | 토큰 인증 후 POST가 아닌 메서드로 호출 |
| `408 Request Timeout` | `OCR_UPSTREAM_REJECTED` | OCR 서버의 업로드 수신 시간 제한 초과 |
| `413 Payload Too Large` | `IMAGE_TOO_LARGE`, `OCR_UPSTREAM_REJECTED` | 파일/전체 요청 크기 또는 이미지 픽셀 수 제한 초과 |
| `415 Unsupported Media Type` | `INVALID_CONTENT_TYPE`, `INVALID_IMAGE_TYPE`, `OCR_UPSTREAM_REJECTED` | multipart 요청 형식 오류, JPEG/PNG가 아님, MIME과 파일 내용 불일치 등 |
| `422 Unprocessable Entity` | `INVALID_REQUEST`, `INVALID_IMAGE`, `OCR_UPSTREAM_REJECTED` | `image` 누락·파일 수 오류·추가 폼 필드·빈 파일·손상 이미지 또는 OCR 입력 제한 위반 |
| `429 Too Many Requests` | `OCR_BUSY`, `RATE_LIMITED`, `OCR_UPSTREAM_REJECTED` | 다른 요청 처리 중이거나 요청 간격 제한 등. 자동 반복 요청 금지 |
| `502 Bad Gateway` | `OCR_UPSTREAM_FAILED`, `OCR_UPSTREAM_REJECTED`, `INVALID_OCR_RESPONSE` | OCR 서버 연결·처리 실패, 내부 인증 실패 또는 응답 JSON 형식 오류 등 |
| `503 Service Unavailable` | `OCR_DISABLED`, `OCR_NOT_CONFIGURED`, `OCR_INTERRUPTED`, `OCR_UPSTREAM_REJECTED` | 기능 비활성·미설정, 처리 중단, OCR 서버 미준비·자원 문제 또는 공유 유료 예산 소진 등 |
| `504 Gateway Timeout` | `OCR_TIMEOUT_OUTCOME_UNKNOWN`, `OCR_UPSTREAM_REJECTED` | 처리 시간 초과. 실행·과금 여부가 불명확할 수 있으므로 자동 재전송 금지 |

`OCR_UPSTREAM_REJECTED`는 OCR 서버가 정상 응답을 반환하지 않았다는 중계 오류다. 내부 오류 코드를 그대로 노출하지 않으므로 **같은 코드가 여러 HTTP 상태에서 나타날 수 있다.**

또한 multipart 파싱 단계, Spring 공통 예외 처리, 프록시·네트워크 등에서 발생한 오류는 위 JSON과 다르거나 본문이 없을 수 있다. 앱은 HTTP 상태와 JSON 파싱 실패도 처리해야 한다.

### 6.3 401 Unauthorized 예시

팀 토큰을 보내지 않았거나 올바르지 않은 경우다.

```json
{
  "error": {
    "requestId": "01234567-89ab-cdef-0123-456789abcdef",
    "code": "UNAUTHORIZED",
    "message": "OCR request could not be completed; do not automatically retry."
  }
}
```

### 6.4 429 Too Many Requests 예시

다른 OCR 요청을 처리 중인 경우다. **백엔드가 생성한 `OCR_BUSY`·`RATE_LIMITED` 응답**에는 `Retry-After: 3` 헤더가 포함된다. 3초 후 처리가 완료된다는 보장이나 자동 재시도 지시는 아니다.

```http
Retry-After: 3
```

```json
{
  "error": {
    "requestId": "01234567-89ab-cdef-0123-456789abcdef",
    "code": "OCR_BUSY",
    "message": "OCR request could not be completed; do not automatically retry."
  }
}
```

### 6.5 503 Service Unavailable 예시

OCR 서버가 요청을 처리할 수 없을 때의 예시다. 유료 호출 예산 소진도 현재 앱에는 이 상태와 코드로 전달된다. **이 응답만으로 예산 소진인지 서버 준비 문제인지 구분할 수 없다.**

```json
{
  "error": {
    "requestId": "01234567-89ab-cdef-0123-456789abcdef",
    "code": "OCR_UPSTREAM_REJECTED",
    "message": "OCR request could not be completed; do not automatically retry."
  }
}
```

<a id="frontend-notes"></a>
## 7. 프론트엔드 연동 시 주의사항

### 7.1 팀 테스트용 접속 경로

팀 테스트용 경로다. 요청 기기에서 Tailscale을 연결해야 한다. 내부 OCR 서버를 직접 호출하거나 OpenAI 키를 앱에 넣지 않는다.

### 7.2 사진 선택과 스캔 실행 분리

사진 선택만으로 요청하지 않고 사용자의 스캔 실행 동작에 따라 호출한다. 실행 중 버튼을 비활성화하고 화면 회전·재진입으로 요청이 중복되지 않도록 한다.

### 7.3 자동 재시도 금지와 대기시간

**자동 재시도를 사용하지 않는다.** 타임아웃이나 연결 종료 이후에도 서버에서 이미 처리했을 수 있다.

| 구분 | 원문 기준 값 | 의미 |
| --- | --- | --- |
| 클라이언트 대기시간 | 150초 이상 권장 | 예상 소요시간이나 성공 보장이 아님 |
| 백엔드 OCR 요청 제한 | 130초 | 현재 백엔드 설정 |
| 백엔드 전체 응답 대기 | 최대 135초 | 현재 백엔드 설정 |

### 7.4 서버 동시 처리 한도와 중복 요청

현재 백엔드는 **전체 OCR 요청을 동시 1건·최소 3초 간격**으로 제한한다. 앱이 같은 사진을 새 요청으로 다시 보내면 중복 처리될 수 있다. **이 API 자체는 요청 ID에 기반한 중복 제거를 제공하지 않는다.**

### 7.5 팀 공용 비용 한도

현재 AWS 스캐너의 유료 LLM 호출 한도는 **누적 1,000회**이며 일일·개인별 한도가 아니다. 비용 한도는 팀 전체가 공유한다.

- LLM 호출 직전에 차감한다.
- 실패·결과 불명 시도도 자동 환급하지 않는다.
- 잔여 횟수는 이 API 응답에 포함되지 않는다.
- 예산 소진에 따른 `503 / OCR_UPSTREAM_REJECTED`만으로는 서버 준비 문제와 구분할 수 없다.

### 7.6 nullable 필드와 응답 문구 처리

`null`, `[]`, `{}`를 허용하고, `totalWeight`와 요청 ID를 숫자로 변환하지 않는다. 응답 문구를 HTML이나 코드로 실행하지 않는다.

### 7.7 OCR 인식 결과와 정답 상품 구분

`OK`는 검색 가능한 문구를 추출했다는 뜻이며, DB 상품 식별이나 알레르기 안전 판정 성공을 의미하지 않는다. DB 검색 API 명세는 별도 문서를 따른다.

### 7.8 추적 ID와 문제 보고

| 위치 | 의미 |
| --- | --- |
| 정상 응답의 `data.requestId` | OCR 작업 ID |
| 응답 헤더의 `X-Request-ID` | 백엔드 중계 ID |
| 중계 오류 본문의 `error.requestId` | 백엔드 중계 요청의 추적 ID |

정상 응답의 두 요청 ID는 별개이므로 구분해서 남긴다. 테스트 시각·HTTP 상태·오류 코드와 함께 기록하되 인증 헤더나 개인정보는 남기지 않는다.

### 7.9 외부 전송

사진은 **AWS OCR 서버**로, 추출된 문구·좌표는 **OpenAI**로 전송된다. 불필요한 개인정보가 포함된 사진은 사용하지 않는다.
