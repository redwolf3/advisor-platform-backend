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
                .pathsToMatch("/api/visitor/**", "/api/session/**")
                .build();
    }

    @Bean
    public GroupedOpenApi aiChatApi() {
        return GroupedOpenApi.builder()
                .group("ai-chat")
                .pathsToMatch("/api/chat/**")
                .build();
    }

    @Bean
    public GroupedOpenApi messagingApi() {
        return GroupedOpenApi.builder()
                .group("messaging")
                .pathsToMatch("/api/message/**", "/api/thread/**")
                .build();
    }
}
