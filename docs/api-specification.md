# API 명세서

## 0. 공통 사항

### 0.1 Base URL
```
/api/v1
```

### 0.2 공통 응답 포맷 (`ApiResponse<T>`)
모든 응답은 다음과 같은 envelope 구조를 가진다.

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
| code    | string | 응답 코드 (성공: `SUCCESS`, 실패: `*_ERROR_CODE`) |
| data    | T or null | 실제 응답 데이터. 데이터가 없을 경우 `null`             |

### 0.3 성공 코드 (`SuccessCode`)
| Enum     | HTTP Status | code      | message   |
|----------|-------------|-----------|-----------|
| SUCCESS  | 200 OK      | `SUCCESS` | `success` |
| CREATED  | 201 Created | `SUCCESS` | `created` |

### 0.4 인증
- 인증이 필요한 API는 요청 헤더에 JWT Access Token을 Bearer 방식으로 전달해야 한다.
```
Authorization: Bearer {accessToken}
```
- `@LoginMember`가 붙은 컨트롤러 메서드는 모두 인증 필요.

### 0.5 공통 에러 코드
| Enum                       | HTTP Status               | code                         |
|----------------------------|---------------------------|------------------------------|
| INTERNAL_SERVER_ERROR      | 500 Internal Server Error | `INTERNAL_SERVER_ERROR`      |
| ALREADY_ASSIGNED_ID        | 500                       | `ALREADY_ASSIGNED_ID`        |
| UNMAPPED_VALIDATION_ERROR  | 500                       | `UNMAPPED_VALIDATION_ERROR`  |
| HANDLER_NOT_FOUND          | 500                       | `HANDLER_NOT_FOUND`          |
| INVALID_ENUM_VALUE         | 400 Bad Request           | `INVALID_ENUM_VALUE`         |
| INVALID_REQUEST_BODY       | 400                       | `INVALID_REQUEST_BODY`       |
| MALFORMED_REQUEST_BODY     | 400                       | `MALFORMED_REQUEST_BODY`     |

### 0.6 인증 관련 에러 코드 (`AuthErrorCode`)
| code                | HTTP |
|---------------------|------|
| `MEMBER_NOT_FOUND`  | 401  |
| `PASSWORD_MISMATCH` | 401  |
| `TOKEN_EMPTY`       | 401  |
| `INVALID_TOKEN`     | 401  |
| `TOKEN_BLACKLISTED` | 401  |

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
| email    | string | O  | `@Email`, NotBlank             |
| password | string | O  | NotBlank                       |

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
| HTTP | code                |
|------|---------------------|
| 400  | `EMAIL_REQUIRED`, `INVALID_EMAIL_FORMAT`, `PASSWORD_REQUIRED` |
| 401  | `MEMBER_NOT_FOUND`, `PASSWORD_MISMATCH` |

---

### 1.2 로그아웃
- **POST** `/api/v1/logout`
- **인증** 필요 (`Authorization: Bearer …`)
- **Request Body** 없음

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": null
}
```

**에러**
| HTTP | code |
|------|------|
| 401  | `TOKEN_EMPTY`, `INVALID_TOKEN`, `TOKEN_BLACKLISTED` |

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

| 필드               | 검증                                                  |
|------------------|-----------------------------------------------------|
| email            | NotBlank, `@Email`                                  |
| nickname         | NotBlank, `@ValidNickname`                          |
| password         | NotBlank, `@ValidPassword`                          |
| passwordConfirm  | NotBlank                                            |
| imageUrl         | NotBlank                                            |

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

---

### 2.3 프로필 수정
- **PATCH** `/api/v1/profile`
- **인증** 필요

**Request Body**
```json
{
  "nickname": "새닉네임",
  "imageUrl": "https://cdn.example.com/profile/new.png"
}
```

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code |
|------|------|
| 400  | `NICKNAME_REQUIRED`, `INVALID_NICKNAME_FORMAT`, `IMAGE_REQUIRED` |
| 409  | `NICKNAME_DUPLICATED` |

---

### 2.4 비밀번호 변경
- **PATCH** `/api/v1/profile/pw`
- **인증** 필요

**Request Body**
```json
{
  "password": "NewPassword1!",
  "passwordConfirm": "NewPassword1!"
}
```

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code |
|------|------|
| 400  | `PASSWORD_REQUIRED`, `INVALID_PASSWORD_FORMAT`, `PASSWORD_CONFIRM_REQUIRED`, `PASSWORD_CONFIRM_MISMATCH` |

---

### 2.5 회원 탈퇴
- **DELETE** `/api/v1/profile`
- **인증** 필요

**Response 200 OK**
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

---

## 3. Post API

> 모든 Post API는 인증 필요 (`@LoginMember`).

### 3.1 게시글 목록 조회 (커서 페이지네이션)
- **GET** `/api/v1/posts?cursor={cursor}&size={size}`

**Query Parameters**
| 이름   | 타입    | 필수 | 기본값 | 설명                       |
|------|-------|----|-----|--------------------------|
| cursor | Long  | X  | -   | 직전 페이지 마지막 postId         |
| size   | Long  | X  | 10  | 한 페이지 항목 수               |

**Response 200 OK**
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "data": [
      {
        "postId": 12,
        "title": "예시 게시글",
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

블라인드 게시글의 경우 `title`은 `"숨김 처리된 게시글"`, 탈퇴 회원의 게시글은 `writerNickname`이 `"알수없음"`으로 반환된다.

---

### 3.2 게시글 상세 조회
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
    "title": "예시 게시글",
    "content": "본문 …",
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
        "createdAt": "2026-06-26T10:35:00"
      }
    ]
  }
}
```

**에러**
| HTTP | code              |
|------|-------------------|
| 404  | `POST_NOT_FOUND`  |
| 429  | `POST_RATE_LIMIT_EXCEEDED` |

> 24시간 단위로 동일 사용자에게 1회만 viewCount가 증가 (`PostViewLog` 기반).

---

### 3.3 게시글 생성
- **POST** `/api/v1/posts`

**Request Body**
```json
{
  "title": "제목",
  "content": "본문",
  "imageUrl": "https://cdn.example.com/post/new.png"
}
```

| 필드     | 검증                                          |
|--------|---------------------------------------------|
| title    | NotEmpty, 최대 30자 (`TITLE_MAX_LENGTH`)       |
| content  | NotEmpty                                    |
| imageUrl | NotBlank                                    |

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

---

### 3.4 게시글 수정
- **PATCH** `/api/v1/posts/{postId}`

**Request Body**
```json
{
  "title": "수정된 제목",
  "content": "수정된 본문",
  "imageUrl": "https://cdn.example.com/post/13-edit.png"
}
```

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
| 400  | `TITLE_REQUIRED`, `TITLE_LENGTH_EXCEEDED`, `CONTENT_REQUIRED`, `IMAGE_REQUIRED` |
| 403  | `NOT_POST_WRITER`                            |
| 404  | `POST_NOT_FOUND`                             |

---

### 3.5 게시글 삭제
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

### 3.6 게시글 좋아요
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

### 3.7 게시글 좋아요 취소
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

### 3.8 댓글 작성
- **POST** `/api/v1/posts/{postId}/comments`

**Request Body**
```json
{ "content": "댓글 내용" }
```

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

### 3.9 댓글 수정
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

### 3.10 댓글 삭제
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

> 모든 API 인증 필요. Base path: `/api/v1/posts/drafts`

### 4.1 임시 저장 글 목록 조회
- **GET** `/api/v1/posts/drafts`

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

**에러**
| HTTP | code                       |
|------|----------------------------|
| 403  | `NOT_POST_DRAFT_WRITER`    |
| 404  | `POST_DRAFT_NOT_FOUND`     |

---

### 4.3 임시 저장 글 생성
- **POST** `/api/v1/posts/drafts`

**Request Body** (모든 필드 nullable)
```json
{
  "title": "초안 제목",
  "content": "초안 본문",
  "imageUrl": "https://cdn.example.com/draft/new.png"
}
```

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

**Request Body** (필드 모두 선택)
```json
{
  "title": "수정된 초안",
  "content": "수정된 초안 본문",
  "imageUrl": "https://cdn.example.com/draft/5-edit.png"
}
```

| 필드   | 검증                                            |
|------|-----------------------------------------------|
| title  | 최대 30자 (`TITLE_LENGTH_EXCEEDED`)              |
| content| 제약 없음                                       |
| imageUrl| 제약 없음                                      |

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

임시 저장 글을 정식 게시글로 발행한다. 발행에 사용할 최종 본문은 `PostCreateReqDto`로 전달한다.

**Request Body**
```json
{
  "title": "최종 제목",
  "content": "최종 본문",
  "imageUrl": "https://cdn.example.com/post/from-draft.png"
}
```

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
| 403  | `NOT_POST_DRAFT_WRITER`                                                    |
| 404  | `POST_DRAFT_NOT_FOUND`                                                     |

---

### 4.6 임시 저장 글 삭제
- **DELETE** `/api/v1/posts/drafts/{draftId}`

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

| 필드             | 타입                | 값                                                                |
|----------------|-------------------|-------------------------------------------------------------------|
| targetId       | Long              | 신고 대상 ID                                                       |
| targetType     | `TargetType` enum | `POST`, `COMMENT`                                                |
| reportReason   | `ReportReason` enum | `SPAM`, `ABUSE`, `INAPPROPRIATE`, `ADVERTISEMENT`, `ETC`         |

**Response 201 Created**
```json
{ "message": "created", "code": "SUCCESS", "data": null }
```

**에러**
| HTTP | code                                            |
|------|-------------------------------------------------|
| 400  | `REPORT_TARGET_REQUIRED`, `REPORT_REASON_REQUIRED`, `SELF_REPORT_NOT_ALLOWED`, `INVALID_ENUM_VALUE` |
| 404  | `POST_NOT_FOUND`, `COMMENT_NOT_FOUND`           |
| 409  | `ALREADY_REPORTED`                              |

> 동일 게시글이 누적 신고 임계치(`BLIND_THRESHOLD = 5`)에 도달하면 자동 블라인드 처리된다.

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
| NICKNAME_REQUIRED        | 400 |
| IMAGE_REQUIRED           | 400 |
| PASSWORD_CONFIRM_REQUIRED| 400 |
| PASSWORD_CONFIRM_MISMATCH| 400 |
| EMAIL_DUPLICATED         | 409 |
| NICKNAME_DUPLICATED      | 409 |

### PostErrorCode
| code                     | HTTP |
|--------------------------|------|
| POST_NOT_FOUND           | 404 |
| NOT_POST_WRITER          | 403 |
| POST_ALREADY_LIKED       | 409 |
| POST_ALREADY_UNLIKED     | 409 |
| POST_RATE_LIMIT_EXCEEDED | 429 |
| TITLE_REQUIRED           | 400 |
| CONTENT_REQUIRED         | 400 |

### PostDraftErrorCode
| code                   | HTTP |
|------------------------|------|
| POST_DRAFT_NOT_FOUND   | 404 |
| NOT_POST_DRAFT_WRITER  | 403 |
| TITLE_LENGTH_EXCEEDED  | 400 |

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
