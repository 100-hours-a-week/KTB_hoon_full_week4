# Postman 테스트 데이터

`spring.profiles.active=local` 으로 실행하면 `DataInitializer`가 시드 데이터를 채워줍니다.
모든 보호된 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다. 토큰은 `/api/v1/login` 응답의 `accessToken` 값을 사용하세요.

- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- 시드 비밀번호: `Password1!` (시드 회원 4명) / `Test1234!` (donghoon)

## 시드 회원

| memberId | email                   | nickname | password    |
|---------:|-------------------------|----------|-------------|
| 1        | alice@example.com       | 앨리스   | Password1!  |
| 2        | bob@example.com         | 밥       | Password1!  |
| 3        | carol@example.com       | 캐롤     | Password1!  |
| 4        | dave@example.com        | 데이브   | Password1!  |
| 5        | leedonghoon@example.com | donghoon | Test1234!   |

> 게시글 ID는 1~6, 댓글 ID는 1~10이 시드 데이터로 생성됩니다.

## Postman Environment 변수 예시

| Key            | Value                                  |
|----------------|----------------------------------------|
| `baseUrl`      | `http://localhost:8080`                |
| `accessToken`  | (로그인 후 자동 세팅)                  |
| `postId`       | `1`                                    |
| `commentId`    | `1`                                    |

로그인 요청의 "Tests" 탭에 아래 스크립트를 넣어두면 `accessToken`이 환경에 자동 저장됩니다.

```javascript
const body = pm.response.json();
pm.environment.set("accessToken", body.data.accessToken);
```

---

## 1. 인증

### 1-1. 로그인 — `POST /api/v1/login`

성공 요청
```json
{
  "email": "leedonghoon@example.com",
  "password": "Test1234!"
}
```
200 응답
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": { "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
}
```

실패 시나리오
```json
{ "email": "leedonghoon@example.com", "password": "WrongPw1!" }
```
```json
{ "email": "", "password": "Test1234!" }
```

### 1-2. 로그아웃 — `POST /api/v1/logout`

- Header: `Authorization: Bearer {{accessToken}}`
- Body: 없음

200 응답
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

---

## 2. 회원

### 2-1. 회원가입 — `POST /api/v1/signup`

성공 요청
```json
{
  "email": "newbie@example.com",
  "nickname": "뉴비",
  "password": "Newbie123!",
  "passwordConfirm": "Newbie123!",
  "imageUrl": "https://cdn.example.com/profile.jpg"
}
```
201 응답
```json
{ "message": "created", "code": "SUCCESS", "data": null }
```

실패 시나리오
```json
{
  "email": "alice@example.com",
  "nickname": "중복이메일",
  "password": "Newbie123!",
  "passwordConfirm": "Newbie123!",
  "imageUrl": "https://cdn.example.com/profile.jpg"
}
```
```json
{
  "email": "잘못된형식",
  "nickname": "포맷오류",
  "password": "Newbie123!",
  "passwordConfirm": "Newbie123!",
  "imageUrl": "https://cdn.example.com/profile.jpg"
}
```
```json
{
  "email": "fail@example.com",
  "nickname": "비번불일치",
  "password": "Newbie123!",
  "passwordConfirm": "Mismatch9!",
  "imageUrl": "https://cdn.example.com/profile.jpg"
}
```

### 2-2. 회원 프로필 조회 — `GET /api/v1/profile`

- Header: `Authorization: Bearer {{accessToken}}`

200 응답
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "email": "leedonghoon@example.com",
    "nickname": "donghoon",
    "imageUrl": "https://cdn.example.com/profile.jpg"
  }
}
```

### 2-3. 회원 프로필 수정 — `PATCH /api/v1/profile`

성공 요청
```json
{
  "nickname": "donghoon2",
  "imageUrl": "https://cdn.example.com/profile-v2.jpg"
}
```
200 응답
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

실패 시나리오 (닉네임 중복)
```json
{
  "nickname": "앨리스",
  "imageUrl": "https://cdn.example.com/profile.jpg"
}
```

### 2-4. 비밀번호 변경 — `PATCH /api/v1/profile/pw`

성공 요청
```json
{
  "password": "Newpass1!",
  "passwordConfirm": "Newpass1!"
}
```

실패 시나리오 (확인 불일치)
```json
{
  "password": "Newpass1!",
  "passwordConfirm": "Other222!"
}
```

### 2-5. 회원 탈퇴 — `DELETE /api/v1/profile`

- Header: `Authorization: Bearer {{accessToken}}`
- Body: 없음

200 응답
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

---

## 3. 게시글

### 3-1. 메인화면 조회 — `GET /api/v1/posts?cursor=&size=10`

- Header: `Authorization: Bearer {{accessToken}}`
- Query
  - `cursor` : 미지정 시 첫 페이지. 응답의 `createdAt_postId` 조합을 다음 호출에 사용
  - `size`   : 기본 10

200 응답
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": [
    {
      "postId": 6,
      "title": "테스트 코드, 어디까지 작성하나요?",
      "likeCount": 0,
      "commentCount": 0,
      "viewCount": 0,
      "isEdited": false,
      "isBlind": false,
      "writerId": 2,
      "writerNickname": "밥",
      "createdAt": "2026-06-07T10:00:00"
    }
  ]
}
```

### 3-2. 상세 조회 — `GET /api/v1/posts/{postId}`

- 예: `GET /api/v1/posts/1`

200 응답
```json
{
  "message": "success",
  "code": "SUCCESS",
  "data": {
    "postId": 1,
    "title": "스프링 부트로 시작하는 백엔드",
    "content": "오늘은 스프링 부트 프로젝트를 처음 세팅해봤어요. 의존성 설정이 생각보다 간단했습니다.",
    "likeCount": 0,
    "viewCount": 1,
    "writerId": 1,
    "writerNickname": "앨리스",
    "imageUrl": "https://picsum.photos/seed/post1/600/400",
    "isMine": false,
    "isBlind": false,
    "isLikedByMe": false,
    "createdAt": "2026-06-07T10:00:00",
    "comments": [
      { "commentId": 1, "content": "저도 어제 세팅했는데 한 번에 잘 되더라구요!", "writerId": 2, "writerNickname": "밥", "isMine": false, "createdAt": "2026-06-07T10:00:01" }
    ]
  }
}
```

실패 시나리오: `GET /api/v1/posts/9999` → 404 `POST_NOT_FOUND`

### 3-3. 게시글 생성 — `POST /api/v1/posts`

성공 요청
```json
{
  "title": "내 첫 게시글",
  "content": "안녕하세요, 가입했어요!",
  "imageUrl": "https://picsum.photos/seed/new/600/400"
}
```

실패 시나리오
```json
{ "title": "", "content": "내용만 있음", "imageUrl": "https://picsum.photos/seed/x/600/400" }
```
```json
{ "title": "잘못된 URL", "content": "내용", "imageUrl": "not-a-url" }
```

### 3-4. 게시글 수정 — `PATCH /api/v1/posts/{postId}`

성공 요청 (본인 글에 대해)
```json
{
  "title": "수정된 제목",
  "content": "수정된 본문",
  "imageUrl": "https://picsum.photos/seed/edit/600/400"
}
```
200 응답
```json
{ "message": "success", "code": "SUCCESS", "data": { "postId": 1 } }
```

실패: 타인 글 수정 → 401 `NOT_POST_WRITER`

### 3-5. 게시글 삭제 — `DELETE /api/v1/posts/{postId}`

- Body: 없음

200 응답
```json
{ "message": "success", "code": "SUCCESS", "data": null }
```

실패: 타인 글 삭제 → 401 `NOT_POST_WRITER`, 없는 글 → 404 `POST_NOT_FOUND`

---

## 4. 게시글 좋아요

### 4-1. 좋아요 — `POST /api/v1/posts/{postId}/likes`

- Body: 없음

200 응답
```json
{ "message": "success", "code": "SUCCESS", "data": { "likeCount": 1, "isLikedByMe": true } }
```

실패: 이미 좋아요 → 409 `POST_ALREADY_LIKED`

### 4-2. 좋아요 취소 — `DELETE /api/v1/posts/{postId}/likes`

- Body: 없음

200 응답
```json
{ "message": "success", "code": "SUCCESS", "data": { "likeCount": 0, "isLikedByMe": false } }
```

실패: 좋아요한 적 없음 → 409 `POST_ALREADY_LIKED`

---

## 5. 댓글

### 5-1. 댓글 생성 — `POST /api/v1/posts/{postId}/comments`

성공 요청
```json
{ "content": "test content" }
```
201 응답
```json
{
  "message": "comment_created_success",
  "code": "SUCCESS",
  "data": { "commentId": 11 }
}
```

실패 시나리오
```json
{ "content": "" }
```
→ 400 `COMMENT_REQUIRED`

`POST /api/v1/posts/9999/comments` → 404 `POST_NOT_FOUND`

### 5-2. 댓글 수정 — `PATCH /api/v1/posts/{postId}/comments/{commentId}`

성공 요청 (본인 댓글)
```json
{ "content": "수정된 댓글 내용" }
```
200 응답
```json
{ "message": "comment_updated_success", "code": "SUCCESS", "data": null }
```

실패 시나리오
- 빈 본문 → 400 `COMMENT_REQUIRED`
- 타인 댓글 → 403 `NOT_COMMENT_WRITER`
- 존재하지 않는 댓글 → 404 `COMMENT_NOT_FOUND`
- 존재하지 않는 게시글 → 404 `POST_NOT_FOUND`

### 5-3. 댓글 삭제 — `DELETE /api/v1/posts/{postId}/comments/{commentId}`

- Body: 없음

200 응답
```json
{ "message": "comment_deleted_success", "code": "SUCCESS", "data": null }
```

실패 시나리오
- 타인 댓글 → 403 `NOT_COMMENT_WRITER`
- 존재하지 않는 댓글 → 404 `COMMENT_NOT_FOUND`
- 존재하지 않는 게시글 → 404 `POST_NOT_FOUND`

---

## 6. 추천 테스트 시나리오

1. `POST /login` (donghoon) → accessToken 저장
2. `GET /profile` → 본인 정보 확인
3. `GET /posts?size=5` → 시드 게시글 확인 (`postId` 메모)
4. `GET /posts/1` → 상세 + 댓글 확인
5. `POST /posts/1/likes` → 좋아요 (likeCount=1)
6. `POST /posts/1/likes` → **409 POST_ALREADY_LIKED** 확인
7. `POST /posts/1/comments` `{ "content": "test content" }` → 201 + commentId 메모
8. `PATCH /posts/1/comments/{commentId}` `{ "content": "수정" }` → 200
9. `PATCH /posts/1/comments/1` (타인 댓글) → **403 NOT_COMMENT_WRITER**
10. `DELETE /posts/1/comments/{commentId}` → 200
11. `POST /posts` 신규 작성 → 본인 글 수정/삭제까지 확인
12. `POST /logout` → 이후 보호 API 호출 시 401 확인
