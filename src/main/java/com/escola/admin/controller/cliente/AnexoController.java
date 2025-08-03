package com.escola.admin.controller.cliente;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.mapper.cliente.AnexoMapper;
import com.escola.admin.model.request.cliente.AnexoRequest;
import com.escola.admin.model.response.cliente.AnexoResponse;
import com.escola.admin.service.cliente.AnexoService;
import graphql.GraphQLException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AnexoController {

    AnexoMapper mapper;
    AnexoService anexoService;
    PageableHelp pageableHelp;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<AnexoResponse> uploadAnexo(@Argument AnexoRequest request) {
        return anexoService.save(request)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar contrato. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<List<AnexoResponse>> anexosDoContrato(@Argument Long idContrato) {
        return anexoService.findByIdContrato(idContrato)
                .map(mapper::toResponseList)
                .onErrorResume(BaseException.class, Mono::error);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public Mono<String> deleteAnexoById(@Argument Long id) {
        return anexoService.deleteById(id)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha realizar operação: " + e.getMessage())));
    }

}
