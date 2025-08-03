package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.service.FileStorageService;
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
public class FileStorageServiceImpl implements FileStorageService {

    HttpGraphQlClient graphQlClient;

    @Override
    public Mono<String> saveFile(String contentBase64) {

        var request = Map.of("contentFileBase64", contentBase64);

        String MUTATION_SEND_FILE = """
                mutation SendFileUpload($request: FileRequest!) {
                  sendFileUpload(request: $request)
                }
                """;
        return graphQlClient.document(MUTATION_SEND_FILE)
                .variables(Map.of("request", request))
                .execute()
                // Aplica a validação como um passo de transformação
                .transform(this::validateGraphQlResponse)
                .flatMap(clientGraphQlResponse -> {
                    String uuid = clientGraphQlResponse.field("sendFileUpload").toEntity(String.class);
                    return Mono.just(uuid);
                });
    }

    @Override
    public Mono<Boolean> deleteFile(String uuid) {
        String MUTATION_SEND_FILE = """
                 mutation DeleteFileByUUID($uuid: String!) {
                  deleteFileByUUID(uuid: $uuid)
                }
                """;
        return graphQlClient.document(MUTATION_SEND_FILE)
                .variables(Map.of("uuid", uuid))
                .execute()
                // Aplica a validação como um passo de transformação
                .transform(this::validateGraphQlResponse)
                .flatMap(clientGraphQlResponse -> {
                    Boolean result = clientGraphQlResponse.field("deleteFileByUUID").toEntity(Boolean.class);
                    return Mono.just(result != null && result);
                });
    }

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
