package com.escola.admin.service.auxiliar.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.auxiliar.Cargo;
import com.escola.admin.model.mapper.auxiliar.CargoMapper;
import com.escola.admin.model.request.auxiliar.CargoRequest;
import com.escola.admin.repository.auxiliar.CargoRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.auxiliar.CargoService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class CargoServiceImpl implements CargoService {

    CargoRepository repository;
    CargoMapper mapper;
    EmpresaService empresaService;

    @Override
    public Mono<Cargo> save(CargoRequest request) {
        log.info("Iniciando operação de salvar/atualizar cargo. Request ID: {}, Empresa ID: {}", request.id(), request.idEmpresa());

        // 1. Validar idEmpresa
        if (request.idEmpresa() == null) {
            log.error("Tentativa de salvar cargo sem idEmpresa fornecido.");
            return Mono.error(new BaseException("O ID da empresa é obrigatório para salvar um cargo."));
        }

        // 2. Buscar a empresa pelo ID
        // Como empresaService.findById retorna Mono<Empresa>, precisamos usar flatMap para encadear.
        return empresaService.findById(request.idEmpresa())
                .flatMap(empresa -> {
                    if (empresa == null) {
                        log.warn("Empresa com ID {} não encontrada para vincular ao cargo.", request.idEmpresa());
                        return Mono.error(new BaseException("Empresa não encontrada com o ID fornecido: " + request.idEmpresa()));
                    }
                    log.info("Empresa com ID {} encontrada.", empresa.getId());

                    // 3. Lógica existente para encontrar ou criar o Cargo
                    return Mono.justOrEmpty(request.id())
                            .flatMap(id -> Mono.fromCallable(() -> repository.findById(id)))
                            .flatMap(Mono::justOrEmpty)
                            .map(existingCargo -> {
                                log.info("Atualizando cargo existente com ID: {}", existingCargo.getId());
                                mapper.updateEntity(request, existingCargo);
                                return existingCargo;
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("Criando nova entidade de cargo.");
                                Cargo newCargo = mapper.toEntity(request);
                                newCargo.setEmpresa(empresa); // **Associa a empresa ao novo cargo**
                                return Mono.just(newCargo);
                            }))
                            // 4. Salvar o Cargo
                            .flatMap(cargoToSave -> Mono.fromCallable(() -> repository.save(cargoToSave))
                                    .doOnSuccess(savedCargo -> log.info("Cargo salvo com sucesso. ID: {}", savedCargo.getId()))
                                    .doOnError(e -> log.error("Erro ao salvar cargo: {}", e.getMessage(), e))
                            )
                            .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                            .onErrorMap(e -> !(e instanceof BaseException), e -> {
                                log.error("Ocorreu um erro genérico não esperado ao salvar o cargo: {}", e.getMessage(), e);
                                return BaseException.handleGenericException(e);
                            });
                })
                .doOnError(e -> { // Loga erros que podem vir do flatMap da busca da empresa
                    if (!(e instanceof BaseException)) {
                        log.error("Erro inesperado antes de salvar o cargo após buscar a empresa: {}", e.getMessage(), e);
                    }
                });
    }

    @Override
    public Mono<Cargo> findById(Long id) {
        log.info("Buscando cargo por ID: {}", id);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(optionalCargo -> {
                    if (optionalCargo.isPresent()) {
                        log.info("Cargo encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalCargo.get());
                    } else {
                        log.warn("Nenhum cargo encontrado para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar cargo por ID {}: {}", id, e.getMessage(), e));
    }

    @Override
    public Optional<Page<Cargo>> findByFiltro(String filtro, Pageable pageable) {
        log.info("Buscando cargos por filtro '{}' com paginação: {}", filtro, pageable);
        try {
            Optional<Page<Cargo>> result = repository.findByFiltro(filtro, pageable);
            if (result.isPresent()) {
                log.info("Encontrados {} cargos paginados para o filtro '{}'.", result.get().getTotalElements(), filtro);
            } else {
                log.warn("Nenhum resultado de paginação para o filtro '{}'.", filtro);
            }
            return result;
        } catch (Exception e) {
            log.error("Erro ao buscar cargos por filtro '{}' e paginação {}: {}", filtro, pageable, e.getMessage(), e);
            throw e; // Re-lança a exceção após logar
        }
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        log.info("Solicitada exclusão de cargo por ID: {}", id);
        return Mono.fromRunnable(() -> repository.deleteById(id))
                .doOnSuccess(v -> log.info("Cargo com ID {} excluído com sucesso.", id))
                .doOnError(e -> log.error("Erro ao excluir cargo por ID {}: {}", id, e.getMessage(), e)).then();
    }

    @Override
    public Mono<Void> delete(Cargo entity) {
        log.info("Solicitada exclusão de cargo pela entidade. ID: {}", entity != null ? entity.getId() : "null");
        return Mono.fromRunnable(() -> repository.delete(entity))
                .doOnSuccess(v -> log.info("Cargo com ID {} excluído com sucesso pela entidade.", entity != null ? entity.getId() : "null"))
                .doOnError(e -> log.error("Erro ao excluir cargo pela entidade {}: {}", entity != null ? entity.getId() : "null", e.getMessage(), e)).then();
    }

    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar cargo: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (nome)")) {
                errorMessage = "Já existe um cargo com este nome. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o cargo.";
        }
        log.error("Erro de integridade de dados processado: {}", errorMessage, e); // Mantendo log.error para a exceção original
        return new BaseException(errorMessage, e);
    }
}