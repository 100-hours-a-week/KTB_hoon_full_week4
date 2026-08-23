package kakao.bootcamp.fullstack.api.repository.search.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Address;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class OpenSearchPostSearchIndex implements PostSearchIndex {

    // 색인의 date 포맷(yyyy-MM-dd HH:mm:ss.SSSSSS)과 일치해야 한다. 저장·조회 모두
    // 타임존 없는 벽시계 시각을 UTC 로 간주하는 규약이라 양쪽이 같은 축 위에 있다.
    private static final DateTimeFormatter DOC_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter RANGE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String indexName;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<Long> searchIds(PostSearchCond cond) {
        JsonNode response =
                restClient
                        .post()
                        .uri("/{index}/_search", indexName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(buildSearchBody(cond).toString())
                        .retrieve()
                        .body(JsonNode.class);
        List<Long> ids = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            ids.add(hit.path("fields").path("id").path(0).asLong());
        }
        return ids;
    }

    // index/delete 는 예외를 그대로 던진다. 실패를 알아야 아웃박스 폴러가 재시도한다.
    @Override
    public void index(Post post) {
        restClient
                .put()
                .uri("/{index}/_doc/{id}", indexName, post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildDocument(post).toString())
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void delete(Long postId) {
        try {
            restClient
                    .delete()
                    .uri("/{index}/_doc/{id}", indexName, postId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // 색인에 애초에 없던 문서 — 삭제 완료와 같은 상태이므로 성공으로 본다
        }
    }

    private ObjectNode buildSearchBody(PostSearchCond cond) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("size", cond.size().intValue());
        body.put("track_total_hits", false);
        body.put("_source", false);
        body.putArray("docvalue_fields").add("id");

        ObjectNode bool = body.putObject("query").putObject("bool");
        ObjectNode phrase = bool.putArray("must").addObject().putObject("bool");
        ArrayNode should = phrase.putArray("should");
        should.addObject().putObject("match_phrase").put("title", cond.keyword());
        should.addObject().putObject("match_phrase").put("content", cond.keyword());
        phrase.put("minimum_should_match", 1);

        ArrayNode filter = bool.putArray("filter");
        addTerm(filter, "deleted", false);
        addTerm(filter, "blinded", false);
        if (cond.category() != null) {
            addTerm(filter, "category", cond.category().name());
        }
        if (cond.meetingType() != null) {
            addTerm(filter, "meeting_type", cond.meetingType().name());
        }
        if (cond.recruitStatus() != null) {
            addTerm(filter, "recruit_status", cond.recruitStatus().name());
        }
        if (cond.sido() != null) {
            addTerm(filter, "sido", cond.sido());
        }
        if (cond.sigungu() != null) {
            addTerm(filter, "sigungu", cond.sigungu());
        }
        if (cond.createdFrom() != null || cond.createdTo() != null) {
            ObjectNode range = filter.addObject().putObject("range").putObject("created_at");
            range.put("format", "yyyy-MM-dd HH:mm:ss");
            if (cond.createdFrom() != null) {
                range.put("gte", cond.createdFrom().format(RANGE_DATE_FORMAT));
            }
            if (cond.createdTo() != null) {
                range.put("lt", cond.createdTo().format(RANGE_DATE_FORMAT));
            }
        }

        ArrayNode sort = body.putArray("sort");
        sort.addObject().put("created_at", "desc");
        sort.addObject().put("id", "desc");

        // (createdAt, id) 복합 커서가 search_after 로 그대로 이어진다. date 는 밀리초
        // 정밀도라 마이크로초가 잘리지만, created_at 이 id 순서와 어긋나지 않는 한 순서가 같다.
        if (cond.cursorCreatedAt() != null && cond.cursorId() != null) {
            ArrayNode searchAfter = body.putArray("search_after");
            searchAfter.add(cond.cursorCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
            searchAfter.add(cond.cursorId());
        }
        return body;
    }

    private void addTerm(ArrayNode filter, String field, String value) {
        filter.addObject().putObject("term").put(field, value);
    }

    private void addTerm(ArrayNode filter, String field, boolean value) {
        filter.addObject().putObject("term").put(field, value);
    }

    private ObjectNode buildDocument(Post post) {
        ObjectNode doc = objectMapper.createObjectNode();
        doc.put("id", post.getId());
        doc.put("title", post.getTitle());
        doc.put("content", post.getContent());
        doc.put("created_at", post.getCreatedAt().format(DOC_DATE_FORMAT));
        doc.put("deleted", post.isDeleted());
        doc.put("blinded", post.isBlinded());
        doc.put("category", post.getCategory().name());
        doc.put("meeting_type", post.getMeetingType().name());
        doc.put("recruit_status", post.getRecruitStatus().name());
        Address address = post.getAddress();
        doc.put("sido", address == null ? null : address.getSido());
        doc.put("sigungu", address == null ? null : address.getSigungu());
        return doc;
    }
}
