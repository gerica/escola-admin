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
import com.escola.admin.service.cliente.ContratoService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MatriculaServiceImpl implements MatriculaService {

    MatriculaRepository repository;
    MatriculaMapper mapper;
    TurmaService turmaService;
    ContratoService contratoService;
    ClienteService clienteService;
    ClienteDependenteService clienteDependenteService;

    public MatriculaServiceImpl(MatriculaRepository repository,
                                MatriculaMapper mapper,
                                TurmaService turmaService,
                                @Lazy ContratoService contratoService,
                                ClienteService clienteService,
                                ClienteDependenteService
                                        clienteDependenteService) {
        this.repository = repository;
        this.mapper = mapper;
        this.turmaService = turmaService;
        this.contratoService = contratoService;
        this.clienteService = clienteService;
        this.clienteDependenteService = clienteDependenteService;
    }

    @Override
    @Transactional
    public Mono<Void> save(MatriculaRequest request) {
        log.info("Iniciando operação de salvar/atualizar matrícula. Request: {}", request);

        return validateMatriculaRequest(request) // Step 1: Validate the incoming request
                .then(getRequiredEntities(request)) // Step 2: Fetch all necessary entities concurrently
                .flatMap(context -> findOrCreateMatricula(request, context.turma, context.cliente, context.dependente)) // Step 3: Find or create the Matricula entity
                .flatMap(this::persistMatricula) // Step 4: Persist the Matricula
                .flatMap(context -> {
                    if (context.isNew()) {
                        log.info("Matrícula é nova. Criando contrato para ID: {}", context.matricula().getId());
                        return contratoService.criarContrato(context.matricula());
                    } else {
                        log.info("Matrícula é uma atualização. Pulando a criação de contrato.");
                        return Mono.just(context.matricula());
                    }
                })
                .doOnSuccess(savedMatricula -> log.info("Matrícula salva com sucesso. ID: {}", savedMatricula.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar matrícula: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException)
                .then();
    }

    private Mono<Matricula> buscarEntidadesLazy(Matricula savedMatricula) {
        log.info("Iniciar inicialização de associações lazy para Matrícula ID: {}", savedMatricula.getId());
        return Mono.fromCallable(() -> {
            Optional<Matricula> test = repository.findByIdWithClienteAndDependente(savedMatricula.getId());
//            System.out.println(test.get().getTurma().getNome());
            // Inicializa a Turma (se for LAZY) e suas associações aninhadas
            if (savedMatricula.getTurma() != null) {
                savedMatricula.getTurma().getId(); // Acessa para inicializar Turma
                log.info("Turma inicializada: {}", savedMatricula.getTurma().getNome());
            }

            // Inicializa o Cliente (se for LAZY) e suas associações aninhadas
            if (savedMatricula.getCliente() != null) {
                savedMatricula.getCliente().getId(); // Acessa para inicializar Cliente
                log.info("Cliente inicializado: {}", savedMatricula.getCliente().getId());
            }

            // Inicializa o ClienteDependente (se for LAZY) e suas associações aninhadas
            if (savedMatricula.getClienteDependente() != null) {
                savedMatricula.getClienteDependente().getId(); // Acessa para inicializar ClienteDependente
                log.info("Cliente Dependente inicializado: {}", savedMatricula.getClienteDependente().getId());

            }
            return test.get(); // Retorna a matrícula com as associações inicializadas
        }).subscribeOn(Schedulers.boundedElastic()); // Garante que a inicialização bloqueante rode em um thread adequado
    }

    private Mono<Matricula> buscarEntidadesLazy2(Matricula savedMatricula) {
        log.info("Iniciar lazys");
        return Mono.fromCallable(() -> {
            if (savedMatricula.getCliente() != null) {
                log.debug("Inicializando lazy client: {}", savedMatricula.getCliente().getId());
            }
            if (savedMatricula.getClienteDependente() != null) {
                log.debug("Inicializando lazy client dependent: {}", savedMatricula.getClienteDependente().getId());
            }
            if (savedMatricula.getTurma() != null) {
                log.debug("Inicializando lazy turma: {}", savedMatricula.getTurma().getId());
            }
            return savedMatricula;
        }).subscribeOn(Schedulers.boundedElastic());
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

        // O Mono que conterá o Cliente OU o ClienteDependente, mas não ambos
        Mono<Object> monoClienteOuDependente; // Usamos Object temporariamente para Mono.zip

        if (request.idCliente() != null) {
            log.debug("Buscando Cliente pelo ID: {}", request.idCliente());
            monoClienteOuDependente = clienteService.findById(request.idCliente())
                    .switchIfEmpty(Mono.error(new BaseException("Cliente não encontrado com o ID: " + request.idCliente())))
                    .cast(Object.class); // Converte para Object para que Mono.zip possa lidar com tipos heterogêneos
        } else if (request.idClienteDependente() != null) {
            log.debug("Buscando ClienteDependente pelo ID: {}", request.idClienteDependente());
            monoClienteOuDependente = clienteDependenteService.findById(request.idClienteDependente())
                    .switchIfEmpty(Mono.error(new BaseException("Cliente Dependente não encontrado com o ID: " + request.idClienteDependente())))
                    .cast(Object.class); // Converte para Object
        } else {
            // Este caso já deveria ter sido pego por validateMatriculaRequest, mas é uma segurança.
            return Mono.error(new BaseException("É obrigatório associar a matrícula a um Cliente ou a um Cliente Dependente."));
        }

        return Mono.zip(monoTurma, monoClienteOuDependente) // Zipando apenas 2 Monos agora
                .flatMap(tuple -> {
                    Turma turma = tuple.getT1();
                    Object clienteOuDependente = tuple.getT2(); // Pode ser Cliente ou ClienteDependente

                    Cliente cliente = null;
                    ClienteDependente dependente = null;

                    if (clienteOuDependente instanceof Cliente) {
                        cliente = (Cliente) clienteOuDependente;
                        log.info("Entidades encontradas: Turma ID {} | Cliente ID {} | Dependente ID {}",
                                turma.getId(), cliente.getId(), "N/A (cliente principal)");
                    } else if (clienteOuDependente instanceof ClienteDependente) {
                        dependente = (ClienteDependente) clienteOuDependente;
                        log.info("Entidades encontradas: Turma ID {} | Cliente ID {} | Dependente ID {}",
                                turma.getId(), "N/A (dependente)", dependente.getId());
                    } else {
                        // Este é um caso de erro grave, pois clienteOuDependente deveria ser um dos tipos
                        log.error("CRITICAL ERROR: Tipo inesperado encontrado após zip: {}", clienteOuDependente.getClass().getName());
                        return Mono.error(new BaseException("Erro interno ao processar tipo de cliente/dependente."));
                    }

                    // Cria o contexto com um dos valores nulo, conforme a lógica de negócio
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
    private Mono<MatriculaContext> findOrCreateMatricula(MatriculaRequest request, Turma turma, Cliente cliente, ClienteDependente dependente) {
        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)
                        .switchIfEmpty(Mono.error(new BaseException("Matricula com ID " + id + " não encontrada para atualização.")))
                )
                .map(existingMatricula -> {
                    log.info("Atualizando matricula existente com ID: {}", existingMatricula.getId());
                    mapper.updateEntity(request, existingMatricula);
                    return new MatriculaContext(existingMatricula, false); // É uma atualização, então isNew = false
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova entidade de matricula para a turma '{}'", turma.getNome());
                    Matricula novaMatricula = mapper.toEntity(request);
                    novaMatricula.setTurma(turma);
                    novaMatricula.setCliente(cliente);
                    novaMatricula.setClienteDependente(dependente);

                    return Mono.fromCallable(() -> {
                                // 1. Busca a última matrícula para esta turma (bloqueante)
                                Optional<Matricula> lastMatriculaOpt = repository.findTopByTurmaIdOrderByCodigoDesc(turma.getId());
                                int nextSequenceNum = 1; // Começa em 1 se não houver matrículas anteriores

                                if (lastMatriculaOpt.isPresent()) {
                                    String lastCodigo = lastMatriculaOpt.get().getCodigo();
                                    // 2. Extrai o número do último código
                                    try {
                                        // Pega a parte após o último hífen, que deve ser o número
                                        String numPart = lastCodigo.substring(lastCodigo.lastIndexOf('-') + 1);
                                        nextSequenceNum = Integer.parseInt(numPart) + 1;
                                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                        log.warn("Formato de código de matrícula inesperado para incremento: {}. Reiniciando a sequência para 1.", lastCodigo);
                                        // Em caso de erro de formato, reinicia a sequência para garantir
                                        nextSequenceNum = 1;
                                    }
                                }

                                // 3. Formata o novo código (ex: MUS-B-24-001)
                                String newCodigo = String.format("%s-%03d", turma.getCodigo(), nextSequenceNum);
                                novaMatricula.setCodigo(newCodigo);
                                return new MatriculaContext(novaMatricula, true);
                            })
                            .subscribeOn(Schedulers.boundedElastic());// Executa a operação de busca no banco em um pool de threads separado
                }));
    }

    // 5. Persists the Matricula entity (blocking operation)
    private Mono<MatriculaContext> persistMatricula(MatriculaContext context) {
        return Mono.fromCallable(() -> repository.save(context.matricula()))
                .map(savedMatricula -> new MatriculaContext(savedMatricula, context.isNew()))
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
    public Mono<Matricula> findByIdWithClienteAndDependente(Long id) {
        log.info("Buscando turma, cliente e depentente por ID: {}", id);
        return Mono.fromCallable(() -> repository.findByIdWithClienteAndDependente(id))
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
        log.info("Iniciando a exclusão da matricula com ID: {}", id);

        // 1. Apaga todos os contratos relacionados à matrícula de forma reativa.
        // O .then() garante que a próxima operação só comece após a conclusão da anterior.
        return contratoService.deleteByIdMatricula(id)
                .then(Mono.fromRunnable(() -> repository.deleteById(id))) // 2. Apaga a matrícula, encapsulando a chamada bloqueante.
                .doOnSuccess(v -> log.info("Matricula com ID {} e contratos relacionados excluídos com sucesso.", id))
                .doOnError(e -> log.error("Erro ao excluir matricula por ID {}: {}", id, e.getMessage(), e))
                .then(); // Garante o retorno de um Mono<Void> final
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

    private record MatriculaContext(Matricula matricula, boolean isNew) {
    }
}