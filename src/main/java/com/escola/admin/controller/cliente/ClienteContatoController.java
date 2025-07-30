package com.escola.admin.controller.cliente;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.mapper.cliente.ClienteContatoMapper;
import com.escola.admin.model.request.cliente.ClienteContatoRequest;
import com.escola.admin.model.response.cliente.ClienteContatoResponse;
import com.escola.admin.service.cliente.ClienteContatoService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
public class ClienteContatoController {

    ClienteContatoMapper mapper;
    ClienteContatoService service;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<ClienteContatoResponse> fetchContatoById(@Argument Long id) {
        return service.findById(id).map(mapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<List<ClienteContatoResponse>> fetchContatoByIdCliente(@Argument Long id) {
        return service.findAllByClienteId(id).map(mapper::toResponseList);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public boolean deleteContatoById(@Argument Long id) {
        var entity = service.apagar(id);
        return entity.orElse(false);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public Mono<ClienteContatoResponse> saveClienteContato(@Argument ClienteContatoRequest request) {
        return service.save(request)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar a contato. O serviço retornou um resultado vazio."))) // Use switchIfEmpty for empty Mono
                .onErrorResume(BaseException.class, Mono::error); // This isn't strictly necessary if BaseException is already Mono.error from service, but good for clarity

    }

}
