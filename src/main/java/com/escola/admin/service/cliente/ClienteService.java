package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.request.cliente.ClienteRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.response.RelatorioBase64Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface ClienteService {
    Mono<Cliente> save(ClienteRequest request);

    Optional<Page<Cliente>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable);

    Mono<Cliente> findById(Long id);

    Optional<Page<Cliente>> findAtivosByFiltro(String filtro, Long empresaIdFromToken, Pageable pageable);

    Optional<Page<Cliente>> findAllClientsByStatusAndFiltroWithDependents(String filtro, Long empresaIdFromToken, Pageable pageable);

    Mono<Void> deleteById(Long id);

    Mono<RelatorioBase64Response> emitirRelatorio(FiltroRelatorioRequest request, Usuario empresaIdFromToken);
}
