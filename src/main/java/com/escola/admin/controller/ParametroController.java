package com.escola.admin.controller;


import com.escola.admin.model.mapper.ParametroMapper;
import com.escola.admin.model.request.ParametroRequest;
import com.escola.admin.model.response.ParametroResponse;
import com.escola.admin.service.ParametroService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ParametroController {

    ParametroService service;
    ParametroMapper mapper;

    @PreAuthorize("isAuthenticated()")
    @QueryMapping
    public ParametroResponse findByChave(@Argument String chave) {
        log.info("Executando findByChave com a chave: {}", chave);
        var optParametro = service.findByChave(chave);
        return optParametro
                .map(mapper::toResponse) // Se o Optional contiver um valor, aplica o mapper
                .orElse(null);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ParametroResponse salvarParametro(@Argument ParametroRequest request) {
        log.info("Executando salvarParametro salvar: {}", request);
        var entity = service.salvar(request);
        return entity
                .map(mapper::toResponse) // Se o Optional contiver um valor, aplica o mapper
                .orElse(null);
    }
}
