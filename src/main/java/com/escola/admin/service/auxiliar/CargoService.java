package com.escola.admin.service.auxiliar;

import com.escola.admin.model.entity.auxiliar.Cargo;
import com.escola.admin.model.request.auxiliar.CargoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface CargoService {

    Mono<Cargo> save(CargoRequest request);

    Mono<Cargo> findById(Long id);

    Optional<Page<Cargo>> findByFiltro(String filtro, Pageable pageable);

    Mono<Void> deleteById(Long id);

    Mono<Void> delete(Cargo entity);


}
