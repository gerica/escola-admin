package com.escola.admin.service.auxiliar;

import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.request.auxiliar.CursoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface CursoService {

    Mono<Curso> save(CursoRequest request);

    Mono<Curso> findById(Long id);

    Optional<Page<Curso>> findByFiltro(String filtro, Pageable pageable);

    Mono<Void> deleteById(Long id);

    Mono<Void> delete(Curso entity);

}
