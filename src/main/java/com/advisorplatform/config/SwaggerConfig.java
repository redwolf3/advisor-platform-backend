package com.advisorplatform.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi visitorSessionApi() {
        return GroupedOpenApi.builder()
                .group("visitor-session")
                .pathsToMatch("/api/v1/visitor/**", "/api/v1/session/**")
                .build();
    }

    @Bean
    public GroupedOpenApi aiChatApi() {
        return GroupedOpenApi.builder()
                .group("ai-chat")
                .pathsToMatch("/api/v1/chat/**")
                .build();
    }

    @Bean
    public GroupedOpenApi messagingApi() {
        return GroupedOpenApi.builder()
                .group("messaging")
                .pathsToMatch("/api/v1/message/**", "/api/v1/thread/**")
                .build();
    }
}
