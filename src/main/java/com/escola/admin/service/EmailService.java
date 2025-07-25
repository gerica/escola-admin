package com.escola.admin.service;

import org.springframework.graphql.client.ClientGraphQlResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EmailService {

    Mono<Void> executeMutation(String mutation, Map<String, Object> variables);

    Mono<ClientGraphQlResponse> executeQuery(String query, Map<String, Object> variables);

}
