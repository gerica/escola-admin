package com.escola.admin.controller;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.UsuarioMapper;
import com.escola.admin.model.request.UsuarioRequest;
import com.escola.admin.model.response.UsuarioResponse;
import com.escola.admin.security.BaseException;
import com.escola.admin.service.UsuarioService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UsuarioController {

    UsuarioMapper mapper;
    UsuarioService service;
    PageableHelp pageableHelp;

    @MutationMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public Mono<UsuarioResponse> saveUsuario(@Argument UsuarioRequest request) { // Return type is now Mono
        return service.save(request)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar o usuário. O serviço retornou um resultado vazio."))) // Use switchIfEmpty for empty Mono
                .onErrorResume(BaseException.class, Mono::error); // This isn't strictly necessary if BaseException is already Mono.error from service, but good for clarity
        // .onErrorResume(Exception.class, e -> Mono.error(new BaseException("Ocorreu um erro inesperado ao processar a solicitação.", e))); // For general fallback
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public Page<UsuarioResponse> fetchAllUsuariosByFilter(
            @Argument String filtro,
            @Argument Optional<Integer> page, // Optional to handle default values from schema
            @Argument Optional<Integer> size, // Optional to handle default values from schema
            @Argument Optional<List<SortInput>> sort // Optional for sorting
    ) {
        Page<Usuario> entities = service.findByFiltro(filtro, pageableHelp.getPageable(page, size, sort)).orElse(Page.empty());
        return entities.map(mapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public Page<UsuarioResponse> fetchAllUsuariosByFilterAndEmpresa(
            @Argument String filtro,
            @Argument Long idEmpresa,
            @Argument Optional<Integer> page, // Optional to handle default values from schema
            @Argument Optional<Integer> size, // Optional to handle default values from schema
            @Argument Optional<List<SortInput>> sort // Optional for sorting
    ) {
        Page<Usuario> entities = service.findByFiltroAndEmpresa(filtro, idEmpresa, pageableHelp.getPageable(page, size, sort)).orElse(Page.empty());
        return entities.map(mapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public Optional<UsuarioResponse> fetchByIdUsuario(@Argument Long id) {
        return service.findById(id).map(mapper::toResponse);
    }

}
