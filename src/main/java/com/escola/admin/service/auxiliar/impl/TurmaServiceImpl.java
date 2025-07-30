package com.escola.admin.service.auxiliar.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.mapper.auxiliar.TurmaMapper;
import com.escola.admin.model.request.auxiliar.TurmaRequest;
import com.escola.admin.repository.auxiliar.TurmaRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.auxiliar.CursoService;
import com.escola.admin.service.auxiliar.TurmaService;
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
public class TurmaServiceImpl implements TurmaService {

    TurmaRepository repository;
    TurmaMapper mapper;
    EmpresaService empresaService;
    CursoService cursoService;

    @Override
    public Mono<Turma> save(TurmaRequest request) {
        log.info("Iniciando operação de salvar/atualizar turma. Request: {}", request);

        // 1. Validações iniciais e rápidas
        if (request.idEmpresa() == null) {
            return Mono.error(new BaseException("O ID da empresa é obrigatório."));
        }
        if (request.idCurso() == null) {
            return Mono.error(new BaseException("O ID do curso é obrigatório."));
        }

        // 2. Prepara os Monos para buscar as entidades relacionadas (Empresa e Curso)
        // Usamos switchIfEmpty para falhar de forma clara se uma entidade não for encontrada.
        Mono<Empresa> monoEmpresa = empresaService.findById(request.idEmpresa())
                .switchIfEmpty(Mono.error(new BaseException("Empresa não encontrada com o ID: " + request.idEmpresa())));

        Mono<Curso> monoCurso = cursoService.findById(request.idCurso())
                .switchIfEmpty(Mono.error(new BaseException("Curso não encontrado com o ID: " + request.idCurso())));

        // 3. Combina os resultados das buscas usando Mono.zip
        // O zip só emitirá um valor quando ambos os Monos (empresa e curso) completarem com sucesso.
        return Mono.zip(monoEmpresa, monoCurso)
                .flatMap(tuple -> {
                    Empresa empresa = tuple.getT1();
                    Curso curso = tuple.getT2();
                    log.info("Entidades encontradas: Empresa ID {} e Curso ID {}", empresa.getId(), curso.getId());

                    // 4. Lógica de encontrar ou criar a Turma, agora com empresa e curso disponíveis
                    return findOrCreateTurma(request, empresa, curso);
                })
                .flatMap(turmaToSave -> Mono.fromCallable(() -> repository.save(turmaToSave))
                        .doOnSuccess(savedTurma -> log.info("Turma salva com sucesso. ID: {}", savedTurma.getId()))
                )
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

    @Override
    public Optional<Page<Turma>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable) {
        log.info("Buscando turmas por filtro '{}' com paginação: {}", filtro, pageable);
        try {
            Optional<Page<Turma>> result = repository.findByFiltro(filtro, idEmpresa, pageable);
            if (result.isPresent()) {
                log.info("Encontrados {} turmas paginados para o filtro '{}'.", result.get().getTotalElements(), filtro);
            } else {
                log.warn("Nenhum resultado de paginação para o filtro '{}'.", filtro);
            }
            return result;
        } catch (Exception e) {
            log.error("Erro ao buscar turmas por filtro '{}' e paginação {}: {}", filtro, pageable, e.getMessage(), e);
            throw e; // Re-lança a exceção após logar
        }
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        log.info("Solicitada exclusão de turma por ID: {}", id);
        return Mono.fromRunnable(() -> repository.deleteById(id))
                .doOnSuccess(v -> log.info("Turma com ID {} excluído com sucesso.", id))
                .doOnError(e -> log.error("Erro ao excluir turma por ID {}: {}", id, e.getMessage(), e)).then();
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
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o turma.";
        }
        log.error("Erro de integridade de dados processado: {}", errorMessage, e); // Mantendo log.error para a exceção original
        return new BaseException(errorMessage, e);
    }
}