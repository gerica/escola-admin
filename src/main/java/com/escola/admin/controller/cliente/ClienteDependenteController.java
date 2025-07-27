package com.escola.admin.controller.cliente;

import com.escola.admin.model.mapper.cliente.ClienteDependenteMapper;
import com.escola.admin.model.request.cliente.ClienteDependenteRequest;
import com.escola.admin.model.response.cliente.ClienteDependenteResponse;
import com.escola.admin.service.cliente.ClienteDependenteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClienteDependenteController {

    ClienteDependenteMapper mapper;
    ClienteDependenteService service;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<ClienteDependenteResponse> fetchDependenteById(@Argument Integer id) {
        return service.findById(id).map(mapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<List<ClienteDependenteResponse>> fetchDependenteByIdCliente(@Argument Integer id) {
        return service.findAllByClienteId(id).map(mapper::toResponseList);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public boolean deleteDependenteById(@Argument Integer id) {
        var entity = service.apagar(id);
        return entity.orElse(false);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ClienteDependenteResponse saveClienteDependente(@Argument ClienteDependenteRequest request) {
        var entity = service.save(request);
        return mapper.toResponse(entity);
    }

}
