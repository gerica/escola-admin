package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.cliente.Anexo;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.request.cliente.AnexoRequest;
import com.escola.admin.model.response.cliente.AnexoBase64Response;
import com.escola.admin.repository.cliente.AnexoRepository;
import com.escola.admin.service.FileStorageService;
import com.escola.admin.service.cliente.AnexoService;
import com.escola.admin.service.cliente.ContratoService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;

@Service()
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class AnexoServiceImpl implements AnexoService {

    FileStorageService storageService;
    ContratoService contratoService;
    AnexoRepository repository;

    @Override
    public Mono<Anexo> save(AnexoRequest request) {
        log.info("Salvar anexo do contrato: {}", request.idContrato());

        return validateRequest(request) // Step 1: Validate the incoming request
                .then(getRequiredEntities(request)) // Step 2: Fetch all necessary entities concurrently
                .flatMap(contrato -> uploadAndCreateAnexo(request, contrato)) // Step 3: Find or create the Matricula entity
                .flatMap(this::persistAnexo) // Step 4: Persist the Matricula
                .doOnSuccess(savedEntity -> log.info("Anexo salvo com sucesso. ID: {}", savedEntity.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar anexo: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);

    }

    private Mono<Anexo> persistAnexo(Anexo anexo) {
        return Mono.fromCallable(() -> repository.save(anexo)).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Anexo> uploadAndCreateAnexo(AnexoRequest request, Contrato contrato) {
        return storageService.saveFile(request.conteudoBase64())
                .flatMap(uuid -> {
                    log.info("uuid: {}", uuid);
                    Anexo anexo = Anexo.builder()
                            .nomeArquivo(request.nomeArquivo())
                            .uuid(uuid)
                            .contrato(contrato)
                            .build();
                    return Mono.just(anexo);
                })
                .doOnError(ex -> log.error("Falha ao salvar arquivo para {}: {}", request.idContrato(), ex.getMessage()));
    }

    private Mono<Contrato> getRequiredEntities(AnexoRequest request) {
        return contratoService.findById(request.idContrato());
    }

    private Mono<Void> validateRequest(AnexoRequest request) {
        if (request.idContrato() == null) {
            return Mono.error(new BaseException("O ID do contrato é obrigatório."));
        }
        return Mono.empty(); // Indicate success
    }

    /**
     * @inheritDoc
     */
    @Override
    public Mono<Anexo> findById(Long id) {
        return Mono.fromCallable(() -> repository.findById(id).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Erro ao buscar anexo pelo id{}: {}", id, e.getMessage(), e));
    }

    /**
     * @inheritDoc
     */
    @Override
    public Mono<List<Anexo>> findByIdContrato(Long idContrato) {
        // Encontra todos os anexos de um contrato específico.
        // O `fromCallable` e `subscribeOn` garantem que a operação de bloqueio
        // (acesso ao repositório) seja executada em um thread pool separado.
        return Mono.fromCallable(() -> repository.findByIdContrato(idContrato).orElse(Collections.emptyList()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Erro ao buscar anexos pelo id do contratoID {}: {}", idContrato, e.getMessage(), e));
    }

    /**
     * @inheritDoc
     */
    @Override
    public Mono<Void> deleteById(Long id) {
        return Mono.fromCallable(() -> repository.findById(id).orElseThrow(() -> new BaseException("Anexo não encontrado.")))
                .subscribeOn(Schedulers.boundedElastic()) // Executa a busca em um thread pool
                .flatMap(anexo -> {
                    log.info("Deletando anexo com ID: {} e UUID: {}", anexo.getId(), anexo.getUuid());
                    // 1. Deleta o arquivo físico do disco usando o serviço de armazenamento
                    // O `deleteFile` retorna um Mono<Boolean>
                    return storageService.deleteFile(anexo.getUuid())
                            .map(wasDeleted -> {
                                if (Boolean.FALSE.equals(wasDeleted)) {
                                    log.warn("Arquivo com UUID {} não encontrado no disco. Continuar deletando do banco de dados.", anexo.getUuid());
                                } else {
                                    log.info("Arquivo com UUID {} deletado com sucesso.", anexo.getUuid());
                                }
                                return anexo;
                            });
                })
                .flatMap(anexo -> {
                    // 2. Deleta o registro do banco de dados.
                    return Mono.fromCallable(() -> {
                        repository.delete(anexo);
                        return anexo;
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .then() // Retorna um Mono<Void> para indicar a conclusão da operação
                .doOnSuccess(v -> log.info("Anexo e arquivo deletados com sucesso."))
                .doOnError(e -> log.error("Falha ao deletar anexo: {}", e.getMessage(), e));
    }

    /**
     * Busca um anexo pelo ID, faz o download do arquivo no sistema de armazenamento
     * e retorna o conteúdo do arquivo como uma String Base64.
     *
     * @param id O ID do anexo.
     * @return Um Mono com o conteúdo do arquivo em Base64.
     */
    @Override
    public Mono<AnexoBase64Response> downloadAnexo(Long id) {
        return Mono.fromCallable(() -> repository.findById(id).orElseThrow(() -> new BaseException("Anexo não encontrado.")))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(anexo -> {
                    log.info("Iniciando download do anexo com ID: {} e UUID: {}", anexo.getId(), anexo.getUuid());
                    return storageService.getFileAsBase64(anexo.getUuid())
                            .flatMap(content -> {
                                return Mono.just(AnexoBase64Response.builder()
                                        .conteudoBase64(content)
                                        .nomeArquivo(anexo.getNomeArquivo())
                                        .build());
                            });
                })
                .doOnSuccess(v -> log.info("Download do anexo concluído com sucesso."))
                .doOnError(e -> log.error("Falha ao baixar anexo: {}", e.getMessage(), e));
    }

    private Throwable handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar anexo: {}", e.getMessage());

        String errorMessage = "Erro de integridade de dados ao salvar o anexo.";
        return new BaseException(errorMessage, e);
    }
}
