-- MySQL 8.4
-- 모집글 1,000,000건 생성. 기존 앱 시드 데이터는 유지하고, 이전 벤치 데이터만 삭제 후 재생성한다.
-- 본문은 bench_docs(04_docs.sql)에서 가져온다. 글마다 본문이 전부 다르다.
-- 선행: 01_numbers -> 02_members -> 03_words -> 04_docs

CREATE TABLE IF NOT EXISTS bench_generated_posts (
    post_id BIGINT NOT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_bench_generated_posts_post
        FOREIGN KEY (post_id) REFERENCES posts (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

DELETE p
FROM posts p
JOIN bench_generated_posts bgp ON bgp.post_id = p.id;

DELETE FROM bench_generated_posts;

INSERT INTO posts (
    blinded,
    capacity,
    deleted,
    edited,
    comment_count,
    created_at,
    deleted_at,
    like_count,
    member_id,
    report_count,
    updated_at,
    view_count,
    eupmyeondong,
    sido,
    sigungu,
    title,
    place_name,
    detail,
    content,
    image_url,
    category,
    meeting_type,
    recruit_status
)
SELECT
    CASE WHEN MOD(CRC32(CONCAT('bl', s.n)), 50) = 0 THEN b'1' ELSE b'0' END AS blinded,
    CASE WHEN MOD(CRC32(CONCAT('cn', s.n)), 10) = 0 THEN NULL ELSE 4 + MOD(CRC32(CONCAT('cp', s.n)), 17) END AS capacity,
    CASE WHEN MOD(CRC32(CONCAT('dl', s.n)), 25) = 0 THEN b'1' ELSE b'0' END AS deleted,
    CASE WHEN MOD(CRC32(CONCAT('ed', s.n)), 7) = 0 THEN b'1' ELSE b'0' END AS edited,
    FLOOR(POW((CRC32(CONCAT('cc', s.n)) / 4294967296), 3) * 80) AS comment_count,
    s.created_at,
    CASE
        WHEN MOD(CRC32(CONCAT('dl', s.n)), 25) = 0
            THEN LEAST(NOW(6), DATE_ADD(s.created_at, INTERVAL MOD(CRC32(CONCAT('da', s.n)), 30) DAY))
        ELSE NULL
    END AS deleted_at,
    FLOOR(POW((CRC32(CONCAT('lc', s.n)) / 4294967296), 2.5) * 500) AS like_count,
    bmi.member_id,
    0 AS report_count,
    LEAST(
        NOW(6),
        DATE_ADD(s.created_at, INTERVAL MOD(CRC32(CONCAT('ua', s.n)), 1209600) SECOND)
    ) AS updated_at,
    FLOOR(POW((CRC32(CONCAT('vc', s.n)) / 4294967296), 1.8) * 5000) AS view_count,
    CASE WHEN s.meeting_type = 'ONLINE' THEN NULL ELSE s.eupmyeondong END,
    CASE WHEN s.meeting_type = 'ONLINE' THEN NULL ELSE s.sido END,
    CASE WHEN s.meeting_type = 'ONLINE' THEN NULL ELSE s.sigungu END,
    LEFT(
        CONCAT(
            ELT(1 + MOD(CRC32(CONCAT('ta', s.n)), 10),
                '', '주말 ', '평일 ', '아침 ', '야간 ',
                '초보 ', '직장인 ', '2030 ', '소수정예 ', '신규 '),
            ELT(dc.topic,
                '러닝', '등산', '축구', '농구', '배드민턴', '클라이밍',
                '요가', '수영', '자전거', '스터디', '독서', '영어회화',
                '코딩', '보드게임', '방탈출', '사진', '베이킹', '맛집투어',
                '봉사', '캠핑', '테니스', '볼링', '필라테스', '낚시'),
            ' ',
            ELT(1 + MOD(CRC32(CONCAT('tc', s.n)), 14),
                '같이 하실 분', '함께해요', '멤버 모집', '정기 모임',
                '주말 번개', '평일 저녁', '초보 환영', '소규모',
                '장기 참여', '신규 오픈', '원데이', '같이 가실 분',
                '모집합니다', '참여자 구해요')
        ),
        30
    ) AS title,
    CASE
        WHEN s.meeting_type = 'ONLINE' THEN '온라인 화상회의'
        ELSE LEFT(CONCAT(s.sigungu, ' ',
             ELT(1 + MOD(CRC32(CONCAT('pl', s.n)), 8),
                 '커뮤니티센터', '스터디룸', '공원', '체육관',
                 '카페', '도서관', '문화센터', '공방')), 50)
    END AS place_name,
    CASE WHEN s.meeting_type = 'ONLINE' THEN NULL ELSE LEFT(
        CONCAT(
            ELT(1 + MOD(CRC32(CONCAT('dt', s.n)), 6),
                '초보자도 편하게 참여할 수 있습니다.',
                '정해진 시간에 꾸준히 활동합니다.',
                '서로 배려하며 즐겁게 진행합니다.',
                '준비물과 세부 일정은 신청 후 안내합니다.',
                '소규모로 깊이 있게 진행합니다.',
                '경험보다 성실한 참여를 중요하게 생각합니다.'),
            ' 정원 ', COALESCE(4 + MOD(CRC32(CONCAT('cp', s.n)), 17), 10), '명.'
        ),
        100
    ) END AS detail,
    dc.body AS content,
    CASE
        WHEN MOD(CRC32(CONCAT('im', s.n)), 5) = 0
            THEN CONCAT('https://example.com/posts/', LPAD(s.n, 7, '0'), '.jpg')
        ELSE NULL
    END AS image_url,
    s.category,
    s.meeting_type,
    CASE
        WHEN s.age_days >= 1460 THEN IF(MOD(CRC32(CONCAT('rs', s.n)), 100) < 92, 'CLOSED', 'RECRUITING')
        WHEN s.age_days >= 1095 THEN IF(MOD(CRC32(CONCAT('rs', s.n)), 100) < 85, 'CLOSED', 'RECRUITING')
        WHEN s.age_days >= 730  THEN IF(MOD(CRC32(CONCAT('rs', s.n)), 100) < 72, 'CLOSED', 'RECRUITING')
        WHEN s.age_days >= 365  THEN IF(MOD(CRC32(CONCAT('rs', s.n)), 100) < 55, 'CLOSED', 'RECRUITING')
        WHEN s.age_days >= 180  THEN IF(MOD(CRC32(CONCAT('rs', s.n)), 100) < 35, 'CLOSED', 'RECRUITING')
        WHEN s.age_days >= 30   THEN IF(MOD(CRC32(CONCAT('rs', s.n)), 100) < 22, 'CLOSED', 'RECRUITING')
        ELSE                         IF(MOD(CRC32(CONCAT('rs', s.n)), 100) < 10, 'CLOSED', 'RECRUITING')
    END AS recruit_status
FROM (
    SELECT
        n,
        FLOOR(1826 * POW(1 - n / 1000000.0, 2)) AS age_days,
        DATE_SUB(
            NOW(6),
            INTERVAL FLOOR(1826 * 86400 * POW(1 - n / 1000000.0, 2)) SECOND
        ) AS created_at,
        CASE
            WHEN MOD(CRC32(CONCAT('mt', n)), 100) < 35 THEN 'ONLINE'
            ELSE 'OFFLINE'
        END AS meeting_type,
        CASE
            WHEN MOD(CRC32(CONCAT('ct', n)), 100) < 30 THEN 'EXERCISE'
            WHEN MOD(CRC32(CONCAT('ct', n)), 100) < 50 THEN 'FOOD'
            WHEN MOD(CRC32(CONCAT('ct', n)), 100) < 65 THEN 'STUDY'
            WHEN MOD(CRC32(CONCAT('ct', n)), 100) < 77 THEN 'HOBBY'
            WHEN MOD(CRC32(CONCAT('ct', n)), 100) < 87 THEN 'GAME'
            WHEN MOD(CRC32(CONCAT('ct', n)), 100) < 95 THEN 'VOLUNTEER'
            ELSE 'ETC'
        END AS category,
        CASE
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 40 THEN '서울특별시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 60 THEN '경기도'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 68 THEN '부산광역시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 74 THEN '인천광역시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 79 THEN '대구광역시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 83 THEN '대전광역시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 86 THEN '광주광역시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 89 THEN '울산광역시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 91 THEN '세종특별자치시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 93 THEN '강원특별자치도'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 94 THEN '충청북도'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 95 THEN '충청남도'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 96 THEN '전북특별자치도'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 97 THEN '전라남도'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 98 THEN '경상북도'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 99 THEN '경상남도'
            ELSE '제주특별자치도'
        END AS sido,
        CASE
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 40 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 5), '강남구','마포구','송파구','영등포구','성동구')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 60 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 5), '수원시','성남시','용인시','화성시','고양시')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 68 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '해운대구','부산진구','수영구')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 74 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '연수구','남동구','부평구')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 79 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '수성구','달서구','중구')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 83 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '유성구','서구','중구')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 86 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '북구','광산구','서구')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 89 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '남구','중구','울주군')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 91 THEN '세종시'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 93 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '춘천시','원주시','강릉시')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 94 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '청주시','충주시')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 95 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '천안시','아산시')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 96 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '전주시','익산시')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 97 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '여수시','순천시')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 98 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '포항시','경주시')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 99 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '창원시','김해시')
            ELSE ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '제주시','서귀포시')
        END AS sigungu,
        CASE
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 40 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 5), '역삼동','합정동','잠실동','여의도동','성수동1가')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 60 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 5), '영통동','정자동','보정동','동탄1동','백석동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 68 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '우동','부전동','광안동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 74 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '송도동','구월동','부평동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 79 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '범어동','상인동','동성로2가')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 83 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '봉명동','둔산동','은행동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 86 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '용봉동','수완동','치평동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 89 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '삼산동','성남동','언양읍')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 91 THEN '나성동'
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 93 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 3), '퇴계동','무실동','교동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 94 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '복대동','연수동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 95 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '불당동','배방읍')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 96 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '효자동','영등동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 97 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '학동','조례동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 98 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '효자동','황성동')
            WHEN MOD(CRC32(CONCAT('rg', n)), 100) < 99 THEN ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '상남동','내동')
            ELSE ELT(1 + MOD(CRC32(CONCAT('sg', n)), 2), '노형동','서홍동')
        END AS eupmyeondong,
        1 + FLOOR(9999 * POW((CRC32(CONCAT('ms', n)) / 4294967296), 3)) AS member_seq
    FROM numbers
    WHERE n <= 1000000
    ORDER BY n
) s
JOIN bench_member_ids bmi ON bmi.seq = s.member_seq
JOIN bench_docs dc ON dc.doc_id = s.n
ORDER BY s.n;

SET @inserted_posts = ROW_COUNT();
SET @first_post_id = LAST_INSERT_ID();

INSERT INTO bench_generated_posts (post_id)
SELECT @first_post_id + n - 1
FROM numbers
WHERE n <= @inserted_posts;

ANALYZE TABLE posts;

SELECT COUNT(*) FROM posts;
SELECT YEAR(created_at) y, COUNT(*) c FROM posts GROUP BY y ORDER BY y;
SELECT category, COUNT(*) FROM posts GROUP BY category ORDER BY 2 DESC;
SELECT deleted, COUNT(*) FROM posts GROUP BY deleted;
SELECT meeting_type, recruit_status, COUNT(*) FROM posts GROUP BY 1,2;
SELECT ROUND(DATA_LENGTH/1024/1024) data_mb, ROUND(INDEX_LENGTH/1024/1024) idx_mb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA='fullstack' AND TABLE_NAME='posts';
