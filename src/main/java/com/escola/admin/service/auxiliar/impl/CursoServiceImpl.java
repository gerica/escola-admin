package com.escola.admin.service.auxiliar.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.mapper.auxiliar.CursoMapper;
import com.escola.admin.model.request.auxiliar.CursoRequest;
import com.escola.admin.repository.auxiliar.CursoRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.auxiliar.CursoService;
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
public class CursoServiceImpl implements CursoService {

    CursoRepository repository;
    CursoMapper mapper;
    EmpresaService empresaService;

    @Override
    public Mono<Curso> save(CursoRequest request) {
        log.info("Iniciando operação de salvar/atualizar curso. Request ID: {}, Empresa ID: {}", request.id(), request.idEmpresa());

        // 1. Validar idEmpresa
        if (request.idEmpresa() == null) {
            log.error("Tentativa de salvar curso sem idEmpresa fornecido.");
            return Mono.error(new BaseException("O ID da empresa é obrigatório para salvar um curso."));
        }

        // 2. Buscar a empresa pelo ID
        // Como empresaService.findById retorna Mono<Empresa>, precisamos usar flatMap para encadear.
        return empresaService.findById(request.idEmpresa())
                .flatMap(empresa -> {
                    if (empresa == null) {
                        log.warn("Empresa com ID {} não encontrada para vincular ao curso.", request.idEmpresa());
                        return Mono.error(new BaseException("Empresa não encontrada com o ID fornecido: " + request.idEmpresa()));
                    }
                    log.info("Empresa com ID {} encontrada.", empresa.getId());

                    // 3. Lógica existente para encontrar ou criar o Curso
                    return Mono.justOrEmpty(request.id())
                            .flatMap(id -> Mono.fromCallable(() -> repository.findById(id)))
                            .flatMap(Mono::justOrEmpty)
                            .map(existingCurso -> {
                                log.info("Atualizando curso existente com ID: {}", existingCurso.getId());
                                mapper.updateEntity(request, existingCurso);
                                return existingCurso;
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("Criando nova entidade de curso.");
                                Curso newCurso = mapper.toEntity(request);
                                newCurso.setEmpresa(empresa); // **Associa a empresa ao novo curso**
                                return Mono.just(newCurso);
                            }))
                            // 4. Salvar o Curso
                            .flatMap(cursoToSave -> Mono.fromCallable(() -> repository.save(cursoToSave))
                                    .doOnSuccess(savedCurso -> log.info("Curso salvo com sucesso. ID: {}", savedCurso.getId()))
                                    .doOnError(e -> log.error("Erro ao salvar curso: {}", e.getMessage(), e))
                            )
                            .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                            .onErrorMap(e -> !(e instanceof BaseException), e -> {
                                log.error("Ocorreu um erro genérico não esperado ao salvar o curso: {}", e.getMessage(), e);
                                return BaseException.handleGenericException(e);
                            });
                })
                .doOnError(e -> { // Loga erros que podem vir do flatMap da busca da empresa
                    if (!(e instanceof BaseException)) {
                        log.error("Erro inesperado antes de salvar o curso após buscar a empresa: {}", e.getMessage(), e);
                    }
                });
    }

    @Override
    public Mono<Curso> findById(Long id) {
        log.info("Buscando curso por ID: {}", id);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(optionalCurso -> {
                    if (optionalCurso.isPresent()) {
                        log.info("Curso encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalCurso.get());
                    } else {
                        log.warn("Nenhum curso encontrado para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar curso por ID {}: {}", id, e.getMessage(), e));
    }

    @Override
    public Optional<Page<Curso>> findByFiltro(String filtro, Pageable pageable) {
        log.info("Buscando cursos por filtro '{}' com paginação: {}", filtro, pageable);
        try {
            Optional<Page<Curso>> result = repository.findByFiltro(filtro, pageable);
            if (result.isPresent()) {
                log.info("Encontrados {} cursos paginados para o filtro '{}'.", result.get().getTotalElements(), filtro);
            } else {
                log.warn("Nenhum resultado de paginação para o filtro '{}'.", filtro);
            }
            return result;
        } catch (Exception e) {
            log.error("Erro ao buscar cursos por filtro '{}' e paginação {}: {}", filtro, pageable, e.getMessage(), e);
            throw e; // Re-lança a exceção após logar
        }
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        log.info("Solicitada exclusão de curso por ID: {}", id);
        return Mono.fromRunnable(() -> repository.deleteById(id))
                .doOnSuccess(v -> log.info("Curso com ID {} excluído com sucesso.", id))
                .doOnError(e -> log.error("Erro ao excluir curso por ID {}: {}", id, e.getMessage(), e)).then();
    }

    @Override
    public Mono<Void> delete(Curso entity) {
        log.info("Solicitada exclusão de curso pela entidade. ID: {}", entity != null ? entity.getId() : "null");
        return Mono.fromRunnable(() -> repository.delete(entity))
                .doOnSuccess(v -> log.info("Curso com ID {} excluído com sucesso pela entidade.", entity != null ? entity.getId() : "null"))
                .doOnError(e -> log.error("Erro ao excluir curso pela entidade {}: {}", entity != null ? entity.getId() : "null", e.getMessage(), e)).then();
    }

    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar curso: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (nome)")) {
                errorMessage = "Já existe um curso com este nome. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o curso.";
        }
        log.error("Erro de integridade de dados processado: {}", errorMessage, e); // Mantendo log.error para a exceção original
        return new BaseException(errorMessage, e);
    }
}