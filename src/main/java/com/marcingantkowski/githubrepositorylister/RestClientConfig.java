package com.marcingantkowski.githubrepositorylister;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    public RestClient githubRestClient(RestClient.Builder builder, @Value("${github.api.base-url}") final String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
