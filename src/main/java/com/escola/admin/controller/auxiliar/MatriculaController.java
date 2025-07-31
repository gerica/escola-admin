package com.escola.admin.controller.auxiliar;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.mapper.auxiliar.MatriculaMapper;
import com.escola.admin.model.request.auxiliar.MatriculaRequest;
import com.escola.admin.model.response.auxiliar.MatriculaResponse;
import com.escola.admin.service.auxiliar.MatriculaService;
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
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MatriculaController {

    MatriculaMapper matriculaMapper;
    MatriculaService matriculaService;
    PageableHelp pageableHelp;

    /**
     * Saves or updates a Matricula.
     * Accessible only to authenticated users.
     *
     * @param request The MatriculaRequest containing the data to save/update.
     * @return The saved or updated Matricula as a MatriculaResponse.
     */
    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public Mono<String> saveMatricula(@Argument MatriculaRequest request) {
        return matriculaService.save(request)
                .then(Mono.just("Operação realizada com sucesso."))
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar turma. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);
    }

    /**
     * Fetches a paginated list of Matriculas, optionally filtered by name.
     * Accessible only to authenticated users.
     *
     * @param idTurma      id da turma.
     * @param page         The page number (0-indexed).
     * @param size         The number of items per page.
     * @param sortRequests List of sorting criteria.
     * @return A Mono containing a Page of MatriculaResponse.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<MatriculaResponse>> fetchAllMatriculas(
            @Argument Long idTurma,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests) {

        Pageable pageable = pageableHelp.getPageable(page, size, sortRequests);

        return Mono.fromCallable(() -> matriculaService.findByTurma(idTurma, pageable)
                .map(turmaPage -> turmaPage.map(matriculaMapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    /**
     * Fetches a single Matricula by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Matricula to fetch.
     * @return An Optional containing the MatriculaResponse if found, otherwise empty.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<MatriculaResponse> fetchByIdMatricula(@Argument Long id) { // ID for Matricula is Long
        return matriculaService.findById(id).blockOptional().map(matriculaMapper::toResponse);
    }

    /**
     * Deletes a Matricula by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Matricula to delete.
     * @return A Mono<Void> indicating completion.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> deleteMatriculaById(@Argument Long id) {
        return matriculaService.deleteById(id)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha realizar operação: " + e.getMessage())));
    }
}