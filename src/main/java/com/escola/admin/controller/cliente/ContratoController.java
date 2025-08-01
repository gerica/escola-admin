package com.escola.admin.controller.cliente;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.cliente.ContratoMapper;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.model.response.cliente.ContratoResponse;
import com.escola.admin.service.cliente.ContratoService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ContratoController {

    public static final String SUCESSO = "Sucesso";
    ContratoMapper contratoMapper;
    ContratoService contratoService;
    PageableHelp pageableHelp;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> saveContrato(@Argument ContratoRequest request) {
        return contratoService.save(request)
                .then(Mono.just("Operação realizada com sucesso."))
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar contrato. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<ContratoResponse>> fetchAllContratos(
            @Argument String filtro,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests,
            Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }

        Pageable pageable = pageableHelp.getPageable(page, size, sortRequests);

        return Mono.fromCallable(() -> contratoService.findByFiltro(filtro, usuarioAutenticado.getEmpresaIdFromToken(), pageable)
                .map(usuarioPage -> usuarioPage.map(contratoMapper::toResponse))
                .orElse(Page.empty(pageable)));

//        Page<Contrato> entities = contratoService.findByFiltro(filtro, pageableHelp.getPageable(page, size, sortRequests)).orElse(Page.empty());
//        return entities.map(contratoMapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<ContratoResponse> fetchByIdContrato(@Argument Long id) {
        return contratoService.findById(id).map(contratoMapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public String apagarContrato(@Argument Integer id) {
        contratoService.deleteById(id);
        return SUCESSO;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<ContratoResponse> parseContrato(@Argument Long id) {
        log.info("Executando parse contrato com o id: {}", id);
        return contratoService.parseContrato(id)
                .map(contratoMapper::toResponse) // Se o Optional contiver um valor, aplica o mapper
                .switchIfEmpty(Mono.error(new BaseException("Não foi fazer o parse do contrato. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);
//        return contratoService.parseContrato(id).map(contratoMapper::toResponse);
//        return contratoService.parseContrato(id)
//                .map(contratoMapper::toResponse)
//                .orElseThrow(() -> new NoSuchElementException("Contrato com ID " + id + " não encontrado."));
        // Ou uma exceção mais específica, com
    }

}
