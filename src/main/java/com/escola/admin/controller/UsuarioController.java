package com.escola.admin.controller;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.model.mapper.UsuarioMapper;
import com.escola.admin.model.request.UsuarioRequest;
import com.escola.admin.model.response.UsuarioResponse;
import com.escola.admin.security.BaseException;
import com.escola.admin.service.UsuarioService;
import graphql.GraphQLException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class UsuarioController {

    UsuarioMapper mapper;
    UsuarioService service;
    PageableHelp pageableHelp;

    /**
     * Mapeia a mutação 'saveUsuario' do GraphQL.
     * Protegido para que apenas SUPER_ADMIN ou ADMIN possam criar/atualizar usuários.
     */
    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public Mono<UsuarioResponse> saveUsuario(@Argument UsuarioRequest request) { // Return type is now Mono
        return service.save(request)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar o usuário. O serviço retornou um resultado vazio."))) // Use switchIfEmpty for empty Mono
                .onErrorResume(BaseException.class, Mono::error); // This isn't strictly necessary if BaseException is already Mono.error from service, but good for clarity
        // .onErrorResume(Exception.class, e -> Mono.error(new BaseException("Ocorreu um erro inesperado ao processar a solicitação.", e))); // For general fallback
    }

    /**
     * Mapeia a query 'fetchByIdUsuario' do GraphQL.
     * Protegido para que apenas usuários autenticados possam buscar por ID.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<UsuarioResponse> fetchByIdUsuario(@Argument Long id) {
        // Usamos Mono.fromCallable para executar a chamada bloqueante em um thread apropriado
        return Mono.fromCallable(() -> service.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new GraphQLException("Usuário com ID " + id + " não encontrado.")))
                // O ideal é agendar em um scheduler elástico se a operação for longa
                // .subscribeOn(Schedulers.boundedElastic());
                ;
    }

    /**
     * Mapeia a query 'fetchAllUsuariosByFilter' do GraphQL.
     * Protegido para que apenas SUPER_ADMIN ou ADMIN possam listar todos os usuários.
     */
    @QueryMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public Mono<Page<UsuarioResponse>> fetchAllUsuariosByFilter(
            @Argument String filtro,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests) {

        Pageable pageable = pageableHelp.getPageable(page, size, sortRequests);

        return Mono.fromCallable(() -> service.findByFiltro(filtro, pageable)
                .map(usuarioPage -> usuarioPage.map(mapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    /**
     * Mapeia a query 'fetchAllUsuariosByFilterAndEmpresa' do GraphQL.
     * Protegido para que apenas SUPER_ADMIN ou ADMIN possam listar usuários de uma empresa.
     * TODO: Adicionar uma verificação de segurança para garantir que um ADMIN só possa ver usuários da sua própria empresa.
     */
    @QueryMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public Mono<Page<UsuarioResponse>> fetchAllUsuariosByFilterAndEmpresa(
            @Argument String filtro,
            @Argument Long idEmpresa,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests) {

        Pageable pageable = pageableHelp.getPageable(page, size, sortRequests);

        return Mono.fromCallable(() -> service.findByFiltroAndEmpresa(filtro, idEmpresa, pageable)
                .map(usuarioPage -> usuarioPage.map(mapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    /**
     * Mapeia a mutação 'changePassword' do GraphQL.
     * Permite que um usuário autenticado altere sua própria senha.
     * A anotação @PreAuthorize garante que apenas usuários logados possam acessar este endpoint.
     * Isso cobre tanto usuários normais quanto aqueles forçados a trocar a senha (com a role ROLE_FORCE_PASSWORD_CHANGE).
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> changePassword(@Argument String newPassword) {
        return service.changePassword(newPassword)
                // O método do serviço retorna Mono<Void>.
                // O operador .then() aguarda a conclusão e o substitui por um novo Mono.
                .then(Mono.just("Senha alterada com sucesso."))
                // Se o serviço retornar um erro (ex: usuário não encontrado), ele será propagado.
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha ao alterar a senha: " + e.getMessage())));
    }

    /**
     * Mapeia a mutação 'resetPassword' do GraphQL.
     * Permite que um usuário resetar a senha.
     */
    @MutationMapping
    public Mono<String> resetPassword(@Argument String email) {
        return service.resetPassword(email)
                // O método do serviço retorna Mono<Void>.
                // O operador .then() aguarda a conclusão e o substitui por um novo Mono.
                .then(Mono.just("Nova senha enviada para o email."))
                // Se o serviço retornar um erro (ex: usuário não encontrado), ele será propagado.
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha ao tentar resetar a senha: " + e.getMessage())));
    }


}
