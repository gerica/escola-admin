package com.escola.admin.controller.auxiliar;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.mapper.auxiliar.CargoMapper;
import com.escola.admin.model.request.auxiliar.CargoRequest;
import com.escola.admin.model.response.auxiliar.CargoResponse;
import com.escola.admin.service.auxiliar.CargoService;
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
public class CargoController {

    CargoMapper cargoMapper;
    CargoService cargoService;
    PageableHelp pageableHelp; // Assuming you'll use this for pagination

    /**
     * Saves or updates a Cargo.
     * Accessible only to authenticated users.
     *
     * @param request The CargoRequest containing the data to save/update.
     * @return The saved or updated Cargo as a CargoResponse.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<CargoResponse> saveCargo(@Argument CargoRequest request) {
        // The service method returns Mono<Cargo>, so we map it to CargoResponse
        return cargoService.save(request)
                .map(cargoMapper::toResponse)
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar cardo. O serviço retornou um resultado vazio."))) // Use switchIfEmpty for empty Mono
                .onErrorResume(BaseException.class, Mono::error); // This isn't strictly necessary if BaseException is already Mono.error from service, but good for clarity
    }

    /**
     * Fetches a paginated list of Cargos, optionally filtered by name.
     * Accessible only to authenticated users.
     *
     * @param filtro       Optional filter string for cargo name.
     * @param page         The page number (0-indexed).
     * @param size         The number of items per page.
     * @param sortRequests List of sorting criteria.
     * @return A Mono containing a Page of CargoResponse.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<CargoResponse>> fetchAllCargos(
            @Argument String filtro,
            @Argument int page,
            @Argument int size,
            @Argument(name = "sort") List<SortInput> sortRequests) {

        // Use PageableHelp to construct the Pageable object
        Pageable pageable = pageableHelp.getPageable(page, size, sortRequests);

        // Call the service method, map the Optional<Page<Cargo>> to Mono<Page<CargoResponse>>
        // and handle the Optional.empty() case by returning an empty page.
        return Mono.fromCallable(() -> cargoService.findByFiltro(filtro, pageable)
                .map(cargoPage -> cargoPage.map(cargoMapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    /**
     * Fetches a single Cargo by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Cargo to fetch.
     * @return An Optional containing the CargoResponse if found, otherwise empty.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<CargoResponse> fetchByIdCargo(@Argument Long id) { // ID for Cargo is Long
        // The service method returns Mono<Cargo>, so we block it here to return Optional<Cargo>
        // and then map it to CargoResponse.
        // NOTE: Blocking in a reactive context (Mono.block()) should be used with caution,
        // typically at the "edges" of your application (like controllers that need to return
        // a synchronous value for some reason, or specific integration points).
        // For GraphQL, it often means the GraphQL execution engine is synchronous.
        return cargoService.findById(id).blockOptional().map(cargoMapper::toResponse);
    }

    /**
     * Deletes a Cargo by its ID.
     * Accessible only to authenticated users.
     *
     * @param id The ID of the Cargo to delete.
     * @return A Mono<Void> indicating completion.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> deleteCargoById(@Argument Long id) {
        return cargoService.deleteById(id)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(e -> Mono.error(new GraphQLException("Falha realizar operação: " + e.getMessage())));
    }
}