# 색인 동기화를 트랜잭셔널 아웃박스로 — 개발 기록

OpenSearch 도입(`opensearch-keyword-search.md`) 때의 색인 동기화는 서비스가 저장 직후
색인 HTTP 를 직접 쏘고, 실패하면 로그만 남기는 best-effort 였다. 그 구조의 구멍 세 개를
트랜잭셔널 아웃박스로 닫은 기록이다. **로컬 검증 완료, 운영 반영 대기.**

## 바꾸기 전의 구멍 세 개

| 구멍 | 원인 |
|---|---|
| A. 유실 | 색인 쓰기 실패 시 로그만 남고 DB 는 커밋 — 그 글은 검색에서 영구 누락 |
| B. 유령 | 색인 쓰기는 커밋 전에 실행 — 색인 성공 후 트랜잭션이 롤백되면 DB 에 없는 문서가 색인에 잔류 |
| C. 경쟁 | 서로 다른 요청이 같은 글을 거의 동시에 수정하면 색인 도착 순서가 DB 커밋 순서와 어긋날 수 있음 |

## 구조

```
[전] 요청 스레드: DB 쓰기 → OpenSearch HTTP(실패=로그만) → 커밋

[후] 요청 스레드: DB 쓰기 → 아웃박스 행 INSERT → 커밋      ← DB 만 만진다
     폴러(@Scheduled 1초): PENDING 집기 → post_id 중복 접기
       → DB 에서 글 현재 상태 재조회
         ├─ 없거나 deleted → 색인 delete
         └─ 그 외          → 색인 upsert
       → 성공 DONE / 실패 retry_count++ + 지수 백오프 / 한도 초과 FAILED
```

핵심 설계는 **아웃박스에 post_id 만 담는 것**이다. "무엇을 하라"(수정 내용, index/delete
구분)를 안 적고 처리 시점에 현재 상태를 다시 읽으므로:
- 순서가 꼬여도 항상 최종 상태로 수렴한다 (구멍 C 소멸)
- 같은 글의 요청 여러 건을 한 번에 접을 수 있다
- 색인 upsert 가 문서 전체 덮어쓰기라 몇 번을 반영해도 안전(멱등) — at-least-once 와 궁합이 맞는다

소프트삭제 덕에 삭제된 글도 행이 남아 있어 "재조회로 판정" 이 가능하다.

구멍 A 는 재시도가(행이 PENDING 으로 남는다), B 는 트랜잭션 원자성이 닫는다 —
아웃박스 INSERT 가 게시글 저장과 같은 트랜잭션이라 롤백되면 요청 자체가 없던 일이 된다.

## 구현

| 무엇 | 내용 |
|---|---|
| `post_search_outbox` 테이블 | `post_id`, `status`(PENDING/DONE/FAILED), `retry_count`, `next_attempt_at` + BaseEntity 컬럼. DDL 은 `bench/sql/08_post_search_outbox.sql` |
| `api/domain/search/PostSearchOutbox` | 엔티티. `markDone/markFailed/retryLater` |
| `PostSearchOutboxRepository` 포트 | 규약대로 Jpa/InMemory 두 벌 + 테스트 fake |
| `api/service/search/PostSearchOutboxProcessor` | 폴러. `@Scheduled(fixedDelay=1000)` + `@Transactional`, 배치 100건, 백오프 1→2→4…최대 300초, 20회 초과 시 FAILED. 시간은 규약대로 `Clock` 주입 |
| 서비스 6곳 | `postSearchIndex.index/delete` 직접 호출 → 전부 `PostSearchOutbox.create(postId)` 저장으로 교체 (생성·발행·수정·마감·삭제·블라인드) |
| `PostRepository.findById` 추가 | 폴러가 소프트삭제된 글까지 봐야 해서. 포트+두 구현+fake 반영 |
| 에러 계약 반전 | `OpenSearchPostSearchIndex.index/delete` 가 예외를 삼키던 것을 **던지도록** 변경 — 실패를 알아야 폴러가 재시도한다. 단 delete 의 404(문서가 애초에 없음)는 성공으로 취급(멱등) |
| `SchedulingConfig` | `@EnableScheduling`. 블랙리스트 정리가 Caffeine TTL 로 대체된 뒤 사라졌던 스케줄러가 다른 용도(작업 큐 처리)로 다시 생겼다 — TTL 은 "만료된 것 치우기" 는 대체해도 "쌓인 일 처리하기" 는 대체하지 못한다 |

**지연 트레이드오프**: 커밋 → 폴링(≤1초) → refresh(≤1초)라 새 글의 검색 노출이 최대
~2초다(전에는 refresh 1초만). 실측으론 +1초 안에 노출됐다.

## 검증

유닛 테스트 7개(`PostSearchOutboxProcessorTest` — 활성/삭제/부재/중복 접기/재시도 지연/
복구 따라잡기/FAILED 격리, `MutableClock` 으로 시간 진행) + 서비스 테스트의 동기화 검증을
아웃박스 기록 확인으로 전환. 전체 빌드 통과.

로컬 E2E (100만 건 + OpenSearch, local 프로파일):

| 시나리오 | 결과 |
|---|---|
| 정상 경로 | 생성 → **+1초에 검색 노출**, 아웃박스 행 DONE(retry 0) |
| **장애 중 생성** | OpenSearch 를 내리고 글 작성 → 글쓰기는 200. 행이 PENDING 으로 남아 백오프(1·2·4초)로 재시도 누적(retry 3) → 컨테이너 복구 → **4번째 시도에 DONE, 검색 노출** |
| 장애 중 삭제 | 색인에 있던 글을 장애 중 삭제 → 복구 후 색인 문서 제거 확인 |
| 장애 중 생성+삭제 | 같은 글의 요청 2건이 한 번으로 접히고, 색인에 없는 문서 삭제(404)가 성공 처리되어 DONE. FAILED 0건 |

**이전 구조에서 영구 유실이던 시나리오("장애 중 생성")가 자동 복구되는 것**이 이 증분의
증명이다.

## 운영 반영 절차 — 순서가 중요하다

⚠️ **DDL 을 먼저, 배포는 나중.** 새 코드는 글을 쓸 때마다 `post_search_outbox` 에 INSERT
하므로, 테이블 없이 배포되면 **글쓰기 자체가 500** 이 난다(INSERT 실패 → 트랜잭션 롤백).

```bash
# 1. RDS 에 DDL 적용 (EC2 에서 — 구 코드는 이 테이블을 모르므로 미리 만들어도 무해)
mysql -h $DB_HOST -u $DB_USERNAME -p $DB_NAME < 08_post_search_outbox.sql
# 2. main 머지 → CI → CD 배포
# 3. 확인: 글 작성 → SELECT * FROM post_search_outbox ORDER BY id DESC LIMIT 3 (DONE 인지)
```

## 남은 것

- **DONE 행 청소 정책이 없다.** 쓰기 빈도가 낮아 당장 문제는 아니지만 무한히 쌓인다.
  주기 삭제(예: 7일 지난 DONE)를 폴러에 붙이는 게 자연스러운 다음 손질.
- **FAILED 행의 알림·재처리 도구가 없다.** 지금은 `log.error` 와 수동 UPDATE(status 를
  PENDING 으로 되돌리면 폴러가 다시 집는다)뿐이다.
- **멀티 인스턴스가 되면 중복 처리 가능.** 폴러가 여러 대면 같은 행을 동시에 집을 수 있다
  (`FOR UPDATE SKIP LOCKED` 등으로 풀어야). 반영해도 멱등이라 정확성은 유지되고, 현재는
  단일 인스턴스라 해당 없음 — 레이트리미터와 같은 종류의 명시된 한계.
