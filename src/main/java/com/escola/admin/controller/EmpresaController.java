package com.escola.admin.controller;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.mapper.EmpresaMapper;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.model.response.EmpresaResponse;
import com.escola.admin.service.EmpresaService;
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
public class EmpresaController {

    EmpresaMapper mapper;
    EmpresaService service;
    PageableHelp pageableHelp;

    @MutationMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public EmpresaResponse saveEmpresa(@Argument EmpresaRequest request) {
        return service.save(request)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Não foi possível salvar a empresa. O serviço retornou um resultado vazio."));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
//    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public Page<EmpresaResponse> fetchAllEmpresasByFilter(
            @Argument String filtro,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sort) {
        Page<Empresa> entities = service.findByFiltro(filtro, pageableHelp.getPageable(page, size, sort)).orElse(Page.empty());
        return entities.map(mapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public Optional<EmpresaResponse> fetchByIdEmpresa(@Argument Long id) {
        return service.findById(id).map(mapper::toResponse);
    }

}
