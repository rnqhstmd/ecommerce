package com.loopers.testcontainers;

import org.springframework.context.annotation.Configuration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class ElasticsearchTestContainersConfig {

    private static final String ES_VERSION = "8.17.0";
    private static final ElasticsearchContainer esContainer;

    static {
        ImageFromDockerfile noriImage = new ImageFromDockerfile("es-nori-test", false)
                .withFileFromString("Dockerfile",
                        "FROM docker.elastic.co/elasticsearch/elasticsearch:" + ES_VERSION + "\n" +
                        "RUN bin/elasticsearch-plugin install --batch analysis-nori"
                );

        // 이미지 빌드 후 ElasticsearchContainer에 전달
        String builtImageName = noriImage.get();

        esContainer = new ElasticsearchContainer(
                DockerImageName.parse(builtImageName)
                        .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
        )
        .withEnv("xpack.security.enabled", "false")
        .withEnv("discovery.type", "single-node")
        .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
        .withExposedPorts(9200);

        esContainer.start();

        System.setProperty("spring.elasticsearch.uris",
                "http://" + esContainer.getHost() + ":" + esContainer.getFirstMappedPort());
    }
}
