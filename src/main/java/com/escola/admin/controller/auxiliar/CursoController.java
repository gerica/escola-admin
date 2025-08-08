package com.escola.admin.controller.auxiliar;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.auxiliar.CursoMapper;
import com.escola.admin.model.request.auxiliar.CursoRequest;
import com.escola.admin.model.response.auxiliar.CursoResponse;
import com.escola.admin.service.auxiliar.CursoService;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CursoController {

    CursoMapper cursoMapper;
    CursoService cursoService;
    PageableHelp pageableHelp;

    /**
     * Saves or updates a Curso.
     * Accessible only to authenticated users.
     *
     * @param request The CursoRequest containing the data to save/update.
     * @return The saved or updated Curso as a CursoResponse.
     */
    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public Mono<CursoResponse> saveCurso(@Argument CursoRequest request, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }
        // Create a new CargoRequest with the updated idEmpresa
        CursoRequest updatedRequest = new CursoRequest(
                request.id(),
                usuarioAutenticado.getEmpresaIdFromToken(), // Set the idEmpresa here
                request.nome(),
                request.descricao(),
                request.duracaoValor(),
                request.duracaoUnidade(),
                request.categoria(),
                request.valorMensalidade(),
                request.ativo()
        );
        return cursoService.save(updatedRequest)
                .map(cursoMapper::toResponse)
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar curso. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);
    }

    /**
     * Fetches a paginated list of Cursos, optionally filtered by name.
     * Accessible only to authenticated users.
     *
     * @param filtro       Optional filter string for curso name.
     * @param page         The page number (0-indexed).
     * @param size         The number of items per page.
     * @param sortRequests List of sorting criteria.
     * @return A Mono containing a Page of CursoResponse.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<CursoResponse>> fetchAllCursos(
            @Argument String filtro,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests) {

        Pageable pageable = pageableHelp.getPageable(page, size, sortRequests);

        return Mono.fromCallable(() -> cursoService.findByFiltro(filtro, pageable)
                .map(cursoPage -> cursoPage.map(cursoMapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    /**
     * Fetches a single Curso by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Curso to fetch.
     * @return An Optional containing the CursoResponse if found, otherwise empty.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<CursoResponse> fetchByIdCurso(@Argument Long id) { // ID for Curso is Long
        return cursoService.findById(id).blockOptional().map(cursoMapper::toResponse);
    }

    /**
     * Deletes a Curso by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Curso to delete.
     * @return A Mono<Void> indicating completion.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> deleteCursoById(@Argument Long id) {
        return cursoService.deleteById(id)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha realizar operação: " + e.getMessage())));
    }
}