package com.loopers.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchConfig {

    private static final String INDEX_NAME = "products";
    private static final String INDEX_SETTINGS_PATH = "elasticsearch/products-index-settings.json";

    private final ElasticsearchClient elasticsearchClient;

    @PostConstruct
    public void createIndexIfNotExists() {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(INDEX_NAME)))
                    .value();

            if (!exists) {
                try (InputStream is = new ClassPathResource(INDEX_SETTINGS_PATH).getInputStream()) {
                    elasticsearchClient.indices().create(c -> c
                            .index(INDEX_NAME)
                            .withJson(is)
                    );
                    log.info("ES '{}' 인덱스 생성 완료", INDEX_NAME);
                }
            } else {
                log.info("ES '{}' 인덱스 이미 존재", INDEX_NAME);
            }
        } catch (Exception e) {
            // 의도적 silent: Circuit Breaker fallback이 MySQL LIKE로 전환하므로 기동은 차단하지 않음
            log.error("ES '{}' 인덱스 생성 실패: {}", INDEX_NAME, e.getMessage(), e);
        }
    }
}
