package com.marcingantkowski.githubrepositorylister;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder(@Value("${github.api.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl);
    }
}
