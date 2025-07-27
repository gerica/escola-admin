package com.escola.admin.controller.cliente;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.mapper.cliente.ClienteMapper;
import com.escola.admin.model.request.cliente.ClienteRequest;
import com.escola.admin.model.response.cliente.ClienteResponse;
import com.escola.admin.service.cliente.ClienteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
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
public class ClienteController {

    ClienteMapper clienteMapper;
    ClienteService clienteService;
    PageableHelp pageableHelp;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ClienteResponse saveCliente(@Argument ClienteRequest request) {
        var entity = clienteService.save(request);
        return clienteMapper.toResponse(entity);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ClienteResponse> fetchAllClientes(
            @Argument String filtro,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests) {

        Page<Cliente> entities = clienteService.findByFiltro(filtro, pageableHelp.getPageable(page, size, sortRequests)).orElse(Page.empty());
        return entities.map(clienteMapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<ClienteResponse> fetchByIdCliente(@Argument Integer id) {
        return clienteService.findById(id).map(clienteMapper::toResponse);
    }

}
