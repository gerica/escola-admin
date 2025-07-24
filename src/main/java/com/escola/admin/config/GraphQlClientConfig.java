package com.escola.admin.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class GraphQlClientConfig {


    private static final int TIMEOUT_SECONDS = 10;
    @Value("${util.service.url}")
    private String utilServiceUrl;

    @Bean
    public HttpGraphQlClient httpGraphQlClient(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT_SECONDS * 1000);

        WebClient webClient = webClientBuilder.clone()
                .baseUrl(utilServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(tokenRelayFilter())
                .build();

        return HttpGraphQlClient.builder(webClient).build();
    }

    private ExchangeFilterFunction tokenRelayFilter() {
        return (clientRequest, next) -> {
            String token = getCurrentRequestToken();
            if (token != null) {
                clientRequest = ClientRequest.from(clientRequest)
                        .headers(headers -> headers.setBearerAuth(token.replace("Bearer ", "")))
                        .build();
            }
            return next.exchange(clientRequest);
        };
    }

    private String getCurrentRequestToken() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
    