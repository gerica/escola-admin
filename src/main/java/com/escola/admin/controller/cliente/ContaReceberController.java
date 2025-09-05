package com.escola.admin.controller.cliente;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.mapper.cliente.ContaReceberMapper;
import com.escola.admin.model.request.cliente.ContaReceberRequest;
import com.escola.admin.model.response.cliente.ContaReceberPorMesResumeResponse;
import com.escola.admin.model.response.cliente.ContaReceberResponse;
import com.escola.admin.service.cliente.ContaReceberService;
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

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ContaReceberController {

    public static final String SUCESSO = "Sucesso";
    ContaReceberMapper mapper;
//    ContaReceberMapper mapper;
    ContaReceberService service;
    PageableHelp pageableHelp;

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('FINANCEIRO', 'ADMIN_EMPRESA')")
    public Mono<String> criarContasReceber(@Argument Long idContrato) {
        return service.criar(idContrato)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha realizar operação: " + e.getMessage())));
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('FINANCEIRO', 'ADMIN_EMPRESA')")
    public Mono<String> saveContaReceber(@Argument ContaReceberRequest request) {
        return service.save(request)
                .then(Mono.just("Operação realizada com sucesso."))
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar a conta receber. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);
    }

    @QueryMapping
    @PreAuthorize("hasAnyAuthority('FINANCEIRO', 'ADMIN_EMPRESA')")
    public Mono<List<ContaReceberResponse>> fetchAllContasReceber(@Argument Long idContrato) {
        return service.findByFiltro(idContrato).map(mapper::toResponseList);
    }

    @QueryMapping
    @PreAuthorize("hasAnyAuthority('FINANCEIRO', 'ADMIN_EMPRESA')")
    public Mono<ContaReceberResponse> fetchByIdContaReceber(@Argument Long id) {
        return service.findById(id).map(mapper::toResponse);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('FINANCEIRO', 'ADMIN_EMPRESA')")
    public Mono<String> apagarContaReceber(@Argument Long id) {
        return service.deletarContaEAtualizarContrato(id)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha realizar operação: " + e.getMessage())));

    }

    @QueryMapping
    @PreAuthorize("hasAnyAuthority('FINANCEIRO', 'ADMIN_EMPRESA')")
    public Mono<ContaReceberPorMesResumeResponse> fetchResumoByMes(@Argument LocalDate dataRef) {
        return service.fetchResumoByMes(dataRef);
    }

}
