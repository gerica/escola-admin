package com.escola.admin.service.auxiliar.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.ClienteDependente;
import com.escola.admin.model.mapper.auxiliar.MatriculaMapper;
import com.escola.admin.model.request.auxiliar.MatriculaRequest;
import com.escola.admin.repository.auxiliar.MatriculaRepository;
import com.escola.admin.service.auxiliar.MatriculaService;
import com.escola.admin.service.auxiliar.TurmaService;
import com.escola.admin.service.cliente.ClienteDependenteService;
import com.escola.admin.service.cliente.ClienteService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class MatriculaServiceImpl implements MatriculaService {

    MatriculaRepository repository;
    MatriculaMapper mapper;
    TurmaService turmaService;
    ClienteService clienteService;
    ClienteDependenteService clienteDependenteService;

    @Override
    @Transactional
    public Mono<Matricula> save(MatriculaRequest request) {
        log.info("Iniciando operação de salvar/atualizar matrícula. Request: {}", request);

        return validateMatriculaRequest(request) // Step 1: Validate the incoming request
                .then(getRequiredEntities(request)) // Step 2: Fetch all necessary entities concurrently
                .flatMap(context -> findOrCreateMatricula(request, context.turma, context.cliente, context.dependente)) // Step 3: Find or create the Matricula entity
                .flatMap(this::persistMatricula) // Step 4: Persist the Matricula
                .doOnSuccess(savedMatricula -> log.info("Matrícula salva com sucesso. ID: {}", savedMatricula.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar matrícula: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }

    // --- Private Helper Methods for Clarity and Maintainability ---

    // 1. Validates the incoming MatriculaRequest
    private Mono<Void> validateMatriculaRequest(MatriculaRequest request) {
        if (request.idTurma() == null) {
            return Mono.error(new BaseException("O ID da turma é obrigatório."));
        }
        if (request.idCliente() == null && request.idClienteDependente() == null) {
            return Mono.error(new BaseException("É obrigatório associar a matrícula a um Cliente ou a um Cliente Dependente."));
        }
        if (request.idCliente() != null && request.idClienteDependente() != null) {
            return Mono.error(new BaseException("A matrícula não pode estar associada a um Cliente E a um Cliente Dependente simultaneamente."));
        }
        return Mono.empty(); // Indicate success
    }

    private Mono<EntitiesContext> getRequiredEntities(MatriculaRequest request) {
        Mono<Turma> monoTurma = turmaService.findById(request.idTurma())
                .switchIfEmpty(Mono.error(new BaseException("Turma não encontrada com o ID: " + request.idTurma())));

        Mono<Cliente> monoCliente;
        Mono<ClienteDependente> monoDependente;

        if (request.idCliente() != null) {
            monoCliente = clienteService.findById(request.idCliente())
                    .switchIfEmpty(Mono.error(new BaseException("Cliente não encontrado com o ID: " + request.idCliente())));
            monoDependente = Mono.just(null); // Explicitly null if client is chosen
        } else if (request.idClienteDependente() != null) {
            monoCliente = Mono.just(null); // Explicitly null if dependent is chosen
            monoDependente = clienteDependenteService.findById(request.idClienteDependente())
                    .switchIfEmpty(Mono.error(new BaseException("Cliente Dependente não encontrado com o ID: " + request.idClienteDependente())));
        } else {
            monoCliente = Mono.just(null);
            monoDependente = Mono.just(null);
        }

        // Zip all three Monos. Since monoCliente and monoDependente are guaranteed to emit
        // either an entity or an explicit null, there's no need for .defaultIfEmpty(null)
        // on the zipped streams.
        return Mono.zip(monoTurma, monoCliente, monoDependente)
                .flatMap(tuple -> {
                    Turma turma = tuple.getT1();
                    Cliente cliente = tuple.getT2();
                    ClienteDependente dependente = tuple.getT3();

                    log.info("Entidades encontradas: Turma ID {} | Cliente ID {} | Dependente ID {}",
                            turma.getId(),
                            Optional.ofNullable(cliente).map(Cliente::getId).orElse(null),
                            Optional.ofNullable(dependente).map(ClienteDependente::getId).orElse(null)
                    );

                    // A final sanity check, though validateMatriculaRequest handles mutual exclusivity
                    if (request.idCliente() == null && request.idClienteDependente() == null) {
                        return Mono.error(new BaseException("É obrigatório associar a matrícula a um Cliente ou a um Cliente Dependente."));
                    }

                    // If all is well, wrap the EntitiesContext in a Mono.just()
                    return Mono.just(new EntitiesContext(turma, cliente, dependente));
                });
    }


    /**
     * Método auxiliar para encapsular a lógica de criação ou atualização da entidade Matricula.
     *
     * @param request    O DTO com os dados da requisição.
     * @param turma      A entidade Turma já buscada.
     * @param cliente    A entidade Cliente já buscada.
     * @param dependente A entidade ClienteDependente já buscada.
     * @return Um Mono contendo a entidade Matricula pronta para ser salva.
     */
    private Mono<Matricula> findOrCreateMatricula(MatriculaRequest request, Turma turma, Cliente cliente, ClienteDependente dependente) {
        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)
                        .switchIfEmpty(Mono.error(new BaseException("Matricula com ID " + id + " não encontrada para atualização.")))
                )
                .map(existingMatricula -> {
                    log.info("Atualizando matricula existente com ID: {}", existingMatricula.getId());
                    mapper.updateEntity(request, existingMatricula);
                    return existingMatricula;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova entidade de matricula para a turma '{}'", turma.getNome());
                    Matricula novaMatricula = mapper.toEntity(request);
                    novaMatricula.setTurma(turma);
                    novaMatricula.setCliente(cliente);
                    novaMatricula.setClienteDependente(dependente);
                    return Mono.just(novaMatricula);
                }));
    }

    // 5. Persists the Matricula entity (blocking operation)
    private Mono<Matricula> persistMatricula(Matricula matriculaToSave) {
        return Mono.fromCallable(() -> repository.save(matriculaToSave))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // --- Exception Handling (as in previous version) ---
    private Throwable handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("uc_matricula_turma_cliente")) {
                return new BaseException("Já existe uma matrícula para este cliente nesta turma.", e);
            }
            if (errorMessage.contains("uc_matricula_turma_dependente")) {
                return new BaseException("Já existe uma matrícula para este dependente nesta turma.", e);
            }
        }
        return new BaseException("Erro de integridade de dados ao salvar a matrícula.", e);
    }

    @Override
    public Mono<Matricula> findById(Long id) {
        log.info("Buscando turma por ID: {}", id);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(optionalMatricula -> {
                    if (optionalMatricula.isPresent()) {
                        log.info("Matricula encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalMatricula.get());
                    } else {
                        log.warn("Nenhum turma encontrado para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar turma por ID {}: {}", id, e.getMessage(), e));
    }

    @Override
    public Optional<Page<Matricula>> findByTurma(Long idTurma, Pageable pageable) {
        log.info("Buscando matriculas por turma '{}' com paginação: {}", idTurma, pageable);
        try {
            Optional<Page<Matricula>> result = repository.findByIdTurma(idTurma, pageable);
            if (result.isPresent()) {
                log.info("Encontrados {} matriculas paginados para o turma '{}'.", result.get().getTotalElements(), idTurma);
            } else {
                log.warn("Nenhum resultado de paginação para o turma '{}'.", idTurma);
            }
            return result;
        } catch (Exception e) {
            log.error("Erro ao buscar matriculas por turma '{}' e paginação {}: {}", idTurma, pageable, e.getMessage(), e);
            throw e; // Re-lança a exceção após logar
        }
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        log.info("Solicitada exclusão de turma por ID: {}", id);
        return Mono.fromRunnable(() -> repository.deleteById(id))
                .doOnSuccess(v -> log.info("Matricula com ID {} excluído com sucesso.", id))
                .doOnError(e -> log.error("Erro ao excluir turma por ID {}: {}", id, e.getMessage(), e)).then();
    }

    @Override
    public Mono<Void> delete(Matricula entity) {
        log.info("Solicitada exclusão de turma pela entidade. ID: {}", entity != null ? entity.getId() : "null");
        return Mono.fromRunnable(() -> repository.delete(entity))
                .doOnSuccess(v -> log.info("Matricula com ID {} excluído com sucesso pela entidade.", entity != null ? entity.getId() : "null"))
                .doOnError(e -> log.error("Erro ao excluir turma pela entidade {}: {}", entity != null ? entity.getId() : "null", e.getMessage(), e)).then();
    }

    // 2. Data structure to hold the fetched entities
    private record EntitiesContext(Turma turma, Cliente cliente, ClienteDependente dependente) {
    }

}