package com.escola.admin.controller;


import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ParametroController {

    ParametroService service;
    ParametroMapper mapper;

    @PreAuthorize("isAuthenticated()")
    @QueryMapping
    public Mono<ParametroResponse> findByChave(@Argument String chave, Authentication authentication) {
        log.info("Executando findByChave com a chave: {}", chave);
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }

        var optParametro = service.findByChave(chave, usuarioAutenticado.getEmpresaIdFromToken());
        log.info("Admin-service retornando para chave {}: {}", chave, optParametro); // <-- Adicione este log
        return optParametro.map(mapper::toResponse);

    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<ParametroResponse> salvarParametro(@Argument ParametroRequest request, Authentication authentication) {
        log.info("Executando salvarParametro salvar: {}", request);
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }

        return service.salvar(request, usuarioAutenticado.getEmpresaIdFromToken())
                .map(mapper::toResponse) // Se o Optional contiver um valor, aplica o mapper
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar parâmetro. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);

    }
}
