package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.cliente.ClienteDependente;
import com.escola.admin.model.mapper.cliente.ClienteDependenteMapper;
import com.escola.admin.model.request.cliente.ClienteDependenteRequest;
import com.escola.admin.repository.cliente.ClienteDependenteRepository;
import com.escola.admin.service.cliente.ClienteDependenteService;
import com.escola.admin.service.cliente.ClienteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ClienteDependenteServiceImpl implements ClienteDependenteService {

    ClienteDependenteRepository repository;
    ClienteService clienteService;
    ClienteDependenteMapper mapper;

    @Override
    public Mono<ClienteDependente> save(ClienteDependenteRequest request) {

        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id)))
                .flatMap(Mono::justOrEmpty).map((existingEmpresa) -> {
                    mapper.updateEntity(request, existingEmpresa);
                    return existingEmpresa;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ClienteDependente newEntity = mapper.toEntity(request);
                    return clienteService.findById(request.idCliente())
                            .switchIfEmpty(Mono.error(new BaseException("Cliente com o ID " + request.idCliente() + " não encontrado.")))
                            .map(cliente -> {
                                newEntity.setCliente(cliente);
                                return newEntity; // Retorna a nova entidade com o cliente associado
                            });
                }))
                .flatMap(uwp -> Mono.fromCallable(() -> repository.save(uwp)))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }


    private Throwable handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return new BaseException("Erro de integridade de dados ao salvar Cliente Dependente: " + ex.getMessage());
    }

    @Override
    public Optional<Boolean> apagar(Long id) {
        repository.deleteById(id);
        return Optional.of(true);
    }

    @Override
    public Mono<ClienteDependente> findById(Long id) {
        log.info("Buscando cliente dependente por ID: {}", id);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(optionalCargo -> {
                    if (optionalCargo.isPresent()) {
                        log.info("ClienteDependente encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalCargo.get());
                    } else {
                        log.warn("Nenhum cargo ClienteDependente para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar ClienteDependente por ID {}: {}", id, e.getMessage(), e));
    }

    @Override
    public Optional<List<ClienteDependente>> findAllByClienteId(Long id) {
        return repository.findAllByClienteId(id);
    }
}
