#!/bin/sh
# EC2 에서 RDS -> OpenSearch 최초 적재. ~/app 에 01, 02 와 함께 두고 실행한다.
# RDS 접속 정보는 같은 디렉터리의 .env(DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD)를 쓴다.
set -e
cd "$(dirname "$0")"
set -a; . ./.env; set +a

if ! curl -sf localhost:9200/posts > /dev/null; then
  sh 01_create_index.sh http://localhost:9200
fi

curl -s -X PUT localhost:9200/posts/_settings -H 'Content-Type: application/json' \
  -d '{"index":{"refresh_interval":"-1"}}' > /dev/null

mysql --default-character-set=utf8mb4 \
  -h "$DB_HOST" -P "${DB_PORT:-3306}" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
  -N -B -r -e "SELECT JSON_OBJECT('id',id,'title',title,'content',content,'created_at',DATE_FORMAT(created_at,'%Y-%m-%d %H:%i:%s.%f'),'deleted',deleted+0,'blinded',blinded+0,'category',category,'meeting_type',meeting_type,'recruit_status',recruit_status,'sido',sido,'sigungu',sigungu) FROM posts" \
  | python3 02_bulk_load.py

curl -s -X PUT localhost:9200/posts/_settings -H 'Content-Type: application/json' \
  -d '{"index":{"refresh_interval":"1s"}}' > /dev/null
curl -s -X POST localhost:9200/posts/_refresh > /dev/null
echo
curl -s 'localhost:9200/_cat/indices/posts?v'
