package com.escola.admin.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
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
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@Slf4j
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
                // Adicione o novo filtro de log ANTES do filtro de token
                .filter(logRequestFilter())
                .filter(tokenRelayFilter())
                .build();

        return HttpGraphQlClient.builder(webClient).build();
    }

    /**
     * Novo filtro para logar os detalhes da requisição enviada.
     */
    private ExchangeFilterFunction logRequestFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // Usamos StringBuilder para uma construção de string eficiente
            String sb = "GraphQL Request Sent -> " + clientRequest.method() +
                    " " +
                    clientRequest.url();
            // Log dos cabeçalhos (opcional, mas útil para depurar o token)
            // clientRequest.headers().forEach((name, values) -> values.forEach(value -> sb.append("\n  ").append(name).append(": ").append(value)));

            // Loga a requisição (sem o corpo, que é um stream)
            log.info(sb);

            // O corpo é um stream e não pode ser lido diretamente aqui sem consumi-lo.
            // A Opção 1 (application.yml) é melhor para ver o corpo da requisição.

            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction tokenRelayFilter() {
        return (clientRequest, next) -> {
            String token = getCurrentRequestToken();
            if (token != null && !token.isBlank()) {
                // Remove o "Bearer " se ele existir, para evitar duplicação
                String bearerToken = token.startsWith("Bearer ") ? token.substring(7) : token;

                ClientRequest newRequest = ClientRequest.from(clientRequest)
                        .headers(headers -> headers.setBearerAuth(bearerToken))
                        .build();
                return next.exchange(newRequest);
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