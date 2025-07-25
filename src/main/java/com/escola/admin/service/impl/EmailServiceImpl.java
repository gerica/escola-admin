package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    HttpGraphQlClient graphQlClient;

    /**
     * Executa uma mutação GraphQL e retorna um Mono<Void> em caso de sucesso.
     * A validação de erro é delegada ao método validateGraphQlResponse.
     *
     * @param mutation  A string da mutação a ser executada.
     * @param variables O mapa de variáveis para a mutação.
     * @return um Mono<Void> que completa em caso de sucesso ou emite um erro.
     */
    public Mono<Void> executeMutation(String mutation, Map<String, Object> variables) {
        return graphQlClient.document(mutation)
                .variables(variables)
                .execute()
                // Aplica a validação como um passo de transformação
                .transform(this::validateGraphQlResponse)
                // Se a validação passar, descarta o corpo e sinaliza conclusão
                .then();
    }

    /**
     * Executa uma query GraphQL e retorna a resposta bruta para o chamador.
     * A validação de erro é delegada ao método validateGraphQlResponse.
     * O chamador é responsável por extrair os dados da resposta.
     *
     * @param query     A string da query a ser executada.
     * @param variables O mapa de variáveis para a query.
     * @return um Mono<ClientGraphQlResponse> contendo a resposta validada, ou um Mono.error.
     */
    public Mono<ClientGraphQlResponse> executeQuery(String query, Map<String, Object> variables) {
        return graphQlClient.document(query)
                .variables(variables)
                .execute()
                // Aplica a validação como um passo de transformação
                .transform(this::validateGraphQlResponse);
    }

    /**
     * Método privado que centraliza a lógica de validação de respostas GraphQL.
     * Ele recebe um Mono<ClientGraphQlResponse>, verifica por erros, e retorna
     * o Mono original se estiver tudo OK, ou um Mono.error se houver falhas.
     *
     * @param responseMono O Mono contendo a resposta do cliente GraphQL.
     * @return O mesmo Mono se a resposta for válida, ou um Mono.error se contiver erros.
     */
    private Mono<ClientGraphQlResponse> validateGraphQlResponse(Mono<ClientGraphQlResponse> responseMono) {
        return responseMono.flatMap(response -> {
            if (!response.getErrors().isEmpty()) {
                log.error("Erro na resposta da chamada GraphQL: {}", response.getErrors());
                String errorMessages = response.getErrors().stream()
                        .map(ResponseError::getMessage)
                        .collect(Collectors.joining(", "));
                return Mono.error(new BaseException("Falha na comunicação com o serviço GraphQL: " + errorMessages));
            }
            // Se não houver erros, apenas passa a resposta adiante.
            return Mono.just(response);
        });
    }
}