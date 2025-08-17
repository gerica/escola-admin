package com.escola.admin.service.auxiliar.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.entity.auxiliar.StatusTurma;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.mapper.auxiliar.TurmaMapper;
import com.escola.admin.model.request.auxiliar.TurmaRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.repository.auxiliar.TurmaRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.FileStorageService;
import com.escola.admin.service.auxiliar.CursoService;
import com.escola.admin.service.auxiliar.TurmaService;
import com.escola.admin.service.report.ReportService;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class TurmaServiceImpl implements TurmaService {

    TurmaRepository repository;
    TurmaMapper mapper;
    EmpresaService empresaService;
    CursoService cursoService;
    ReportService<Turma> reportService;
    FileStorageService storageService;

    @Override
    public Mono<Turma> save(TurmaRequest request) {
        return validateRequest(request) // Passo 1: Valida a requisição de entrada
                .then(getRequiredEntities(request)) // Passo 2: Busca todas as entidades necessárias concorrentemente
                .flatMap(context -> updateOrCreate(request, context)) // Passo 3: Encontra ou cria a entidade ContaReceber
                .flatMap(this::persist) // Passo 4: Persiste a ContaReceber
                .flatMap(entity -> findByIdAndLoadCurso(entity.getId()))
                .doOnSuccess(savedEntity -> log.info("Turma salvo com sucesso. ID: {}", savedEntity.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar turma: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }

    /**
     * Método auxiliar para encapsular a lógica de criação ou atualização da entidade Turma.
     *
     * @param request O DTO com os dados da requisição.
     * @param empresa A entidade Empresa já buscada.
     * @param curso   A entidade Curso já buscada.
     * @return Um Mono contendo a entidade Turma pronta para ser salva.
     */
    private Mono<Turma> findOrCreateTurma(TurmaRequest request, Empresa empresa, Curso curso) {
        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)
                        // Se o ID foi fornecido mas a turma não existe, é um erro.
                        .switchIfEmpty(Mono.error(new BaseException("Turma com ID " + id + " não encontrada para atualização.")))
                )
                .map(existingTurma -> {
                    log.info("Atualizando turma existente com ID: {}", existingTurma.getId());
                    mapper.updateEntity(request, existingTurma);
                    existingTurma.setCurso(curso);     // Associa o curso
                    return existingTurma;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova entidade de turma para o curso '{}'", curso.getNome());
                    Turma novaTurma = mapper.toEntity(request);
                    novaTurma.setEmpresa(empresa);
                    novaTurma.setCurso(curso);
                    return Mono.just(novaTurma);
                }));
    }

    @Override
    public Mono<Turma> findById(Long id) {
        log.info("Buscando turma por ID: {}", id);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(optionalTurma -> {
                    if (optionalTurma.isPresent()) {
                        log.info("Turma encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalTurma.get());
                    } else {
                        log.warn("Nenhum turma encontrado para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar turma por ID {}: {}", id, e.getMessage(), e));
    }

    private Mono<Turma> findByIdAndLoadCurso(Long id) {
        log.info("Buscando turma por ID e recuperar o curso: {}", id);
        return Mono.fromCallable(() -> repository.findByIdAndLoadCursoAndEmpresa(id).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Erro ao buscar turma por ID {}: {}", id, e.getMessage(), e));
//        log.info("Buscando turma por ID: {}", id);
//        return Mono.fromCallable(() -> repository.findByIdAndLoadCursoAndEmpresa(id))
//                .flatMap(Mono::justOrEmpty) // Converte Optional<Turma> para Mono<Turma> ou Mono.empty()
//                .doOnSuccess(turma -> {
//                    if (turma != null) {
//                        log.info("Turma com ID {} e curso '{}' encontrados com sucesso.", id, turma.getCurso().getNome());
//                    } else {
//                        log.warn("Nenhuma turma encontrada para o ID: {}", id);
//                    }
//                })
//                .doOnError(e -> log.error("Erro ao buscar turma por ID {}: {}", id, e.getMessage(), e))
//                .subscribeOn(Schedulers.boundedElastic()); // É uma boa prática mover chamadas de bloqueio (JDBC) para um scheduler apropriado.
    }

    @Override
    public Optional<Page<Turma>> findByFiltro(String filtro, List<StatusTurma> status, Long idEmpresa, Pageable pageable) {
        Pageable effectivePageable = (pageable != null) ? pageable : Pageable.unpaged();
        List<StatusTurma> statusParaFiltro = (status != null && status.isEmpty()) ? null : status;
        return repository.findByFiltro(filtro, statusParaFiltro, idEmpresa, effectivePageable);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        log.info("Solicitada exclusão de turma por ID: {}", id);
        return Mono.fromRunnable(() -> repository.deleteById(id))
                .doOnSuccess(v -> log.info("Turma com ID {} excluído com sucesso.", id))
                .doOnError(e -> log.error("Erro ao excluir turma por ID {}: {}", id, e.getMessage(), e)).then()
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }

    @Override
    public Mono<Void> delete(Turma entity) {
        log.info("Solicitada exclusão de turma pela entidade. ID: {}", entity != null ? entity.getId() : "null");
        return Mono.fromRunnable(() -> repository.delete(entity))
                .doOnSuccess(v -> log.info("Turma com ID {} excluído com sucesso pela entidade.", entity != null ? entity.getId() : "null"))
                .doOnError(e -> log.error("Erro ao excluir turma pela entidade {}: {}", entity != null ? entity.getId() : "null", e.getMessage(), e)).then();
    }

    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar turma: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (nome)")) {
                errorMessage = "Já existe um turma com este nome. Por favor, escolha outro.";
            } else if (message.contains("key (codigo)")) {
                errorMessage = "Já existe um turma com este código. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else if (message.contains("violates foreign key constraint")) {
            if (message.contains("table \"tb_contrato\"")) {
                errorMessage = "Já existe um matrícula para essa turma. Apague as mastrículas primeiro.";
            } else {
                errorMessage = "Existe um relacionamento de turma com alguma outra entidade. Não é possível excluir o contrato";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o turma.";
        }
        log.error("Erro de integridade de dados processado: {}", errorMessage, e); // Mantendo log.error para a exceção original
        return new BaseException(errorMessage, e);
    }

    private Mono<Void> validateRequest(TurmaRequest request) {
        // 1. Validações iniciais e rápidas
        if (request.idEmpresa() == null) {
            return Mono.error(new BaseException("O ID da empresa é obrigatório."));
        }
        if (request.idCurso() == null) {
            return Mono.error(new BaseException("O ID do curso é obrigatório."));
        }
        return Mono.empty();
    }

    private Mono<EntitiesContext> getRequiredEntities(TurmaRequest request) {
        // 2. Prepara os Monos para buscar as entidades relacionadas (Empresa e Curso)
        // Usamos switchIfEmpty para falhar de forma clara se uma entidade não for encontrada.
        Mono<Empresa> monoEmpresa = empresaService.findById(request.idEmpresa())
                .switchIfEmpty(Mono.error(new BaseException("Empresa não encontrada com o ID: " + request.idEmpresa())));

        Mono<Curso> monoCurso = cursoService.findById(request.idCurso())
                .switchIfEmpty(Mono.error(new BaseException("Curso não encontrado com o ID: " + request.idCurso())));


        return Mono.zip(monoEmpresa, monoCurso) // Zipando apenas 2 Monos agora
                .flatMap(tuple -> {
                    Empresa empresa = tuple.getT1();
                    Curso curso = tuple.getT2();

                    return Mono.just(new EntitiesContext(empresa, curso));
                });
    }

    private Mono<Turma> updateOrCreate(TurmaRequest request, EntitiesContext context) {
        return Mono.justOrEmpty(request.id())
                .flatMap(this::findById)
                .map(existingEntity -> {
                    log.info("Atualizando turma existente com ID: {}", existingEntity.getId());
                    mapper.updateEntity(request, existingEntity);
                    existingEntity.setCurso(context.curso);
                    return existingEntity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova entidade de turma para o curso '{}'", context.curso.getNome());
                    Turma novaTurma = mapper.toEntity(request);
                    novaTurma.setEmpresa(context.empresa);
                    novaTurma.setCurso(context.curso);
                    return gerarCodigoDaTurma(context.curso, novaTurma.getAnoPeriodo(), novaTurma.getDataInicio())
                            .flatMap(codigo -> {
                                novaTurma.setCodigo(codigo);
                                return Mono.just(novaTurma);
                            });
                }));
    }

    private Mono<Turma> persist(Turma entity) {
        return Mono.fromCallable(() -> repository.save(entity)).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> gerarCodigoDaTurma(Curso curso, String anoPeriodo, LocalDate dataInicio) {
        String cursoAbreviado = curso.getNome().substring(0, 3).toUpperCase();
        String periodoAbreviado = anoPeriodo.substring(0, 1).toUpperCase();
        String anoAbreviado = String.valueOf(dataInicio.getYear()).substring(2);

        return Mono.fromCallable(() -> repository.countByCursoAndAnoPeriodoAndAno(curso, anoPeriodo, dataInicio.getYear()))
                .map(proximaSequencia -> {
                    String sequenciaFormatada = String.format("%02d", proximaSequencia + 1);
                    // 5. Monta o código final
                    return String.format("%s-%s-%s-%s", cursoAbreviado, periodoAbreviado, anoAbreviado, sequenciaFormatada);
                });

    }

    public Mono<RelatorioBase64Response> emitirRelatorio(FiltroRelatorioRequest request, Usuario usuario) {
        // 1. Encontra os clientes e retorna um Mono<Page<Cliente>>.
        // Usar Mono.justOrEmpty para lidar com o Optional.
        return Mono.justOrEmpty(findByFiltro(request.filtro(), null, usuario.getEmpresaIdFromToken(), null))
                .flatMap(entitiesPage -> {
                    // 2. Busca a empresa do usuário. A partir daqui, o fluxo é garantido.
                    return empresaService.findById(usuario.getEmpresaIdFromToken())
                            .flatMap(empresa -> {
                                // 3. Define a busca do logo. Se o logo for nulo, retorna um Mono vazio.
                                // Isso evita a chamada ao storageService.
                                Mono<String> logoMono = (empresa.getLogo() != null)
                                        ? storageService.getFileAsBase64(empresa.getLogo().getUuid())
                                        .doOnError(e -> log.error("Arquivo do logo não encontrado para a empresa com ID {}: {}", empresa.getId(), e.getMessage(), e))
                                        .onErrorResume(e -> Mono.just("")) // Resiliência: retorna vazio em caso de erro
                                        : Mono.just(""); // Se não tiver logo, emite uma string vazia imediatamente

                                // 4. Combina os resultados do cliente e do logo para gerar o relatório.
                                return logoMono.flatMap(logoBase64 ->
                                        this.generateReport(request, entitiesPage.getContent(), usuario, empresa.getNomeFantasia(), logoBase64)
                                );
                            })
                            .switchIfEmpty(
                                    // 5. Se a empresa não for encontrada ou o ID for nulo, gera o relatório com um nome genérico e sem logo.
                                    // Isso lida com o caso em que usuario.getEmpresaIdFromToken() é nulo
                                    this.generateReport(request, entitiesPage.getContent(), usuario, "Escolar", null)
                            )
                            .doOnError(e -> log.error("Erro ao buscar a empresa com ID {}: {}", usuario.getEmpresaIdFromToken(), e.getMessage(), e));
                })
                .switchIfEmpty(Mono.empty()); // Retorna Mono.empty() se findByFiltro não encontrar nada
    }

    private Mono<RelatorioBase64Response> generateReport(
            FiltroRelatorioRequest request,
            List<Turma> entities,
            Usuario usuario,
            String empresaNome,
            String logoBase64) {
        // 5. Unifica a lógica de geração do relatório em um método separado.
        return Mono.fromCallable(() -> {
            MetadadosRelatorioRequest metadados = MetadadosRelatorioRequest.builder()
                    .nomeUsuario("%s %s".formatted(usuario.getFirstname(), usuario.getLastname()))
                    .titulo("Sistema de Gestão: " + empresaNome)
                    .subtitulo("Relatório de turmas")
                    .logoBase64(logoBase64.isBlank() ? null : logoBase64) // Garante que a string vazia seja tratada como nula
                    .nomeArquivo("turma")
                    .build();

            // 6. A lógica de geração de relatório continua a mesma, mas agora está em um só lugar.
            ObjectNode jsonNodes = reportService.generateReport(request.tipo(), entities, metadados, Turma.class);

            String nomeArquivo = jsonNodes.get("filename").asText();
            String conteudoBase64 = jsonNodes.get("content").asText();
            return new RelatorioBase64Response(nomeArquivo, conteudoBase64);

        }).onErrorResume(BaseException.class, e -> {
            // Trata exceções específicas
            log.error("Erro no processamento do relatório: {}", e.getMessage(), e);
            return Mono.error(e);
        }).onErrorResume(Exception.class, e -> {
            // Trata outras exceções
            log.error("Erro desconhecido no processamento do relatório: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Erro ao processar o relatório", e));
        });
    }


    private record EntitiesContext(Empresa empresa, Curso curso) {
    }
}