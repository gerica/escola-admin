package com.escola.admin.service.auxiliar;

import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.request.auxiliar.TurmaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface TurmaService {

    Mono<Turma> save(TurmaRequest request);

    Mono<Turma> findById(Long id);

    Optional<Page<Turma>> findByFiltro(String filtro, Pageable pageable);

    Mono<Void> deleteById(Long id);

    Mono<Void> delete(Turma entity);

}
