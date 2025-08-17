package com.escola.admin.controller.auxiliar;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.entity.auxiliar.StatusTurma;
import com.escola.admin.model.mapper.auxiliar.TurmaMapper;
import com.escola.admin.model.request.auxiliar.TurmaRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.model.response.auxiliar.TurmaResponse;
import com.escola.admin.service.auxiliar.TurmaService;
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
public class TurmaController {

    TurmaMapper turmaMapper;
    TurmaService turmaService;
    PageableHelp pageableHelp;

    /**
     * Saves or updates a Turma.
     * Accessible only to authenticated users.
     *
     * @param request The TurmaRequest containing the data to save/update.
     * @return The saved or updated Turma as a TurmaResponse.
     */
    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public Mono<TurmaResponse> saveTurma(@Argument TurmaRequest request, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }
        // Create a new CargoRequest with the updated idEmpresa
        TurmaRequest updatedRequest = new TurmaRequest(
                request.id(),
                request.idCurso(),
                usuarioAutenticado.getEmpresaIdFromToken(), // Set the idEmpresa here
                request.nome(),
                request.codigo(),
                request.capacidadeMaxima(),
                request.status(),
                request.anoPeriodo(),
                request.horarioInicio(),
                request.horarioFim(),
                request.diasDaSemana(),
                request.professor(),
                request.dataInicio(),
                request.dataFim()
        );
        return turmaService.save(updatedRequest)
                .map(turmaMapper::toResponse)
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar turma. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);
    }

    /**
     * Fetches a paginated list of Turmas, optionally filtered by name.
     * Accessible only to authenticated users.
     *
     * @param filtro       Optional filter string for turma name.
     * @param page         The page number (0-indexed).
     * @param size         The number of items per page.
     * @param sortRequests List of sorting criteria.
     * @return A Mono containing a Page of TurmaResponse.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<TurmaResponse>> fetchAllTurmas(
            @Argument String filtro,
            @Argument List<StatusTurma> status,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests,
            Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }

        Pageable pageable = pageableHelp.getPageable(page, size, sortRequests);

        return Mono.fromCallable(() -> turmaService.findByFiltro(filtro, status, usuarioAutenticado.getEmpresaIdFromToken(), pageable)
                .map(turmaPage -> turmaPage.map(turmaMapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    /**
     * Fetches a single Turma by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Turma to fetch.
     * @return An Optional containing the TurmaResponse if found, otherwise empty.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<TurmaResponse> fetchByIdTurma(@Argument Long id) { // ID for Turma is Long
        return turmaService.findById(id).blockOptional().map(turmaMapper::toResponse);
    }

    /**
     * Deletes a Turma by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Turma to delete.
     * @return A Mono<Void> indicating completion.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> deleteTurmaById(@Argument Long id) {
        return turmaService.deleteById(id)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(BaseException.class, Mono::error);
    }

    @QueryMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public Mono<RelatorioBase64Response> downloadListaTurmas(@Argument FiltroRelatorioRequest request,
                                                               Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }

        return turmaService.emitirRelatorio(request, usuarioAutenticado)
                .onErrorResume(BaseException.class, Mono::error);
    }
}