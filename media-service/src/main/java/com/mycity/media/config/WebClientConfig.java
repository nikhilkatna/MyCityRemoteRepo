package com.mycity.media.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    @LoadBalanced  // Enable load balancing with Eureka if required
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
