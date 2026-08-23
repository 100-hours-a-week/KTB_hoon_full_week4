# OpenSearch 로 키워드 검색 개선 — 개발 기록

키워드 검색이 매치가 많은 검색어에서 수십 초 걸리는 문제
(`fulltext-search-experiment.md`)를 OpenSearch 도입으로 푼 기록이다.
**운영 반영 완료(2026-08-23).** 같은 날 같은 요청으로 잰 결과가 이렇다.

| `?keyword=테니스&size=10` (운영 HTTP) | 실측 |
|---|---|
| 배포 직전 (MySQL FULLTEXT) | 42.44 / 34.83초 |
| 배포 직후 (OpenSearch) | 1.15(첫 요청) / **0.21 / 0.17초** |

warm 기준 약 **170~200배**. 아래는 여기까지 온 과정이다.

## 왜 OpenSearch 인가

MySQL FULLTEXT 는 매치 집합을 통째로 만들고 나서야 정렬을 시작해서, 11건만 필요해도
매치가 4만 건이면 4만 건을 다 다룬다. `LIMIT` 이 안 통하고, MySQL 안에서의 우회는
일곱 가지가 전부 막혔다. 실험 문서의 남은 선택지 중 C안(검색엔진 도입)이 이것인데,
그 표에 적혀 있듯 **아직 근거 측정이 없다.** 그 측정을 만드는 것부터가 이 문서의 시작이다.

## OpenSearch 가 뭔가

- **검색 전용 서버**다. Elasticsearch 에서 갈라져 나온 오픈소스이고, 내부 검색 엔진은
  루씬(Lucene)이라는 자바 라이브러리다.
- **DB 를 대체하는 게 아니다.** 원본 데이터는 계속 MySQL 에 있고, 검색용 사본을
  OpenSearch 에 하나 더 두는 구조다. 검색 요청만 OpenSearch 가 받는다.
- 데이터는 JSON 문서로 넣고, 검색도 HTTP API 로 한다. 별도 쿼리 언어 대신 JSON 으로
  조건을 적어 보낸다.

## 왜 MySQL 에서 막힌 문제가 여기서는 안 생기나

MySQL 이 막힌 지점과 하나씩 대응시키면 이렇다.

| MySQL FULLTEXT 에서 막힌 것 | OpenSearch(루씬)에서는 |
|---|---|
| 매치 전량을 만들고 나서야 정렬 → `LIMIT` 무력 | 인덱스 자체를 최신순으로 미리 정렬해둘 수 있다(index sorting). 최신순 요청이면 위에서부터 걷다가 11건 차면 멈춘다 |
| 정렬하려면 매치 전량의 정렬 키를 확인 | 정렬값(`created_at`)이 색인 안에 같이 들어 있어(doc values) 행을 읽으러 갈 필요가 없다. 전량을 훑더라도 수만 건에서 상위 11건 뽑는 건 ms 단위 |
| phrase 검증이 매치 건수에 초선형으로 비쌈 | 토큰 위치 대조가 루씬의 기본 동작이라 빠르다 |
| 커서 페이지네이션 | `search_after` 파라미터가 우리 `(createdAt, id)` 복합 커서와 같은 개념. 지금 설계가 그대로 이식된다 |
| 매치 0건은 즉시 | 같다. 역색인 조회라 즉시 끝난다 |
| (별개 문제) 결과 0건 필터 조합이 100만 행을 걺 | 필터도 역색인이라 교집합이 비면 즉시 0건. 검색 전체를 옮기면 이 문제도 같이 풀린다 |

단, 여기까지는 "구조상 그래야 한다"는 설명이다. 이 프로젝트에서 결론은 실측으로만 낸다.

## 치러야 하는 대가

- **서버가 하나 더 생긴다.** 자바 기반이라 메모리를 꽤 먹는다(최소 512MB~1GB).
  운영 EC2 에 넣을 수 있을지는 인스턴스 사양에 달렸다.
- **동기화가 필요하다.** 글 생성/수정/삭제/블라인드 때 OpenSearch 에도 반영해야 한다.
  사본이라 원본과 짧은 지연·불일치가 생길 수 있다.
- **검색 결과의 의미가 바뀔 수 있다.** 텍스트를 어떻게 토큰으로 쪼개느냐(analyzer)에 따라
  같은 검색어라도 결과가 달라진다. 아래 「결정할 것」 참고.

## 계획

**1단계 — 로컬 검증 (측정이 먼저)**
- 로컬 Docker 에 OpenSearch 1대를 띄운다
- 로컬 MySQL 의 bench 100만 건을 색인한다
- FULLTEXT 실험과 같은 5개 시나리오(`포핸드`/`복식`/`라켓`/`테니스`/매치 0건)를
  같은 조건(최신순 `LIMIT 11`)으로 측정한다
- 여기서 수십 초 → 수십 ms 급의 차이가 확인돼야 다음으로 간다

**2단계 — 애플리케이션 연동**
- `SearchRepository` 포트 구조는 유지하고, 키워드 경로만 OpenSearch 구현으로 바꾼다
- 글 저장/수정/삭제 시 색인 동기화
- 테스트 fake 작성

**3단계 — 운영 반영**
- EC2 컨테이너 추가 vs AWS 관리형 서비스. 1단계 측정과 EC2 사양을 보고 결정한다

## 결정한 것

| 항목 | 결정 | 이유 |
|---|---|---|
| 매칭 방식(analyzer) | **지금과 동일하게 2글자 ngram + phrase 재현** | MySQL 과 결과가 같아야 성능 비교 검증이 깔끔하다. nori(형태소 분석)는 품질 개선 카드로 남겨둔다 |
| 적용 범위 | **키워드 검색만** | keyword 가 있을 때만 OpenSearch, 나머지 목록/필터는 MySQL 유지. 변경 최소, 장애 시 영향도 최소 |
| 클라이언트 | **의존성 추가 없음 — 스프링 내장 `RestClient` + Jackson** | 필요한 호출이 검색·색인·삭제 세 개뿐이다. `opensearch-java` 를 들이면 전송 계층 의존성과 버전 매트릭스가 따라오는데, 그 대가로 얻는 게 지금은 없다. 보내는 JSON 이 1단계에서 curl 로 검증한 것과 글자 그대로 같다는 것도 장점. 벌크 색인 등이 필요해지는 시점에 재검토 |
| 운영 배포 위치 | **기존 EC2(t3.small)에 컨테이너로 추가** + 스왑 2GB + EBS 16GB 증설 | 추가 비용이 월 $1 미만(EBS 증설분)으로 가장 싸다. 별도 EC2(+$15)·관리형(+$30, 프리티어면 무료)과 비교 후 결정. 코드가 `OPENSEARCH_URI` 하나에만 의존해서 나중에 옮기기도 쉽다 |
| 관리형(AWS OpenSearch Service) 전환 | **보류** (2026-08-23 재검토) | 무료 조건이 소멸했다 — 구 프리티어(t3.small.search 750h/월, 12개월)는 2025-07-15 이전 생성 계정 대상이라 지금은 어느 계정이든 기간 만료고, 이 계정의 크레딧 잔액도 0 임을 확인했다. 월 $30~35 실비인데, 현 구조의 유일한 실질 문제(유휴 후 첫 요청 지연, 10절)는 t3.medium(+$19)이나 별도 t4g.small(+$15)로도 풀린다. 전환 자체는 HTTPS+인증 헤더 소폭 수정이라 필요해지면 언제든 쉽다 |

## 진행 기록

### 1. 로컬 환경 구성 (2026-08-22)

OpenSearch 2.19.1 을 Docker 로 띄웠다. 로컬 검증용이라 보안 플러그인은 껐다.

```bash
docker run -d --name bench-opensearch -p 9200:9200 \
  -e discovery.type=single-node \
  -e DISABLE_SECURITY_PLUGIN=true \
  -e DISABLE_INSTALL_DEMO_CONFIG=true \
  -e "JDK_JAVA_OPTS=-XX:UseSVE=0" \
  -e "OPENSEARCH_JAVA_OPTS=-Xms1g -Xmx1g" \
  -v bench-opensearch-data:/usr/share/opensearch/data \
  opensearchproject/opensearch:2.19.1
```

걸린 것 하나: **Apple M4 맥에서는 컨테이너 JVM 이 SIGILL 로 즉사한다.** 리눅스 aarch64 JDK 가
M4 의 CPU 기능(SVE)을 잘못 감지하는 알려진 버그로, `JDK_JAVA_OPTS=-XX:UseSVE=0` 을 줘야 뜬다.
(`OPENSEARCH_JAVA_OPTS` 가 아니라 `JDK_JAVA_OPTS` 여야 하는 이유: 기동 전에 도는 자바 버전
체크 프로세스까지 같은 플래그를 받아야 해서다.)

### 2. 색인 설계

인덱스 이름 `posts`, 샤드 1개(단일 노드라 나눌 이유가 없다). 핵심 설정 두 가지:

- **2글자 ngram analyzer** — MySQL 의 `ngram_token_size=2` 를 그대로 재현한다.
  `min_gram=2, max_gram=2`, 공백·문장부호에서 끊어지도록 `token_chars: [letter, digit]`.
- **index sorting** — `(created_at desc, id desc)`. 이게 이번 도입의 요점이다.
  색인을 디스크에 쓸 때부터 최신순으로 정렬해두면, 최신순 검색이 들어왔을 때 위에서부터
  걷다가 11건 차는 순간 멈출 수 있다. MySQL FULLTEXT 에 없던 조기 종료가 여기서 생긴다.

필드는 검색 대상(`title`, `content`)과 정렬 키(`created_at`, `id`), 소프트삭제/블라인드
(`deleted`, `blinded`), 그리고 2단계에서 필터로 쓸 컬럼(`category`, `meeting_type`,
`recruit_status`, `sido`, `sigungu`)까지 넣었다. 필터 컬럼은 `keyword` 타입(분석 없이
값 그대로 색인 = MySQL 의 `=` 비교에 해당)이다.

`created_at` 은 date 타입이라 밀리초까지만 저장된다(원본은 마이크로초). 같은 밀리초 안의
순서는 `id` 가 정하는데, 이 데이터셋은 `created_at` 이 `id` 순서와 일치하도록 정리돼 있어
(날짜 범위 문서 7절) 최종 순서는 MySQL 과 같다.

### 3. 적재

MySQL 에서 `JSON_OBJECT()` 로 한 줄에 한 문서씩 뽑아 OpenSearch `_bulk` API 로 5,000건씩
밀어넣는 파이프라인. 적재 중에는 `refresh_interval=-1`(검색 노출용 갱신을 꺼서 색인 속도 확보),
끝나면 되돌린다. `_id` 는 게시글 `id` 를 그대로 써서 재적재해도 중복이 안 생긴다.

### 4. 예상 밖 발견 — 로컬 MySQL 에서는 문제가 재현되지 않는다

적재 전에 로컬 MySQL 로 대조 측정을 했더니 **전부 빨랐다.**

| 키워드 | 매치 | 로컬 MySQL (3회) | 운영 RDS (참고) |
|---|---|---|---|
| `포핸드` | 3,478 | 0.10~0.13초 | 0.07~1.26초 |
| `복식` | 49,685 | 0.19~0.26초 | 22.97~30.50초 |
| `라켓` | 69,237 | 0.23~0.24초 | 32.61~41.94초 |
| `테니스` | 41,679 | 0.28~0.31초 | 33.19~51.80초 |
| 매치 0건 | 0 | 0.07~0.08초 | 즉시 |

측정은 운영과 같은 쿼리 형태(`SELECT p.* ... ORDER BY created_at DESC, id DESC LIMIT 11`),
`docker exec` 오버헤드(`SELECT 1` 기준 0.09~0.11초) 포함 값이다.

빠른 이유는 환경이다. 로컬 컨테이너의 버퍼풀이 **1.5GB** 라 `posts` 486MB 가 통째로 메모리에
들어가고, M4 CPU 가 빠르다. RDS 는 버퍼풀 128MB 로 테이블의 1/3.8 이었다.

다만 **구조는 로컬에서도 똑같이 재현된다.** `EXPLAIN ANALYZE` 를 뜨면 FT 노드가
`rows=41679`(매치 전량)를 넘기는 게 운영과 동일하다 — 같은 일을 하는데 하드웨어가 좋아서
빨리 끝날 뿐이다.

```
-> Limit: 11 row(s)
    -> Sort row IDs: created_at DESC, id DESC
        -> Filter: ... MATCH(...)                (rows=39795)
            -> Full-text index search on p ...   (rows=41679)   ← 로컬에서도 전량
```

그래서 **1단계 검증의 축을 바꾼다.** 로컬에서 절대 시간을 비교하는 건 의미가 약하다
(둘 다 빠를 것이다). 대신 이걸 확인한다:

1. **읽는 양** — MySQL 은 11건을 위해 41,679건을 읽는다. OpenSearch 가 정말 몇 건만
   보고 멈추는지 확인한다.
2. **결과 동일성** — 같은 검색어에 대해 매치 건수와 상위 11건이 MySQL 과 같은지.
3. **운영급 판정은 3단계에서** — 진짜 "34초 → 몇 ms" 는 운영과 같은 제약(작은 메모리)의
   환경에서 재야 한다.

### 5. 적재와 1단계 검증 결과

적재는 100만 건이 **27.5초**에 끝났고, 색인 크기는 **222.8MB**(MySQL `posts` 486MB 의
절반 이하), 세그먼트 3개.

**① 결과가 MySQL 과 정확히 같다.** 매치 건수(활성 필터 포함)가 다섯 시나리오 전부
FULLTEXT 실험 10의 "Filter 통과" 수치와 일치하고, `포핸드`·`테니스` 의 상위 11건 id 를
나란히 뽑아보니 **한 건도 다르지 않았다.** 2글자 ngram + phrase 재현이 의도대로 됐다는 뜻이다.

| 키워드 | MySQL Filter 통과 | OpenSearch `_count` | 상위 11건 id |
|---|---|---|---|
| `포핸드` | 3,308 | 3,308 | 일치 |
| `복식` | 47,409 | 47,409 | — |
| `라켓` | 66,117 | 66,117 | — |
| `테니스` | 39,795 | 39,795 | 일치 |
| 매치 0건 | 0 | 0 | — |

**② 읽는 양이 다르다 — 이게 핵심 증거다.** `profile` 로 잡은 `테니스` 검색에서
phrase 쿼리가 순회한 문서는 **35건**이다. 같은 11건을 뽑는 데 MySQL FULLTEXT 는
41,679건을 읽었다.

```
PhraseQuery: next_doc_count=35     ← 매치 39,795건 중 35건만 보고 멈췄다
collector: search_top_hits 0.10ms
```

인덱스가 최신순으로 정렬돼 있으니(index sorting) 위에서부터 걷다가 세그먼트마다 11건씩
차는 순간 멈춘 것이다. MySQL 에서 일곱 가지 우회로도 뚫지 못한 "조기 종료" 가 여기서는
기본 동작이다.

**③ 시간은 매치 건수와 무관하다.** 5회씩 잰 `took` 이 전 키워드 1~9ms. 매치가 6.6만 건인
`라켓` 과 0건인 검색어가 같은 자릿수다. 전체 건수를 세게 강제해도(`track_total_hits=true`)
1~2ms 로, 이 데이터 규모에서는 카운트 비용도 문제가 아니다.

| 키워드 | 매치 | took (5회) |
|---|---|---|
| `포핸드` | 3,308 | 2~8ms |
| `복식` | 47,409 | 1~8ms |
| `라켓` | 66,117 | 1~4ms |
| `테니스` | 39,795 | 1~9ms |
| 매치 0건 | 0 | 1ms |

> 이 절대값은 M4 + 힙 1GB 로컬의 값이다. 운영급 숫자는 3단계에서 다시 잰다.
> 다만 "매치 건수에 시간이 비례하지 않는다" 와 "35건만 읽는다" 는 구조의 증거라
> 환경이 바뀌어도 유지된다 — MySQL 의 `rows=매치 전량` 이 환경과 무관했던 것과 같은 이유다.

**1단계 통과.** 2단계(애플리케이션 연동)로 간다.

색인 생성 스크립트와 최초 적재 도구는 재현할 수 있게 리포지토리에 뒀다 —
`bench/opensearch/01_create_index.sh`, `bench/opensearch/02_bulk_load.py`.

### 6. 2단계 — 애플리케이션 연동

저장소 포트 구조를 그대로 두고 색인용 포트 하나를 새로 팠다.

| 파일 | 역할 |
|---|---|
| `api/repository/search/PostSearchIndex.java` | 신규 포트. `searchIds(cond)` / `index(post)` / `delete(id)` / `isEnabled()` |
| `.../search/opensearch/OpenSearchPostSearchIndex.java` | 구현. 스프링 `RestClient` 로 HTTP JSON 호출 |
| `.../search/DisabledPostSearchIndex.java` | 꺼져 있을 때 구현. 색인 호출은 무시, 검색은 안 받음 |
| `.../search/opensearch/OpenSearchProperties.java` + `global/config/OpenSearchConfig.java` | `opensearch.enabled` 값으로 두 구현 중 하나를 빈으로 |
| `.../search/jpa/JpaSearchRepositoryAdapter.java` | 키워드 경로 라우팅 (아래) |
| `.../search/jpa/JpaSearchRepository.java` | id 목록으로 본문 채우는 `findActiveWithMemberByIdIn` 추가 |
| `src/test/.../search/fake/FakePostSearchIndex.java` | 테스트 fake (색인/삭제 호출 기록) |

**검색 흐름** — 키워드가 있으면 OpenSearch 에서 **id 11개만** 받아오고, 본문과 작성자는
MySQL 에서 `JOIN FETCH member` 로 채운 뒤 색인이 준 순서(최신순)로 재정렬한다.
색인에 본문·닉네임까지 넣지 않는 이유: 색인은 "찾기" 만 담당시키고 화면에 보이는 데이터의
원본은 MySQL 하나로 유지해야, 닉네임 변경 같은 것까지 동기화 대상이 되는 걸 막을 수 있다.
커서는 기존 `(createdAt, id)` 복합 커서가 OpenSearch `search_after` 로 그대로 이어진다.

**켜고 끄기** — 프로파일이 아니라 설정값(`opensearch.enabled`)으로 가른다.
local 은 `true`, prod 는 환경변수(`OPENSEARCH_ENABLED`, 기본 false), test·inmemory 는
기본값 false. 꺼져 있으면 기존 FULLTEXT 네이티브 쿼리를 그대로 탄다 — 운영은 3단계에서
환경변수만 켜면 전환되고, 문제가 생기면 끄면 롤백이다.

**폴백** — OpenSearch 호출이 실패하면 경고 로그를 남기고 기존 FULLTEXT 쿼리로 그 자리에서
폴백한다. 검색이 죽는 대신 느려지는 쪽을 택했다.

**동기화 지점 6곳** — 색인에 실리는 필드(제목·본문·필터 컬럼·상태 플래그)가 바뀌는 곳마다
색인 호출을 명시적으로 넣었다.

| 지점 | 호출 |
|---|---|
| `PostService.createPost` | `index` |
| `PostDraftService.publishPostDraft` | `index` |
| `PostService.updatePost` | `index` (제목·본문·필터 컬럼 변경) |
| `PostService.closeRecruiting` | `index` (`recruit_status` 변경) |
| `PostService.deletePost` | `delete` |
| `PostReportHandler.handleReported` | 블라인드로 바뀌는 순간에만 `index` |

`index`/`delete` 는 실패해도 본 트랜잭션을 깨지 않는다(로그만 남김). 그래서 **DB 와 색인이
어긋날 수 있는 창이 존재한다** — 색인 실패, 또는 색인 후 트랜잭션 롤백. 지금은
`02_bulk_load.py` 재적재(문서 id = 게시글 id 라 멱등)가 복구 수단이고, 정합성 검증·재시도는
운영에서 필요해지면 붙인다. 좋아요·댓글·조회수는 색인에 없으므로 동기화 대상이 아니다.

### 7. 로컬 E2E 검증

앱을 local 프로파일로 띄우고(로컬 MySQL 100만 건 + OpenSearch) API 로 확인했다.

**검색** — `?keyword=테니스&size=10` 응답 시간 0.100초(첫 요청) / 0.021초(반복 2회).
상위 결과가 1단계에서 SQL 로 대조한 id 와 일치하고, 매치 0건은 0.016초. 폴백 경고 로그 0건
— OpenSearch 경로를 탔다는 뜻이다.

**동기화** — 고유 키워드를 넣은 글을 API 로 생성 → 즉시 검색에 노출, OpenSearch 문서도
확인됨 → 삭제 → 검색 0건, 색인 문서도 제거됨.

**커서** — `nextCursor` 로 2페이지를 이어 받아 중복 없음·내림차순 유지 확인.
키워드+`category`+`sido` 필터 결합도 정상.

**장애 폴백** — OpenSearch 컨테이너를 내리고 같은 검색을 보내니 200 / **0.349초**
(기존 FULLTEXT 경로) + 경고 로그 1건. 같은 축(HTTP)에서 새 경로 0.021초와 비교하면
로컬에서도 약 17배 차이인데, 1회 측정이고 로컬은 어차피 둘 다 빠른 환경이라 참고값이다.

2단계 완료. 남은 것은 3단계 — EC2 에 OpenSearch 컨테이너를 올리고, RDS 데이터로 초기
적재를 하고, `OPENSEARCH_ENABLED=true` 로 켠 뒤 **운영 환경에서 34초짜리 시나리오를
재측정**하는 것이다.

### 8. 3단계 준비 — 배포는 어떻게 하나

**OpenSearch 용 Docker 이미지는 만들지 않는다.** 이미지를 새로 굽는 건 우리 코드가 이미지에
들어가야 할 때(backend)뿐이고, OpenSearch 는 nginx 처럼 공식 이미지를 설정만 바꿔 쓰는
인프라 컨테이너다. ngram·정렬 같은 색인 설정은 이미지가 아니라 **데이터**라서, 컨테이너를
띄운 뒤 `01_create_index.sh` 로 한 번 만들면 볼륨에 남는다 — MySQL 이미지에 테이블을 굽지
않는 것과 같다.

`deploy/docker-compose.yml` 에 반영해둔 것:

- `opensearch` 서비스 — 공식 이미지 2.19.1, 포트는 **`127.0.0.1:9200` 루프백 전용 바인딩**
  (색인 생성·초기 적재를 EC2 호스트에서 하기 위해. 외부에는 안 열리고 보안그룹도 막고 있어
  보안 플러그인을 끈 게 허용된다. backend 는 compose 네트워크로 `http://opensearch:9200`),
  힙 512m(t3.small 2GB 기준), 데이터 볼륨.
- backend 서비스에 환경변수 2개 — `OPENSEARCH_URI=http://opensearch:9200`,
  `OPENSEARCH_ENABLED`(기본 **false**). 컨테이너를 먼저 올리고 초기 적재까지 끝낸 다음에
  true 로 켜는 순서를 강제하기 위해 기본을 꺼둔다. 문제가 생기면 이 값 하나로 롤백.

**CD 파이프라인은 건드리지 않는다.** CD 는 backend 이미지만 교체하고, opensearch 는
nginx·frontend 처럼 compose 로 관리되는 상주 컨테이너다. 단, `docs/cicd.md` 6절대로
**CD 는 `deploy/` 를 EC2 로 복사하지 않으므로** compose 변경은 EC2 `~/app/` 에 수동 반영해야
한다.

EC2 반영 절차 (아직 실행 안 함):

```bash
# 1. ~/app/docker-compose.yml 에 opensearch 서비스·환경변수 반영 (deploy/ 사본과 동일하게)
# 2. 컨테이너 기동 (EC2 는 x86_64 라 M4 용 JDK_JAVA_OPTS 불필요)
docker compose up -d opensearch
# 3. 색인 생성
./01_create_index.sh http://localhost:9200   # 또는 docker exec 로
# 4. 초기 적재 — EC2 에서 RDS 로 붙어 02_bulk_load.py 파이프 실행 (로컬과 같은 명령,
#    mysql 접속 정보만 RDS 로. 로컬 100만 건이 27.5초였으니 몇 분 안쪽 예상)
# 5. 스위치 온 — ~/app/.env 에 OPENSEARCH_ENABLED=true 를 넣고 backend 재기동.
#    export 로 켜면 안 된다: CD 재배포가 매번 새 SSH 세션에서 돌아서 다음 배포 때
#    기본값 false 로 조용히 꺼진다. compose 가 자동으로 읽는 .env 에 넣어야 유지된다.
docker compose up -d backend
# 6. 운영 재측정 — 테니스/복식/라켓/포핸드/0건, HTTP 반복 측정
```

착수 전 EC2 실측: nginx 3.5MiB + frontend 4.1MiB + backend 446.5MiB, `available` 865MB,
**스왑 0**. 힙 512m OpenSearch 의 상주 메모리는 JVM 오버헤드까지 700~900MB 라 이대로 올리면
OOM 킬 위험이 있다 → **2GB 스왑을 먼저 붙이고 올리기로 했다** (`vm.swappiness=10` 으로
평시에는 스왑을 거의 안 쓰게). 그래도 빠듯하면 관리형/인스턴스 업그레이드를 검토한다.
적재 중 refresh off 는 로컬과 동일하게 적용한다.

### 9. 3단계 실행 기록 (2026-08-23)

**디스크가 먼저 터졌다.** 스왑 2GB 를 만들려는데 `No space left on device` — 루트 볼륨
6.7GB 가 95% 찬 상태였다. 범인은 CD 가 배포마다 쌓아온 `:<SHA>` 태그 이미지
(backend 27개 + frontend 15개). CD 의 `docker image prune -f` 는 태그 없는 이미지만 지우기
때문에 한 번도 정리된 적이 없었다. 실행 중인 컨테이너의 이미지만 남기고 지워 2.5GB 를
확보했고, 재발 방지로 CD 의 정리 명령을 `prune -af --filter "until=168h"` 로 바꿨다
(`docs/cicd.md`).

이후 순서대로:
- **EBS 8→16GB 증설** (콘솔 Modify volume → `growpart` → `resize2fs`, 무중단. Avail 8.3GB)
- **스왑 2GB 활성** (`fallocate` → `mkswap` → `swapon`, fstab 등록)
- **OpenSearch 기동** — green. 상주 919.7MiB(예측 700~900MB 범위), 커널이 backend 의 유휴
  페이지를 스왑으로 밀어 backend RSS 446→90MiB, 스왑 사용 440MiB 에서 안정
- **초기 적재** — RDS → OpenSearch **1,000,028건 / 140.4초**, 색인 310.2MB
  (로컬 27.5초 대비 5배 — t3.small CPU 차이)
- **스위치 켜기 전 기준선(HTTP, 같은 날)** — `테니스` **42.44 / 34.83초**,
  `포핸드` 1.53초, 매치 0건 0.13초, 조건 없음 0.16초. 역대 범위(33~52초)가 그대로 재현됐다

**여기서 하나 막혔었다: 운영 backend 는 OpenSearch 코드가 없는 옛 이미지였다.** 연동 코드가
main 에 머지·배포되지 않으면 `.env` 의 `OPENSEARCH_ENABLED=true` 는 아무 일도 하지 않는다.
부수 발견: 수동으로 `docker compose up -d backend` 를 치면 `BACKEND_TAG` 가 없어 `:latest`
를 pull 하려다 만료된 GHCR 토큰으로 실패한다(`cicd.md` 8절의 함정 그대로). 기존 컨테이너는
영향 없이 살아 있었고, PR 머지 → CD 배포로 해소했다.

### 10. 배포 후 운영 측정 — 결론

머지 커밋이 CD 로 배포되면서 스위치가 켜졌다(`.env` 에 미리 넣어둔 값). 배포 직전에 잰
같은 날 기준선과 나란히 놓으면:

| 요청 | 배포 직전 (FULLTEXT) | 배포 직후 (OpenSearch, 3회) |
|---|---|---|
| `테니스` (매치 4.2만) | 42.44 / 34.83초 | 1.148 / 0.209 / 0.172초 |
| `복식` (5.0만) | 역대 22.97~30.50초 | 0.253 / 0.170 / 0.162초 |
| `라켓` (6.9만) | 역대 32.61~41.94초 | 0.164 / 0.146 / 0.169초 |
| `포핸드` (0.3만) | 1.53초 | 0.185 / 0.129 / 0.151초 |
| 매치 0건 | 0.128초 | 0.096 / 0.086 / 0.082초 |
| 조건 없음 (대조, MySQL 경로) | 0.165초 | 0.312 / 0.086 / 0.080초 |

읽을 점:
- **매치 수만 건짜리 키워드가 전부 0.2초 안쪽으로 내려왔고, 매치 건수와의 상관이 사라졌다.**
  warm 기준 `테니스` 는 약 170~200배. FULLTEXT 문서에서 "매치가 많으면 무너진다" 던
  그 축이 통째로 없어진 것이다.
- 폴백 경고 로그 0건 — 전부 OpenSearch 경로를 탔다.
- 키워드 없는 목록(조건 없음)은 배포 전과 같은 수준. MySQL 경로 그대로라는 대조 확인.

**운영 동기화도 확인했다.** 글 생성 → 검색 노출 → 삭제 → 검색 제외까지 동작하는데,
한 가지 특성이 있다: **생성 직후 약 1초는 검색에 안 잡힌다.** OpenSearch 는 색인된 문서를
refresh 주기(1초)마다 검색에 노출하는 near-realtime 구조라서다(생성 직후 검색이 비었다가
2초 뒤 잡히는 걸로 확인). 삭제 반영도 같은 주기를 따른다. 목록·상세는 MySQL 이라 즉시고,
검색만 1초 지연이다 — 이 서비스에서 문제될 게 없다고 판단한다.

**유휴 뒤 첫 요청만 느리다 (스왑 콜드스타트).** 배포 후 "필터 붙인 검색이 살짝 느리다" 는
제보가 있어 쪼개 쟀는데, 해당 조합(키워드+모집중+날짜)의 서버 응답은 0.10~0.35초로 키워드
단독과 같았고 폴백도 0건 — 필터 문제가 아니었다. 실체는 **첫 요청만 느린 패턴**이다
(브라우저 개발자도구로도 확인). 2GB 에 컨테이너 4개를 넣고 스왑에 ~780MB 가 밀려나 있는
상태라, 유휴 시간이 지나면 커널이 backend/OpenSearch 의 안 쓰는 페이지를 스왑으로 내리고
다음 첫 요청이 그걸 다시 불러오며 첫 타만 0.3~1.1초를 낸다. 두 번째부터는 0.1초대.
근본 해법은 메모리(t3.medium 또는 별도 인스턴스, 진행 기록 8의 대안 비교)고, t3.small 을
유지하는 동안은 감수하기로 한 트레이드오프다.

### 남은 것 / 하지 않은 것

- **결과 0건 필터 조합의 100만 행 스캔(8.7초)은 그대로 남아 있다** — 적용 범위를 키워드
  검색으로 한정했기 때문. 검색 전체를 OpenSearch 로 옮기면 같이 풀린다
  (`date-range-search-troubleshooting.md` 12절).
- DB 와 색인의 정합성 검증·재시도 장치는 없다. 어긋나면 `03_initial_load_ec2.sh` 재적재가
  복구 수단(문서 id = 게시글 id 라 멱등).
  개선안으로 **트랜잭셔널 아웃박스**를 구상해뒀다(미착수): 게시글 저장과 같은 트랜잭션에
  "이 post_id 를 다시 색인하라" 요청만 저장(payload 없이 id 만 — 처리 시점에 DB 현재 상태를
  다시 읽어 upsert/delete 하면 순서·중복 문제가 소멸한다), 폴러가 주기적으로 미처리 행을
  집어 처리하고 상태를 바꾼다. retry_count 초과 시 FAILED 로 치워 독약 행을 격리한다.
  대가는 폴링 주기만큼의 노출 지연 추가, `@Scheduled` 도입(CLAUDE.md 의 "스케줄러는 없다"
  서술 갱신 필요 — 금지 규약이 아니라 블랙리스트 정리가 TTL 로 대체된 뒤의 사실 서술이다),
  아웃박스 테이블 DDL 수동 적용(prod `ddl-auto: none`).
- nori(형태소 분석) 등 검색 품질 개선은 별도 증분으로. 현재 검색은 "입력한 글자가 글에
  연속으로 들어 있는가"만 보는 부분 문자열 매칭이다(운영 확인: `테니`·`니스` 는 잡히고
  `테니쓰`·`태니스` 같은 오타는 0건). 개선 순서는 ngram+nori 멀티필드 → fuzziness →
  (커서와의 충돌 설계 후) 관련도 정렬.
- **벡터 검색(k-NN)은 쓰지 않는다.** 임베딩 벡터로 "의미가 가까운" 문서를 찾는 방식이고
  OpenSearch 에 기능 자체는 기본 포함이라 엔진 교체 없이 갈 수 있지만, 지금 안 쓰는 이유가
  네 가지다. ① 글 저장·검색마다 텍스트를 벡터로 바꿀 임베딩 모델이 필요하다 — 외부 API 면
  요청당 비용, 로컬 모델이면 메모리·CPU 인데 스왑으로 버티는 t3.small 에 자리가 없다.
  ② 메모리 체급이 다르다 — 100만 건 × 수백 차원 float + HNSW 그래프면 수 GB 급으로,
  현재 색인 310MB 와 비교가 안 된다. ③ 유사도 점수 정렬이라 커서 페이지네이션의
  "정렬 키 불변" 전제가 깨진다(관련도 정렬과 같은 문제). ④ 모집글 키워드 검색이라는
  용도에는 lexical + nori·fuzziness 로 충분하다. 벡터가 값을 하는 건 자연어 질의나
  "비슷한 모임 추천" 같은 기능이 생길 때고, 그때도 lexical 과 섞는 하이브리드가 정석이다.

### 부록 — 로컬 재현 절차

```bash
# 1. OpenSearch 컨테이너 (M4 맥은 JDK_JAVA_OPTS 필수 — 진행 기록 1)
docker start bench-opensearch   # 최초 생성 명령은 진행 기록 1

# 2. 색인 생성 + 적재 (적재 명령 상세는 02_bulk_load.py 머리 주석)
bench/opensearch/01_create_index.sh

# 3. 앱 실행 — local 프로파일이 opensearch.enabled=true
./gradlew bootRun --args='--spring.profiles.active=local'
```
