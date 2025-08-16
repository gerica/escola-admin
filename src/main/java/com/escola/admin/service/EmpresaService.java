package com.escola.admin.service;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.response.EmpresaResponse;
import com.escola.admin.model.response.RelatorioBase64Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface EmpresaService {

    Mono<Void> save(EmpresaRequest request);

    Mono<Empresa> findById(Long id);

    Mono<EmpresaResponse> findEntityAndLogoById(Long id);

    Optional<Page<Empresa>> findByFiltro(String filtro, Pageable pageable);

    Optional<Void> deleteById(Long id);

    Optional<Void> delete(Empresa empresa);

    Mono<Empresa> findEmpresaByUsuarioId(Long usuarioId);

    Mono<RelatorioBase64Response> emitirRelatorio(FiltroRelatorioRequest request);
}
