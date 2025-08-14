package com.escola.admin.service.auxiliar;

import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.auxiliar.StatusMatricula;
import com.escola.admin.model.request.auxiliar.MatriculaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface MatriculaService {

    Mono<Void> save(MatriculaRequest request, Long empresaIdFromToken);

    Mono<Matricula> findById(Long id);

    Mono<Matricula> findByIdWithClienteAndDependente(Long id);

    Optional<Page<Matricula>> findByTurma(Long idTurma, Pageable pageable);

    Mono<Void> deleteById(Long id);

    Mono<Void> delete(Matricula entity);

    Mono<Void> alterarStatus(Matricula matricula, StatusMatricula statusMatricula);
}
