# MySQL posts 전체를 OpenSearch 에 최초 적재한다. stdin 으로 한 줄에 한 JSON 문서를 받는다.
#
# 사용법 (로컬 bench MySQL 기준):
#   docker exec bench-mysql mysql --default-character-set=utf8mb4 -ubench -pbench fullstack \
#     -N -B -r -e "SELECT JSON_OBJECT('id',id,'title',title,'content',content, \
#       'created_at',DATE_FORMAT(created_at,'%Y-%m-%d %H:%i:%s.%f'), \
#       'deleted',deleted+0,'blinded',blinded+0,'category',category, \
#       'meeting_type',meeting_type,'recruit_status',recruit_status, \
#       'sido',sido,'sigungu',sigungu) FROM posts" \
#   | python3 bench/opensearch/02_bulk_load.py
#
# 적재 전 refresh 를 끄면 빠르다:
#   curl -X PUT localhost:9200/posts/_settings -H 'Content-Type: application/json' \
#     -d '{"index":{"refresh_interval":"-1"}}'
# 끝나면 "1s" 로 되돌리고 _refresh 한 번.
import json
import os
import sys
import time
import urllib.request

BULK_URL = os.environ.get("OS_URI", "http://localhost:9200") + "/posts/_bulk"
BATCH = 5000


def flush(lines, total):
    body = "\n".join(lines) + "\n"
    req = urllib.request.Request(
        BULK_URL,
        data=body.encode("utf-8"),
        headers={"Content-Type": "application/x-ndjson"},
    )
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
    if result.get("errors"):
        for item in result["items"]:
            info = item.get("index", {})
            if info.get("error"):
                print("ERROR:", json.dumps(info["error"])[:300], file=sys.stderr)
                sys.exit(1)
    print(f"indexed {total}", flush=True)


start = time.time()
lines = []
total = 0
for raw in sys.stdin:
    raw = raw.strip()
    if not raw:
        continue
    doc = json.loads(raw)
    doc["deleted"] = bool(doc["deleted"])
    doc["blinded"] = bool(doc["blinded"])
    lines.append(json.dumps({"index": {"_id": doc["id"]}}, ensure_ascii=False))
    lines.append(json.dumps(doc, ensure_ascii=False))
    total += 1
    if total % BATCH == 0:
        flush(lines, total)
        lines = []
if lines:
    flush(lines, total)
print(f"done: {total} docs in {time.time() - start:.1f}s", flush=True)
