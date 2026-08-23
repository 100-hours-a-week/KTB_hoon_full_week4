package kakao.bootcamp.fullstack.api.repository.search.opensearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opensearch")
public record OpenSearchProperties(boolean enabled, String uri, String index) {

    public OpenSearchProperties {
        if (uri == null || uri.isBlank()) {
            uri = "http://localhost:9200";
        }
        if (index == null || index.isBlank()) {
            index = "posts";
        }
    }
}
