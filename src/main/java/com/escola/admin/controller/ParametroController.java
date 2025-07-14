package com.escola.admin.controller;


import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.mapper.ParametroMapper;
import com.escola.admin.model.response.ParametroResponse;
import com.escola.admin.service.ParametroService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Optional;

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
        Optional<Parametro> optParametro = service.findByChave(chave);
        return optParametro
                .map(mapper::toResponse) // Se o Optional contiver um valor, aplica o mapper
                .orElse(null);
    }
}
