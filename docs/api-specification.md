# API 명세서

## 0. 공통 사항

### 0.1 Base URL
```
/api/v1
```

### 0.2 공통 응답 포맷 (`ApiResponse<T>`)
모든 응답은 성공/실패를 가리지 않고 다음 envelope 구조를 가진다.

```json
{
  "message": "string",
  "code": "string",
  "data": { /* T 또는 null */ }
}
```

| 필드      | 타입   | 설명                                         |
|---------|------|--------------------------------------------|
| message | string | 응답 메시지 (성공/에러 코드별 정의된 메시지)               |
| code    | string | 응답 코드 (성공: `SUCCESS`, 실패: 에러 코드 문자열)      |
| data    | T or null | 실제 응답 데이터. 데이터가 없을 경우 `null`             |

> 프론트에서는 **HTTP status로 성공/실패를 판단**하고, 분기 처리에는 `code`를 사용하면 된다.
> 실패 응답의 `data`는 항상 `null`이며, 검증 실패도 필드별 상세 없이 **에러 코드 1개**만 내려온다
> (검증 오류가 여러 개면 그중 하나만 반환).

### 0.3 성공 코드 (`SuccessCode`)
| Enum     | HTTP Status | code      | message   |
|----------|-------------|-----------|-----------|
| SUCCESS  | 200 OK      | `SUCCESS` | `success` |
| CREATED  | 201 Created | `SUCCESS` | `created` |

> 생성 API도 `code`는 동일하게 `SUCCESS`이고 `message`만 `created`다.

### 0.4 인증

#### Access Token (AT)
- 인증이 필요한 API는 요청 헤더에 JWT Access Token을 Bearer 방식으로 전달한다.
```
Authorization: Bearer {accessToken}
```
- AT는 **로그인(1.1)·재발급(1.2) 응답 body**로 내려온다. 기본 유효기간 **10분**(`jwt.access-token-expire-seconds`).
- 인증이 필요한 API에서 AT가 없거나 만료·위조·로그아웃된 토큰이면 **401 `INVALID_TOKEN`**.
- 인증은 되었으나 권한이 부족하면 **403 `ACCESS_DENIED`**.

#### Refresh Token (RT)
- RT는 응답 body에 노출되지 않고 **HttpOnly 쿠키**로만 전달된다. 기본 유효기간 **14일**(`jwt.refresh-token-expire-seconds`).

```
Set-Cookie: refresh_token={rt}; Max-Age={jwt.refresh-token-expire-seconds}; Path=/api/v1; HttpOnly; SameSite=Lax
```

| 속성 | 값 | 비고 |
|---|---|---|
| 쿠키명 | `refresh_token` | |
| HttpOnly | true | JS에서 읽을 수 없음 |
| Secure | **false** | 현재 HTTP 환경. HTTPS 전환 시 `Secure` + `SameSite=None`으로 변경 예정 |
| SameSite | `Lax` | |
| Path | `/api/v1` | 이 경로 하위 요청에만 쿠키가 전송됨 |

- 브라우저에서 호출할 때 **`credentials: 'include'`(axios는 `withCredentials: true`)** 를 설정해야
  로그인 응답의 쿠키가 저장되고, 재발급·로그아웃 요청에 쿠키가 실려나간다.
- **RTR(Refresh Token Rotation)**: 재발급(1.2) 성공 시마다 RT가 새로 발급되고 이전 RT는 폐기된다.
  이미 회전된 RT를 다시 제출하면 **재사용으로 간주해 해당 로그인 세션(family) 전체를 폐기**하며,
  이후 그 세션의 AT도 즉시 401이 된다.
- 보안상 RT 관련 실패는 사유를 구분하지 않고 모두 **401 `INVALID_REFRESH_TOKEN`** 으로 통일된다
  (미제출·만료·위조·재사용·탈퇴 회원 모두 동일). 이 응답을 받으면 **로그인 화면으로 보내면 된다.**

#### 입력 형식 규칙 (`@ValidNickname` / `@ValidPassword`)
| 대상 | 규칙 | 위반 시 코드 |
|---|---|---|
| nickname | 공백 없는 문자 1~10자 (`^\S{1,10}$`) | `INVALID_NICKNAME_FORMAT` |
| password | 영문·숫자·특수문자 각 1자 이상 포함, 8~20자 (`^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,20}$`) | `INVALID_PASSWORD_FORMAT` |

#### 인증 불필요 엔드포인트
`/api/v1/login`, `/api/v1/signup`, `/api/v1/reissue`, `/api/v1/logout` 이외의 모든 API는 AT가 필요하다.

#### 회원 상태와 401/404
인증은 통과했지만 토큰의 회원이 이미 탈퇴한 경우, **모든 인증 API가 404 `MEMBER_NOT_FOUND`** 를 반환할 수 있다.
(코드 값은 `MEMBER_NOT_FOUND`, HTTP는 404이므로 401과 구분해서 처리할 것.)

### 0.5 공통 에러 코드 (`CommonErrorCode`)
| Enum                       | HTTP Status               | code                         | 발생 상황 |
|----------------------------|---------------------------|------------------------------|-----------|
| INTERNAL_SERVER_ERROR      | 500 Internal Server Error | `INTERNAL_SERVER_ERROR`      | 처리되지 않은 서버 예외 |
| ALREADY_ASSIGNED_ID        | 500                       | `ALREADY_ASSIGNED_ID`        | 서버 내부 오류 |
| UNMAPPED_VALIDATION_ERROR  | 500                       | `UNMAPPED_VALIDATION_ERROR`  | 검증 메시지가 에러 코드에 매핑되지 않음(서버 버그) |
| HANDLER_NOT_FOUND          | 500                       | `HANDLER_NOT_FOUND`          | 서버 내부 오류 |
| INVALID_ENUM_VALUE         | 400 Bad Request           | `INVALID_ENUM_VALUE`         | enum에 정의되지 않은 값 전달 (**body·쿼리 파라미터 모두**) |
| INVALID_REQUEST_BODY       | 400                       | `INVALID_REQUEST_BODY`       | 타입 불일치 등 body 구조 오류 |
| MALFORMED_REQUEST_BODY     | 400                       | `MALFORMED_REQUEST_BODY`     | JSON 파싱 실패 / body 누락 |
| INVALID_PARAMETER_TYPE     | 400                       | `INVALID_PARAMETER_TYPE`     | 쿼리 파라미터 타입 불일치 (예: `cursor=abc`) |

### 0.6 인증 관련 에러 코드 (`AuthErrorCode`)
| code                    | HTTP | 발생 상황 |
|-------------------------|------|-----------|
| `LOGIN_FAILED`          | 401  | 로그인 실패 (이메일 미존재·비밀번호 불일치 동일 응답) |
| `INVALID_TOKEN`         | 401  | AT 없음/만료/위조/로그아웃·세션 폐기됨 |
| `INVALID_REFRESH_TOKEN` | 401  | RT 없음/만료/위조/재사용 감지 |
| `ACCESS_DENIED`         | 403  | 인증됐으나 권한 부족 |

---

## 1. Auth API

### 1.1 로그인
- **POST** `/api/v1/login`
- **인증** 불필요

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

| 필드     | 타입     | 필수 | 검증                              |
|--------|--------|----|---------------------------------|
| email    | string | O  | NotBlank, `@Email`             |
| password | string | O  | NotBlank                       |

**Response 200 OK**

RT 쿠키가 함께 내려온다: `Set-Cookie: refresh_token=…; Path=/api/v1; HttpOnly`

```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**에러**
| HTTP | code                |
|------|---------------------|
| 400  | `EMAIL_REQUIRED`, `INVALID_EMAIL_FORMAT`, `PASSWORD_REQUIRED` |
| 401  | `LOGIN_FAILED` (이메일 미존재·비밀번호 불일치 모두 동일 응답) |

---

### 1.2 토큰 재발급
- **POST** `/api/v1/reissue`
- **인증** 불필요 (AT 헤더 불필요). **RT 쿠키 필요** → `credentials: 'include'`
- **Request Body** 없음

AT가 만료되어 401 `INVALID_TOKEN`을 받으면 이 API로 새 AT를 받고 원 요청을 재시도하면 된다.
성공하면 **RT도 새 값으로 교체**되어 `Set-Cookie`로 다시 내려온다(RTR).

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**에러**
| HTTP | code                    |
|------|-------------------------|
| 401  | `INVALID_REFRESH_TOKEN` (쿠키 미전송·만료·위조·재사용·탈퇴 회원 전부 동일) |

> 401을 받으면 재시도하지 말고 로그인 화면으로 이동시킬 것.
> 동시 요청으로 재발급이 중복 호출되면 뒤늦게 도착한 이전 RT가 **재사용으로 감지되어 세션 전체가 폐기**되므로,
> 재발급 요청은 **한 번만 수행하고 나머지 요청은 그 결과를 기다리도록** 큐잉하는 것을 권장한다.

---

### 1.3 로그아웃
- **POST** `/api/v1/logout`
- **인증 불필요(permitAll)**. 단, 실제로 세션을 끊으려면 **AT 헤더와 RT 쿠키를 모두 함께 보내야** 한다.
- **Request Body** 없음

동작:
- 유효한 AT가 오면 해당 AT를 블랙리스트에 등록해 만료 전이라도 사용 불가로 만든다.
- RT 쿠키가 오면 그 RT가 속한 세션(family) 전체를 폐기한다.
- AT/RT가 없거나 이미 무효해도 **에러 없이 200**을 반환한다(멱등).
- 응답에 RT 쿠키 삭제용 `Set-Cookie: refresh_token=; Max-Age=0; Path=/api/v1`가 포함된다.

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": null
}
```

**에러** — 없음 (항상 200).

---

## 2. Member API

### 2.1 회원가입
- **POST** `/api/v1/signup`
- **인증** 불필요

**Request Body**
```json
{
  "email": "user@example.com",
  "nickname": "닉네임",
  "password": "Password1!",
  "passwordConfirm": "Password1!",
  "imageUrl": "https://cdn.example.com/profile/u1.png"
}
```

| 필드               | 타입   | 필수 | 검증                                       |
|------------------|------|----|------------------------------------------|
| email            | string | O | NotBlank, `@Email`                       |
| nickname         | string | O | NotBlank, `@ValidNickname`               |
| password         | string | O | NotBlank, `@ValidPassword`               |
| passwordConfirm  | string | O | NotBlank, `password`와 일치해야 함           |
| imageUrl         | string | O | NotBlank                                 |

> 이메일·닉네임 중복 검사는 **탈퇴한 회원까지 포함**해서 수행한다.
> 즉 탈퇴한 계정의 이메일·닉네임은 재사용할 수 없다.

**Response 201 Created**
```json
{
  "message": "created",
  "code": "SUCCESS",
  "data": null
}
```

**에러**
| HTTP | code                                                                |
|------|---------------------------------------------------------------------|
| 400  | `EMAIL_REQUIRED`, `INVALID_EMAIL_FORMAT`, `PASSWORD_REQUIRED`, `INVALID_PASSWORD_FORMAT`, `PASSWORD_CONFIRM_REQUIRED`, `PASSWORD_CONFIRM_MISMATCH`, `NICKNAME_REQUIRED`, `INVALID_NICKNAME_FORMAT`, `IMAGE_REQUIRED` |
| 409  | `EMAIL_DUPLICATED`, `NICKNAME_DUPLICATED`                            |

---

### 2.2 내 프로필 조회
- **GET** `/api/v1/profile`
- **인증** 필요

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "email": "user@example.com",
    "nickname": "닉네임",
    "imageUrl": "https://cdn.example.com/profile/u1.png"
  }
}
```

**에러**
| HTTP | code             |
|------|------------------|
| 401  | `INVALID_TOKEN`  |
| 404  | `MEMBER_NOT_FOUND` |

---

### 2.3 프로필 수정
- **PATCH** `/api/v1/profile`
- **인증** 필요
- PATCH지만 **부분 수정이 아니다.** `nickname`, `imageUrl` 모두 필수이며 전달한 값으로 덮어쓴다.

**Request Body**
```json
{
  "nickname": "새닉네임",
  "imageUrl": "https://cdn.example.com/profile/new.png"
}
```

| 필드       | 타입   | 필수 | 검증                        |
|----------|------|----|---------------------------|
| nickname | string | O | NotBlank, `@ValidNickname` |
| imageUrl | string | O | NotBlank                   |

> 닉네임을 **바꾸지 않은 경우(기존 값과 동일)** 에는 중복 검사를 하지 않는다.

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code |
|------|------|
| 400  | `NICKNAME_REQUIRED`, `INVALID_NICKNAME_FORMAT`, `IMAGE_REQUIRED` |
| 401  | `INVALID_TOKEN` |
| 404  | `MEMBER_NOT_FOUND` |
| 409  | `NICKNAME_DUPLICATED` |

---

### 2.4 비밀번호 변경
- **PATCH** `/api/v1/profile/pw`
- **인증** 필요

**Request Body**
```json
{
  "currentPassword": "Password1!",
  "password": "NewPassword1!",
  "passwordConfirm": "NewPassword1!"
}
```

| 필드            | 타입   | 필수 | 검증                        |
|-----------------|--------|----|---------------------------|
| currentPassword | string | O  | NotBlank (현재 비밀번호와 일치해야 함) |
| password        | string | O  | NotBlank, `@ValidPassword`  |
| passwordConfirm | string | O  | NotBlank, `password`와 일치     |

> **변경에 성공하면 해당 회원의 RT가 전부 폐기된다.** 기존 RT로는 재발급이 불가하므로
> (401 `INVALID_REFRESH_TOKEN`) 변경 후에는 재로그인을 유도해야 한다.
> 이미 발급된 AT는 만료 전까지 유효하다.
>
> 검증 순서상 `currentPassword` 불일치가 `passwordConfirm` 불일치보다 먼저 감지된다.

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code |
|------|------|
| 400  | `CURRENT_PASSWORD_REQUIRED`, `CURRENT_PASSWORD_MISMATCH`, `PASSWORD_REQUIRED`, `INVALID_PASSWORD_FORMAT`, `PASSWORD_CONFIRM_REQUIRED`, `PASSWORD_CONFIRM_MISMATCH` |
| 401  | `INVALID_TOKEN` |
| 404  | `MEMBER_NOT_FOUND` |

---

### 2.5 회원 탈퇴
- **DELETE** `/api/v1/profile`
- **인증** 필요

소프트 삭제로 처리된다. 탈퇴 후 이 회원이 쓴 글·댓글은 남고, 작성자 닉네임만 `"알수없음"`으로 표시된다.

> 탈퇴 시 토큰을 자동으로 폐기하지는 않으므로, 클라이언트에서 **탈퇴 직후 로그아웃(1.3)을 호출**해
> AT/RT를 정리하는 것을 권장한다. (탈퇴 후 남은 AT로 API를 호출하면 404 `MEMBER_NOT_FOUND`가 내려온다.)

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code             |
|------|------------------|
| 401  | `INVALID_TOKEN`  |
| 404  | `MEMBER_NOT_FOUND` |

---

## 3. Post API

> 모든 Post API는 **인증 필요**.
> 아래 각 API의 에러 표에서는 공통 응답인 401 `INVALID_TOKEN` / 404 `MEMBER_NOT_FOUND`를 생략한다.

### 3.1 게시글 목록 조회 (커서 페이지네이션)
- **GET** `/api/v1/posts?cursor={cursor}&size={size}`

**Query Parameters**
| 이름   | 타입    | 필수 | 기본값 | 설명                       |
|------|-------|----|-----|--------------------------|
| cursor | Long  | X  | -   | 직전 페이지의 `nextCursor`. 첫 페이지는 생략 |
| size   | Long  | X  | 10  | 한 페이지 항목 수. **1 이상 10 이하** |

- 최신순(`id DESC`) 정렬이며, `cursor`보다 작은 id부터 조회한다.
- `size`가 범위를 벗어나면 **400 `INVALID_PAGE_SIZE`**.

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "data": [
      {
        "postId": 12,
        "title": "같이 농구하실 분",
        "category": "EXERCISE",
        "meetingType": "OFFLINE",
        "address": {
          "sido": "서울특별시",
          "sigungu": "강남구",
          "eupmyeondong": "대치동",
          "detail": null
        },
        "placeName": "대치중학교 운동장",
        "recruitStatus": "RECRUITING",
        "capacity": 6,
        "likeCount": 3,
        "commentCount": 1,
        "viewCount": 100,
        "isEdited": false,
        "isBlind": false,
        "memberId": 7,
        "writerNickname": "닉네임",
        "createdAt": "2026-06-26T10:30:00"
      }
    ],
    "nextCursor": 12,
    "hasNext": true
  }
}
```

| 필드 | 설명 |
|---|---|
| data | 모집글 요약 목록 |
| nextCursor | 다음 페이지 요청에 쓸 커서. `hasNext`가 false면 `null` |
| hasNext | 다음 페이지 존재 여부 |

| 항목 필드 | 타입 | 설명 |
|---|---|---|
| category | enum | 모임 분류. 6.3 참고 |
| meetingType | enum | `ONLINE` / `OFFLINE`. 6.4 참고 |
| address | object \| null | 시도·시군구·읍면동·상세. **온라인 모임은 `null`** |
| placeName | string | 장소명 |
| recruitStatus | enum | `RECRUITING` / `CLOSED`. 6.5 참고 |
| capacity | int \| null | 모집 인원 |

- 블라인드 게시글: `isBlind = true`, `title`은 `"숨김 처리된 게시글"`로 마스킹된다.
  **`category`·`meetingType`·`address`·`placeName`·`recruitStatus`·`capacity`는 마스킹되지 않고 그대로 내려간다.**
- 탈퇴 회원의 게시글: `writerNickname`이 `"알수없음"` (`memberId`는 그대로 내려간다).

**에러**
| HTTP | code                |
|------|---------------------|
| 400  | `INVALID_PAGE_SIZE` |

---

### 3.2 게시글 검색
- **GET** `/api/v1/posts/search?keyword={keyword}&category={category}&cursor={cursor}&size={size}`

**Query Parameters**
| 이름 | 타입 | 필수 | 기본값 | 설명 |
|------|------|----|-----|------|
| keyword | string | **O** | - | 검색어. 비어 있거나 공백만 있으면 400 |
| category | enum | X | - | 모임 분류로 좁힌다. **생략하면 전체 분류가 대상**이다. 6.3 참고 |
| cursor | Long | X | - | 직전 페이지의 `nextCursor`. 첫 페이지는 생략 |
| size | Long | X | 10 | 한 페이지 항목 수. **1 이상 10 이하** |

- **제목 또는 본문**에 `keyword`가 포함된 모집글을 찾는다. **대소문자를 구분하지 않으며 부분 일치**다.
- 앞뒤 공백은 제거하고 검색한다.
- 정렬·페이지네이션은 3.1과 같다(`id DESC`, 커서 기반).
- 응답 형식은 **3.1 목록 조회와 완전히 동일**하다(같은 `PostSummaryPageResDto`).
- **블라인드 게시글(`isBlind = true`)은 검색 결과에서 제외된다.** 목록 조회(3.1)가 마스킹해서
  보여주는 것과 다르다. 본문이 가려진 글이 검색어에 걸리면 **매칭됐다는 사실 자체가 가려진 내용을
  노출**하기 때문이다. 따라서 검색 결과에는 `isBlind = true`인 항목이 나오지 않는다.
  (상세 조회 `GET /posts/{postId}`는 그대로 접근 가능하며 마스킹된 형태로 응답한다.)

**Response 200 OK**
- 3.1과 동일한 구조. 조건에 맞는 글이 없으면 `data: []`, `nextCursor: null`, `hasNext: false`.

**에러**
| HTTP | code                      | 상황 |
|------|---------------------------|---|
| 400  | `SEARCH_KEYWORD_REQUIRED` | `keyword` 누락 또는 공백뿐 |
| 400  | `INVALID_PAGE_SIZE`       | `size`가 1~10 범위 밖 |
| 400  | `INVALID_ENUM_VALUE`      | 정의되지 않은 `category` 값 |
| 400  | `INVALID_PARAMETER_TYPE`  | `cursor`가 숫자가 아님 |

---

### 3.3 게시글 상세 조회
- **GET** `/api/v1/posts/{postId}`

**Path Parameter**
- `postId` (Long): 게시글 ID

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "postId": 12,
    "title": "같이 농구하실 분",
    "content": "본문 …",
    "category": "EXERCISE",
    "meetingType": "OFFLINE",
    "address": {
      "sido": "서울특별시",
      "sigungu": "강남구",
      "eupmyeondong": "대치동",
      "detail": null
    },
    "placeName": "대치중학교 운동장",
    "recruitStatus": "RECRUITING",
    "capacity": 6,
    "likeCount": 3,
    "viewCount": 101,
    "memberId": 7,
    "writerNickname": "닉네임",
    "imageUrl": "https://cdn.example.com/post/12.png",
    "isMine": false,
    "isBlind": false,
    "isLikedByMe": true,
    "createdAt": "2026-06-26T10:30:00",
    "comments": [
      {
        "commentId": 50,
        "content": "댓글입니다",
        "memberId": 9,
        "writerNickname": "다른유저",
        "isMine": false,
        "isBlind": false,
        "createdAt": "2026-06-26T10:35:00"
      }
    ]
  }
}
```

**블라인드 게시글(`isBlind = true`)의 응답**
| 필드 | 값 |
|---|---|
| title | `"숨김 처리된 게시글"` |
| content | `"숨김 처리된 게시글"` |
| imageUrl | `null` |
| 그 외 | 정상 값 (모집 정보와 댓글도 그대로 내려감) |

> `category`·`meetingType`·`address`·`placeName`·`recruitStatus`·`capacity`는 블라인드돼도 마스킹되지 않는다.
> `address`는 **온라인 모임이면 `null`** 이다.

- 댓글은 최신순이 아닌 등록 순(`createdAt ASC`)으로 전부 내려오며 **페이지네이션이 없다.**
- 탈퇴 회원의 댓글은 `writerNickname`이 `"알수없음"`.
- 블라인드된 댓글(`isBlind = true`)은 `content`가 `"숨김 처리된 댓글"`로 마스킹된다.
  그 외 필드(`memberId`, `writerNickname`, `isMine`, `createdAt`)는 정상 값이다.

**조회수**
- 동일 회원 기준 **24시간에 1회만** `viewCount`가 증가한다(`PostViewLog`).
- 본인 글을 조회해도 증가한다.

**에러**
| HTTP | code              |
|------|-------------------|
| 404  | `POST_NOT_FOUND`  |

---

### 3.4 게시글 생성
- **POST** `/api/v1/posts`
- **레이트리밋 적용**: 회원당 **1분에 3건**. 초과 시 429 `POST_RATE_LIMIT_EXCEEDED`.
  (4.5 임시 저장 글 발행과 **동일한 카운터**를 공유한다.)

**Request Body**
```json
{
  "title": "같이 농구하실 분",
  "content": "본문",
  "imageUrl": "https://cdn.example.com/post/new.png",
  "category": "EXERCISE",
  "meetingType": "OFFLINE",
  "address": {
    "sido": "서울특별시",
    "sigungu": "강남구",
    "eupmyeondong": "대치동",
    "detail": "정문 앞"
  },
  "placeName": "대치중학교 운동장",
  "capacity": 6
}
```

| 필드 | 타입 | 필수 | 검증 |
|--------|------|----|---------------------------------------------|
| title | string | O | NotEmpty, 최대 30자 |
| content | string | O | NotEmpty |
| imageUrl | string | O | NotBlank |
| category | enum | O | NotNull. 6.3 참고 |
| meetingType | enum | O | NotNull. `ONLINE` / `OFFLINE` |
| address | object | 조건부 | **`meetingType`이 `OFFLINE`이면 필수**, `ONLINE`이면 생략 가능 |
| address.sido | string | O* | NotBlank (address를 보낼 때) |
| address.sigungu | string | O* | NotBlank (address를 보낼 때) |
| address.eupmyeondong | string | O* | NotBlank (address를 보낼 때) |
| address.detail | string | X | 상세 주소 |
| placeName | string | O | NotBlank, 최대 50자 |
| capacity | int | X | 보내면 양수여야 한다(Positive) |

> **`meetingType`이 `ONLINE`이면 `address`를 보내도 무시되고 `null`로 저장된다.**
> 오프라인인데 `address`가 없으면 400 `ADDRESS_REQUIRED_FOR_OFFLINE`.
> `recruitStatus`는 요청으로 받지 않는다. 생성 시 항상 `RECRUITING`으로 시작한다.

**Response 201 Created**
```json
{
  "message": "created",
  "code": "SUCCESS",
  "data": { "postId": 13 }
}
```

**에러**
| HTTP | code                                          |
|------|-----------------------------------------------|
| 400  | `TITLE_REQUIRED`, `TITLE_LENGTH_EXCEEDED`, `CONTENT_REQUIRED`, `IMAGE_REQUIRED` |
| 400  | `CATEGORY_REQUIRED`, `MEETING_TYPE_REQUIRED`, `ADDRESS_REQUIRED_FOR_OFFLINE` |
| 400  | `SIDO_REQUIRED`, `SIGUNGU_REQUIRED`, `EUPMYEONDONG_REQUIRED` |
| 400  | `PLACE_NAME_REQUIRED`, `PLACE_NAME_LENGTH_EXCEEDED`, `CAPACITY_POSITIVE` |
| 429  | `POST_RATE_LIMIT_EXCEEDED`                    |

---

### 3.5 게시글 수정
- **PATCH** `/api/v1/posts/{postId}`
- PATCH지만 **부분 수정이 아니다.** 필수 필드를 모두 보내야 하며 전달한 값으로 덮어쓴다.
- 수정하면 이전 내용이 수정 이력으로 보관되고, 목록의 `isEdited`가 `true`가 된다.

**Request Body**
```json
{
  "title": "수정된 제목",
  "content": "수정된 본문",
  "imageUrl": "https://cdn.example.com/post/13-edit.png",
  "category": "STUDY",
  "meetingType": "ONLINE",
  "address": null,
  "placeName": "디스코드",
  "capacity": 10
}
```

| 필드 | 타입 | 필수 | 검증 |
|--------|------|----|---------------------------------------------|
| title | string | O | NotEmpty, 최대 30자 |
| content | string | O | NotEmpty |
| imageUrl | string | O | NotEmpty |
| category | enum | O | NotNull |
| meetingType | enum | O | NotNull |
| address | object | 조건부 | `meetingType`이 `OFFLINE`이면 필수 |
| placeName | string | O | NotBlank, 최대 50자 |
| capacity | int | X | 보내면 양수여야 한다 |

> 검증 규칙은 3.4 게시글 생성과 동일하다(`imageUrl`만 NotBlank → NotEmpty).
> **`recruitStatus`는 수정할 수 없다.** 모집 마감 API는 아직 없다.
> 수정 이력에는 `title`·`content`·`imageUrl`만 보관되며, **모집 정보 변경 이력은 남지 않는다.**

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": { "postId": 13 }
}
```

**에러**
| HTTP | code                                         |
|------|----------------------------------------------|
| 400  | `TITLE_REQUIRED`, `TITLE_LENGTH_EXCEEDED`, `CONTENT_REQUIRED`, `IMAGE_REQUIRED`, `POST_IMAGE_REQUIRED` |
| 400  | `CATEGORY_REQUIRED`, `MEETING_TYPE_REQUIRED`, `ADDRESS_REQUIRED_FOR_OFFLINE` |
| 400  | `SIDO_REQUIRED`, `SIGUNGU_REQUIRED`, `EUPMYEONDONG_REQUIRED` |
| 400  | `PLACE_NAME_REQUIRED`, `PLACE_NAME_LENGTH_EXCEEDED`, `CAPACITY_POSITIVE` |
| 403  | `NOT_POST_WRITER`                            |
| 404  | `POST_NOT_FOUND`                             |

> `POST_IMAGE_REQUIRED`는 이미지가 없는 과거 게시글을 수정할 때 수정 이력 저장 단계에서 발생한다.

---

### 3.6 게시글 삭제
- **DELETE** `/api/v1/posts/{postId}`

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code               |
|------|--------------------|
| 403  | `NOT_POST_WRITER`  |
| 404  | `POST_NOT_FOUND`   |

---

### 3.7 게시글 좋아요
- **POST** `/api/v1/posts/{postId}/likes`

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": { "likeCount": 4, "isLikedByMe": true }
}
```

**에러**
| HTTP | code                  |
|------|-----------------------|
| 404  | `POST_NOT_FOUND`      |
| 409  | `POST_ALREADY_LIKED`  |

---

### 3.8 게시글 좋아요 취소
- **DELETE** `/api/v1/posts/{postId}/likes`

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": { "likeCount": 3, "isLikedByMe": false }
}
```

**에러**
| HTTP | code                    |
|------|-------------------------|
| 404  | `POST_NOT_FOUND`        |
| 409  | `POST_ALREADY_UNLIKED`  |

---

### 3.9 댓글 작성
- **POST** `/api/v1/posts/{postId}/comments`

**Request Body**
```json
{ "content": "댓글 내용" }
```

| 필드    | 타입   | 필수 | 검증      |
|-------|------|----|---------|
| content | string | O | NotEmpty |

**Response 201 Created**
```json
{
  "message": "created",
  "code": "SUCCESS",
  "data": { "commentId": 51 }
}
```

**에러**
| HTTP | code               |
|------|--------------------|
| 400  | `COMMENT_REQUIRED` |
| 404  | `POST_NOT_FOUND`   |

---

### 3.10 댓글 수정
- **PATCH** `/api/v1/posts/{postId}/comments/{commentId}`

**Request Body**
```json
{ "content": "수정된 댓글" }
```

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code                  |
|------|-----------------------|
| 400  | `COMMENT_REQUIRED`    |
| 403  | `NOT_COMMENT_WRITER`  |
| 404  | `POST_NOT_FOUND`, `COMMENT_NOT_FOUND` |

---

### 3.11 댓글 삭제
- **DELETE** `/api/v1/posts/{postId}/comments/{commentId}`

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code                  |
|------|-----------------------|
| 403  | `NOT_COMMENT_WRITER`  |
| 404  | `POST_NOT_FOUND`, `COMMENT_NOT_FOUND` |

---

## 4. PostDraft API (임시 저장 글)

> 모든 API **인증 필요**. Base path: `/api/v1/posts/drafts`
> 각 API의 에러 표에서 401 `INVALID_TOKEN` / 404 `MEMBER_NOT_FOUND`는 생략한다.

임시 저장 글은 `DRAFT` → (발행) → `PUBLISHED` 상태를 가진다.
**발행하는 순간 조회 대상에서 완전히 빠진다.** 목록(4.1)에 안 나오는 것은 물론이고,
상세 조회·수정·삭제·재발행(4.2·4.4·4.5·4.6) 모두 **404 `POST_DRAFT_NOT_FOUND`** 가 된다.
저장소가 `status = DRAFT` 인 것만 조회하기 때문이며, 따라서 **같은 draft가 두 번 발행될 수 없다.**
프론트에서는 발행 성공 시 해당 draftId를 더 이상 참조하지 않도록 처리하면 된다.

### 4.1 임시 저장 글 목록 조회
- **GET** `/api/v1/posts/drafts`
- 내 임시 저장 글 중 **아직 발행하지 않은 것만** 반환한다. 페이지네이션 없음.

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": [
    { "draftId": 5, "title": "작성중인 글" },
    { "draftId": 4, "title": null }
  ]
}
```

> `title`은 저장하지 않았으면 `null`일 수 있다.

---

### 4.2 임시 저장 글 상세 조회
- **GET** `/api/v1/posts/drafts/{draftId}`

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "draftId": 5,
    "title": "작성중인 글",
    "content": "초고...",
    "imageUrl": "https://cdn.example.com/draft/5.png"
  }
}
```

> `title`·`content`·`imageUrl`은 모두 `null`일 수 있다.

**에러**
| HTTP | code                       |
|------|----------------------------|
| 403  | `NOT_POST_DRAFT_WRITER`    |
| 404  | `POST_DRAFT_NOT_FOUND`     |

---

### 4.3 임시 저장 글 생성
- **POST** `/api/v1/posts/drafts`

**Request Body** (모든 필드 선택, `null` 허용)
```json
{
  "title": "초안 제목",
  "content": "초안 본문",
  "imageUrl": "https://cdn.example.com/draft/new.png"
}
```

| 필드     | 타입   | 필수 | 검증     |
|--------|------|----|--------|
| title    | string | X | 제약 없음 |
| content  | string | X | 제약 없음 |
| imageUrl | string | X | 제약 없음 |

**Response 201 Created**
```json
{
  "message": "created",
  "code": "SUCCESS",
  "data": { "draftId": 6 }
}
```

---

### 4.4 임시 저장 글 수정
- **PATCH** `/api/v1/posts/drafts/{draftId}`
- PATCH지만 **부분 수정이 아니다.** 생략한 필드는 `null`로 덮어쓰이므로, 항상 세 필드를 모두 보낼 것.

**Request Body**
```json
{
  "title": "수정된 초안",
  "content": "수정된 초안 본문",
  "imageUrl": "https://cdn.example.com/draft/5-edit.png"
}
```

| 필드   | 타입   | 필수 | 검증                                            |
|------|------|----|-----------------------------------------------|
| title  | string | X | 최대 30자 (`TITLE_LENGTH_EXCEEDED`)             |
| content| string | X | 제약 없음                                       |
| imageUrl| string | X | 제약 없음                                      |

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": { "draftId": 5 }
}
```

**에러**
| HTTP | code                       |
|------|----------------------------|
| 400  | `TITLE_LENGTH_EXCEEDED`    |
| 403  | `NOT_POST_DRAFT_WRITER`    |
| 404  | `POST_DRAFT_NOT_FOUND`     |

---

### 4.5 임시 저장 글 게시(발행)
- **POST** `/api/v1/posts/drafts/{draftId}/publish`
- **레이트리밋 적용**: 회원당 **1분에 3건** (3.4 게시글 생성과 카운터 공유). 초과 시 429.

임시 저장 글을 정식 게시글로 발행한다. 발행에 사용할 최종 본문은 **요청 body의 값**이며,
저장돼 있던 draft 내용은 사용하지 않는다. 검증 규칙은 3.4 게시글 생성과 동일하다.
발행에 성공하면 draft는 `PUBLISHED`가 되어 조회 대상에서 빠지므로,
**같은 draftId로 다시 발행하면 404 `POST_DRAFT_NOT_FOUND`** 가 된다(중복 게시 불가).

**Request Body**
- **3.4 게시글 생성과 동일한 body**(`PostCreateReqDto`)를 보낸다.

```json
{
  "title": "최종 제목",
  "content": "최종 본문",
  "imageUrl": "https://cdn.example.com/post/from-draft.png",
  "category": "STUDY",
  "meetingType": "OFFLINE",
  "address": {
    "sido": "서울특별시",
    "sigungu": "마포구",
    "eupmyeondong": "서교동",
    "detail": null
  },
  "placeName": "홍대입구 스터디카페",
  "capacity": 8
}
```

| 필드 | 타입 | 필수 | 검증 |
|--------|------|----|-----------------------|
| title | string | O | NotEmpty, 최대 30자 |
| content | string | O | NotEmpty |
| imageUrl | string | O | NotBlank |
| category | enum | O | NotNull |
| meetingType | enum | O | NotNull |
| address | object | 조건부 | `meetingType`이 `OFFLINE`이면 필수 |
| placeName | string | O | NotBlank, 최대 50자 |
| capacity | int | X | 보내면 양수여야 한다 |

> **임시 저장 글(`PostDraft`)에는 모집 정보 필드가 없다.** `category`·`meetingType`·`address`·
> `placeName`·`capacity`는 저장돼 있지 않으므로 **발행 요청에서 반드시 새로 지정**해야 한다.

**Response 201 Created**
```json
{
  "message": "created",
  "code": "SUCCESS",
  "data": { "postId": 14 }
}
```

**에러**
| HTTP | code                                                                       |
|------|----------------------------------------------------------------------------|
| 400  | `TITLE_REQUIRED`, `TITLE_LENGTH_EXCEEDED`, `CONTENT_REQUIRED`, `IMAGE_REQUIRED` |
| 400  | `CATEGORY_REQUIRED`, `MEETING_TYPE_REQUIRED`, `ADDRESS_REQUIRED_FOR_OFFLINE` |
| 400  | `SIDO_REQUIRED`, `SIGUNGU_REQUIRED`, `EUPMYEONDONG_REQUIRED` |
| 400  | `PLACE_NAME_REQUIRED`, `PLACE_NAME_LENGTH_EXCEEDED`, `CAPACITY_POSITIVE` |
| 403  | `NOT_POST_DRAFT_WRITER`                                                    |
| 404  | `POST_DRAFT_NOT_FOUND` (없는 draft **또는 이미 발행된 draft**)               |
| 429  | `POST_RATE_LIMIT_EXCEEDED`                                                 |

---

### 4.6 임시 저장 글 삭제
- **DELETE** `/api/v1/posts/drafts/{draftId}`
- 아직 발행하지 않은 draft만 삭제할 수 있다. 이미 발행했다면 404다(발행된 게시글은 3.6으로 삭제).

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code                       |
|------|----------------------------|
| 403  | `NOT_POST_DRAFT_WRITER`    |
| 404  | `POST_DRAFT_NOT_FOUND`     |

---

## 5. Report API

### 5.1 신고하기
- **POST** `/api/v1/report`
- **인증** 필요

**Request Body**
```json
{
  "targetId": 12,
  "targetType": "POST",
  "reportReason": "SPAM"
}
```

| 필드             | 타입                | 필수 | 값                                                              |
|----------------|-------------------|----|-------------------------------------------------------------------|
| targetId       | Long              | O  | 신고 대상 ID (미전달 시 `REPORT_TARGET_REQUIRED`)                   |
| targetType     | `TargetType` enum | O  | `POST`, `COMMENT` (미전달 시 `REPORT_TARGET_REQUIRED`)             |
| reportReason   | `ReportReason` enum | O | `SPAM`, `ABUSE`, `INAPPROPRIATE`, `ADVERTISEMENT`, `ETC`          |

- **자기 자신이 쓴 글·댓글은 신고할 수 없다** → 400 `SELF_REPORT_NOT_ALLOWED`.
- 동일 대상에 대해 같은 회원이 두 번 신고할 수 없다 → 409 `ALREADY_REPORTED`.
- 검증 순서: 대상 존재(404) → 자기 신고(400) → 중복 신고(409).

**Response 201 Created**
```json
{ "message": "created", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code                                            |
|------|-------------------------------------------------|
| 400  | `REPORT_TARGET_REQUIRED`, `REPORT_REASON_REQUIRED`, `SELF_REPORT_NOT_ALLOWED`, `INVALID_ENUM_VALUE` |
| 401  | `INVALID_TOKEN`                                 |
| 404  | `POST_NOT_FOUND`, `COMMENT_NOT_FOUND`, `MEMBER_NOT_FOUND` |
| 409  | `ALREADY_REPORTED`                              |

> 게시글·댓글 모두 누적 신고 **5회**에 도달하면 자동으로 블라인드 처리된다.
> 블라인드되면 응답의 `isBlind`가 `true`가 되고 본문이 마스킹된다(3.1·3.3 참고).

---

## 6. Enum 참조

### 6.1 `TargetType`
| 값       | 설명     |
|---------|--------|
| POST    | 게시글 신고 |
| COMMENT | 댓글 신고  |

### 6.2 `ReportReason`
| 값             | label          |
|---------------|----------------|
| SPAM          | 스팸             |
| ABUSE         | 욕설/비하          |
| INAPPROPRIATE | 부적절한 콘텐츠       |
| ADVERTISEMENT | 광고             |
| ETC           | 기타             |

### 6.3 `PostCategory`
| 값 | label |
|---|---|
| EXERCISE | 운동/스포츠 |
| STUDY | 스터디 |
| HOBBY | 취미 |
| GAME | 게임/오락 |
| FOOD | 맛집/모임 |
| VOLUNTEER | 봉사/나눔 |
| ETC | 기타 |

### 6.4 `MeetingType`
| 값 | label |
|---|---|
| ONLINE | 온라인 |
| OFFLINE | 오프라인 |

### 6.5 `RecruitStatus`
| 값 | label |
|---|---|
| RECRUITING | 모집중 |
| CLOSED | 모집완료 |

> 요청·응답 모두 **enum 이름**(`EXERCISE` 등)을 쓴다. label은 화면 표시용 참고값이며 API로 내려가지 않는다.
> 정의되지 않은 값을 보내면 400 `INVALID_ENUM_VALUE`.
> `RecruitStatus`는 생성 시 항상 `RECRUITING`이며, 현재 이를 `CLOSED`로 바꾸는 API는 없다.

> 요청에는 **enum 이름**(`SPAM` 등)을 그대로 보낸다. 정의되지 않은 값이면 400 `INVALID_ENUM_VALUE`.

---

## 7. 도메인별 에러 코드 요약

### MemberErrorCode
| code                     | HTTP |
|--------------------------|------|
| INVALID_EMAIL_FORMAT     | 400 |
| INVALID_PASSWORD_FORMAT  | 400 |
| INVALID_NICKNAME_FORMAT  | 400 |
| EMAIL_REQUIRED           | 400 |
| PASSWORD_REQUIRED        | 400 |
| CURRENT_PASSWORD_REQUIRED| 400 |
| NICKNAME_REQUIRED        | 400 |
| IMAGE_REQUIRED           | 400 |
| PASSWORD_CONFIRM_REQUIRED| 400 |
| PASSWORD_CONFIRM_MISMATCH| 400 |
| CURRENT_PASSWORD_MISMATCH| 400 |
| EMAIL_DUPLICATED         | 409 |
| NICKNAME_DUPLICATED      | 409 |
| MEMBER_NOT_FOUND         | 404 |

### PostErrorCode
| code                     | HTTP |
|--------------------------|------|
| POST_NOT_FOUND           | 404 |
| NOT_POST_WRITER          | 403 |
| POST_ALREADY_LIKED       | 409 |
| POST_ALREADY_UNLIKED     | 409 |
| POST_RATE_LIMIT_EXCEEDED | 429 |
| INVALID_PAGE_SIZE        | 400 |
| TITLE_REQUIRED           | 400 |
| CONTENT_REQUIRED         | 400 |
| POST_IMAGE_REQUIRED      | 400 |
| CATEGORY_REQUIRED        | 400 |
| MEETING_TYPE_REQUIRED    | 400 |
| ADDRESS_REQUIRED_FOR_OFFLINE | 400 |
| SIDO_REQUIRED            | 400 |
| SIGUNGU_REQUIRED         | 400 |
| EUPMYEONDONG_REQUIRED    | 400 |
| PLACE_NAME_REQUIRED      | 400 |
| PLACE_NAME_LENGTH_EXCEEDED | 400 |
| CAPACITY_POSITIVE        | 400 |

> 게시글 본문 검증에 쓰이는 `TITLE_LENGTH_EXCEEDED`(400)는 `PostDraftErrorCode`에 정의되어 있으나,
> **게시글 생성·수정·발행에서도 동일하게 반환**된다.

### SearchErrorCode
| code                     | HTTP |
|--------------------------|------|
| SEARCH_KEYWORD_REQUIRED  | 400 |

> 검색(3.2)은 `SearchController` / `SearchService` / `SearchRepository`로 Post와 분리되어 있다.
> `size` 검증 위반은 `PostErrorCode.INVALID_PAGE_SIZE`(400)를 그대로 쓴다.

### PostDraftErrorCode
| code                         | HTTP |
|------------------------------|------|
| POST_DRAFT_NOT_FOUND         | 404 |
| NOT_POST_DRAFT_WRITER        | 403 |
| TITLE_LENGTH_EXCEEDED        | 400 |

### CommentErrorCode
| code                | HTTP |
|---------------------|------|
| COMMENT_REQUIRED    | 400 |
| COMMENT_NOT_FOUND   | 404 |
| NOT_COMMENT_WRITER  | 403 |

### ReportErrorCode
| code                     | HTTP |
|--------------------------|------|
| REPORT_TARGET_REQUIRED   | 400 |
| REPORT_REASON_REQUIRED   | 400 |
| SELF_REPORT_NOT_ALLOWED  | 400 |
| ALREADY_REPORTED         | 409 |
