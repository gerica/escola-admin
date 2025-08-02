package com.escola.admin.controller.cliente;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.controller.help.SortInput;
import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.cliente.ClienteMapper;
import com.escola.admin.model.request.cliente.ClienteRequest;
import com.escola.admin.model.response.cliente.ClienteResponse;
import com.escola.admin.service.cliente.ClienteService;
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

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClienteController {

    ClienteMapper clienteMapper;
    ClienteService clienteService;
    PageableHelp pageableHelp;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<ClienteResponse> saveCliente(@Argument ClienteRequest request, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario usuarioAutenticado)) {
            return Mono.error(new IllegalStateException("Principal não é do tipo Usuario."));
        }
        // Create a new CargoRequest with the updated idEmpresa
        ClienteRequest updatedRequest = new ClienteRequest(
                request.id(),
                usuarioAutenticado.getEmpresaIdFromToken(), // Set the idEmpresa here
                request.nome(),
                request.dataNascimento(),
                request.cidade(),
                request.uf(),
                request.codigoCidade(),
                request.docCPF(),
                request.docRG(),
                request.telResidencial(),
                request.telCelular(),
                request.endereco(),
                request.email(),
                request.profissao(),
                request.localTrabalho(),
                request.statusCliente()
        );
        return clienteService.save(updatedRequest)
                .map(clienteMapper::toResponse)
//                .then(Mono.just("Operação realizada com sucesso."))
                .switchIfEmpty(Mono.error(new BaseException("Não foi possível salvar cliente. O serviço retornou um resultado vazio.")))
                .onErrorResume(BaseException.class, Mono::error);

    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<ClienteResponse>> fetchAllClientes(
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


        return Mono.fromCallable(() -> clienteService.findByFiltro(filtro, usuarioAutenticado.getEmpresaIdFromToken(), pageable)
                .map(usuarioPage -> usuarioPage.map(clienteMapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<ClienteResponse>> fetchAllClientesAtivos(
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

        return Mono.fromCallable(() -> clienteService.findAtivosByFiltro(filtro, usuarioAutenticado.getEmpresaIdFromToken(), pageable)
                .map(usuarioPage -> usuarioPage.map(clienteMapper::toResponse))
                .orElse(Page.empty(pageable)));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<Page<ClienteResponse>> fetchAllClientsByStatusAndFiltroWithDependents(
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

        return Mono.fromCallable(() -> clienteService.findAllClientsByStatusAndFiltroWithDependents(filtro, usuarioAutenticado.getEmpresaIdFromToken(), pageable)
                .map(usuarioPage -> usuarioPage.map(clienteMapper::toResponseComDependentes))
                .orElse(Page.empty(pageable)));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<ClienteResponse> fetchByIdCliente(@Argument Long id) {
        return clienteService.findById(id).map(clienteMapper::toResponse);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public Mono<String> deleteClienteById(@Argument Long id) {
        return clienteService.deleteById(id)
                .then(Mono.just("Operação realizada com sucesso."))
                .onErrorResume(BaseException.class, Mono::error);
//                .onErrorResume(e -> Mono.error(new GraphQLException("Falha realizar operação: " + e.getMessage())));
    }
}
