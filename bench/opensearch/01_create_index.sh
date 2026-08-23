#!/bin/sh
# posts 색인 생성. 사용법: ./01_create_index.sh [opensearch-uri]
# 핵심: ngram(2,2) analyzer = MySQL ngram_token_size=2 재현,
#       index.sort (created_at desc, id desc) = 최신순 검색의 조기 종료 근거.
OS=${1:-http://localhost:9200}

curl -s -X PUT "$OS/posts" -H 'Content-Type: application/json' -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.sort.field": ["created_at", "id"],
    "index.sort.order": ["desc", "desc"],
    "analysis": {
      "tokenizer": {
        "ngram2": { "type": "ngram", "min_gram": 2, "max_gram": 2, "token_chars": ["letter", "digit"] }
      },
      "analyzer": {
        "ngram2": { "type": "custom", "tokenizer": "ngram2", "filter": ["lowercase"] }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "title": { "type": "text", "analyzer": "ngram2" },
      "content": { "type": "text", "analyzer": "ngram2" },
      "created_at": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss.SSSSSS||yyyy-MM-dd HH:mm:ss" },
      "deleted": { "type": "boolean" },
      "blinded": { "type": "boolean" },
      "category": { "type": "keyword" },
      "meeting_type": { "type": "keyword" },
      "recruit_status": { "type": "keyword" },
      "sido": { "type": "keyword" },
      "sigungu": { "type": "keyword" }
    }
  }
}'
echo
