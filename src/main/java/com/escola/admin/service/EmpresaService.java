package com.escola.admin.service;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.request.EmpresaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface EmpresaService {

    Mono<Empresa> save(EmpresaRequest request);

    Mono<Empresa> findById(Long id);

    Optional<Page<Empresa>> findByFiltro(String filtro, Pageable pageable);

    Optional<Void> deleteById(Long id);

    Optional<Void> delete(Empresa empresa);

    Mono<Empresa> findEmpresaByUsuarioId(Long usuarioId);
}
