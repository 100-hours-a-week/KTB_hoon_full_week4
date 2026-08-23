package kakao.bootcamp.fullstack.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import kakao.bootcamp.fullstack.api.repository.search.DisabledPostSearchIndex;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchIndex;
import kakao.bootcamp.fullstack.api.repository.search.opensearch.OpenSearchPostSearchIndex;
import kakao.bootcamp.fullstack.api.repository.search.opensearch.OpenSearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

    @Bean
    public PostSearchIndex postSearchIndex(
            OpenSearchProperties properties, ObjectMapper objectMapper) {
        if (!properties.enabled()) {
            return new DisabledPostSearchIndex();
        }
        return new OpenSearchPostSearchIndex(
                RestClient.create(properties.uri()), objectMapper, properties.index());
    }
}
