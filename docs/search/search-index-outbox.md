# 색인 동기화 아웃박스 전환

OpenSearch 도입 초기의 색인 동기화는 저장 직후 같은 스레드에서 색인 HTTP 를 직접 호출하고
실패하면 로그만 남기는 방식이었다. 이 구조의 결함 세 가지를 트랜잭셔널 아웃박스로 해소했다.
2026-08-23 운영 반영.

| 결함 | 원인 |
|---|---|
| 유실 | 색인 쓰기 실패 시 로그만 남고 DB 는 커밋 — 해당 글이 검색에서 영구 누락 |
| 유령 문서 | 색인 호출이 커밋 전에 실행 — 색인 성공 후 롤백되면 DB 에 없는 문서가 색인에 잔류 |
| 순서 역전 | 같은 글의 동시 수정 시 색인 도착 순서가 DB 커밋 순서와 어긋날 수 있음 |

## 구조

```
[전] 요청 스레드: DB 쓰기 → OpenSearch HTTP(실패 시 로그만) → 커밋

[후] 요청 스레드: DB 쓰기 → 아웃박스 행 INSERT → 커밋          (외부 호출 없음)
     폴러(@Scheduled 1초): PENDING 집기 → post_id 중복 접기
       → DB 에서 글 현재 상태 재조회
         ├─ 없거나 deleted → 색인 delete
         └─ 그 외          → 색인 upsert
       → 성공 DONE / 실패 retry_count++ · 지수 백오프 / 한도 초과 FAILED
```

아웃박스에는 post_id 만 저장한다. "무엇을 하라"(수정 내용, index/delete 구분)를 저장하지
않고 처리 시점에 현재 상태를 다시 읽으므로, 요청 순서가 어긋나도 항상 최종 상태로 수렴하고
같은 글의 요청 여러 건을 한 번으로 접을 수 있다. 색인 쓰기가 문서 전체를 덮어쓰는 upsert 라
반복 반영에도 안전하다(at-least-once + 멱등). 소프트삭제 덕에 삭제된 글도 행이 남아 있어
재조회 판정이 가능하다.

결함별 해소 방식 — 유실은 재시도(행이 PENDING 으로 남는다), 유령 문서는 트랜잭션 원자성
(아웃박스 INSERT 가 게시글 저장과 같은 트랜잭션이라 롤백 시 요청도 사라진다), 순서 역전은
처리 시점 재조회.

## 구현

| 무엇 | 내용 |
|---|---|
| `post_search_outbox` 테이블 | `post_id`, `status`(PENDING/DONE/FAILED), `retry_count`, `next_attempt_at`. DDL 은 `bench/sql/08_post_search_outbox.sql` (prod `ddl-auto: none` 이라 수동 적용) |
| `api/domain/search/PostSearchOutbox` | 엔티티. `markDone` / `markFailed` / `retryLater` |
| `PostSearchOutboxRepository` 포트 | 규약대로 Jpa/InMemory 두 구현 + 테스트 fake |
| `api/service/search/PostSearchOutboxProcessor` | 폴러. `@Scheduled(fixedDelay=1000)` + `@Transactional`, 배치 100건, 백오프 1→2→4…최대 300초, 20회 초과 시 FAILED. 시간은 `Clock` 주입 |
| 서비스 6곳 | 직접 색인 호출을 전부 아웃박스 INSERT 로 교체 (생성·발행·수정·마감·삭제·블라인드) |
| `PostRepository.findById` 추가 | 폴러가 소프트삭제된 글까지 조회해야 하므로 |
| 색인 구현 에러 계약 변경 | `index`/`delete` 가 예외를 삼키던 것을 던지도록 — 실패를 알아야 폴러가 재시도한다. 삭제의 404 는 성공으로 처리(멱등) |
| `SchedulingConfig` | `@EnableScheduling`. 스케줄러 용도는 이 폴러 하나 — 만료·정리는 여전히 TTL·요청 시점 계산으로 처리한다 |

트레이드오프: 새 글의 검색 노출이 refresh 1초에서 폴링(≤1초)+refresh(1초) 최대 ~2초로
늘었다. 실측으로는 +1초 안에 노출됐다.

## 검증

유닛 테스트 7개 — 활성/삭제/부재 글 처리, 같은 글 요청 중복 접기, 실패 시 재시도 지연
(백오프 전에는 재선택되지 않음), 복구 후 따라잡기, 재시도 한도 초과 시 FAILED 격리.
`MutableClock` 으로 시간을 진행시켜 확인했다.

로컬 장애 주입 E2E (100만 건 + OpenSearch):

| 시나리오 | 결과 |
|---|---|
| 정상 경로 | 생성 → +1초에 검색 노출, 행 DONE(retry 0) |
| 장애 중 생성 | OpenSearch 를 내리고 글 작성 → 글쓰기는 200. 백오프(1·2·4·8초)로 재시도 4회 실패 후, 복구되자 5번째 시도에 DONE·검색 노출 (최종 retry_count 4) |
| 장애 중 삭제 | 색인에 있던 글을 장애 중 삭제 → 복구 후 색인 문서 제거 |
| 장애 중 생성+삭제 | 같은 글의 요청 2건이 한 번으로 접히고, 색인에 없는 문서 삭제(404)가 성공 처리되어 DONE. FAILED 0건 |

장애 중 생성은 이전 구조에서 영구 유실이던 시나리오다.

## 남은 것

- DONE 행 청소 정책이 없다. 쓰기 빈도가 낮아 당장 문제는 아니지만 무한히 쌓인다 —
  주기 삭제(예: 7일 지난 DONE)가 자연스러운 다음 손질
- FAILED 행의 알림·재처리 도구가 없다. 현재는 로그와 수동 UPDATE(PENDING 으로 되돌리면
  폴러가 다시 집는다)뿐
- 폴러가 멀티 인스턴스가 되면 같은 행을 중복 처리할 수 있다(`FOR UPDATE SKIP LOCKED` 등
  필요). 멱등이라 정확성은 유지되며, 현재 단일 인스턴스에서는 해당 없음
