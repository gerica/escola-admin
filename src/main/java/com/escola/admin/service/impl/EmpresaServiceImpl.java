package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Logo;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.EmpresaMapper;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.escola.admin.model.response.EmpresaResponse;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.repository.EmpresaRepository;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.FileStorageService;
import com.escola.admin.service.report.ReportService;
import com.escola.admin.util.HashUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class EmpresaServiceImpl implements EmpresaService {

    EmpresaRepository repository;
    UsuarioRepository usuarioRepository;
    EmpresaMapper mapper;
    ReportService<Empresa> reportService;
    FileStorageService storageService;

    @Override
    @Transactional
    public Mono<Void> save(EmpresaRequest request) {
        log.info("Salvar empresa: {}", request.nomeFantasia());

        return validateRequest(request)
                .then(updateOrCreate(request))
                .flatMap(empresa -> this.uploadFile(empresa, request))
                .flatMap(this::persist)
                .doOnSuccess(savedEntity -> log.info("Empresa salvo com sucesso. ID: {}", savedEntity.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar empresa: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException)
                .then();

    }

    @Override
    public Mono<Empresa> findById(Long id) {
        log.info("Buscando empresa por ID: {}", id);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(optionalCargo -> {
                    if (optionalCargo.isPresent()) {
                        log.info("Cargo encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalCargo.get());
                    } else {
                        log.warn("Nenhum empresa encontrado para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar empresa por ID {}: {}", id, e.getMessage(), e));
    }

    @Override
    public Mono<EmpresaResponse> findEntityAndLogoById(Long id) {
        return findById(id)
                .flatMap(empresa -> {
                    // Verifica se a empresa tem um logo
                    if (empresa.getLogo() == null) {
                        // Se não tiver, retorna a resposta sem a imagem
                        return Mono.just(mapper.toResponse(empresa));
                    }

                    // Tenta buscar o arquivo Base64
                    return storageService.getFileAsBase64(empresa.getLogo().getUuid())
                            .doOnError(e -> log.error("Arquivo do logo não encontrado para a empresa com ID {} (UUID: {}): {}",
                                    id, empresa.getLogo().getUuid(), e.getMessage()))
                            // RESILIÊNCIA: Em caso de erro, "resgata" o fluxo retornando um Mono vazio.
                            .onErrorResume(e -> Mono.just(""))
                            // Mapeia a resposta com o Base64 obtido (ou a string vazia)
                            .map(logoBase64 -> mapper.toResponseWithLogo(empresa, logoBase64, empresa.getLogo().getMimeType()));
                })
                .doOnError(e -> log.error("Erro ao buscar empresa com ID {}: {}", id, e.getMessage(), e));
    }

    @Override
    public Optional<Page<Empresa>> findByFiltro(String filtro, Pageable pageable) {
        Pageable effectivePageable = (pageable != null) ? pageable : Pageable.unpaged();
        return repository.findByFiltro(filtro, effectivePageable);
    }

    @Override
    public Optional<Void> deleteById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Void> delete(Empresa empresa) {
        return Optional.empty();
    }

    /**
     * Busca a empresa associada a um usuário de forma reativa e segura.
     *
     * @param usuarioId O ID do usuário logado.
     * @return um Mono contendo a Empresa ou um Mono vazio se não houver associação.
     */
    public Mono<Empresa> findEmpresaByUsuarioId(Long usuarioId) {
        // Envolve a chamada bloqueante do repositório em um Mono
        return Mono.fromCallable(() -> usuarioRepository.findEmpresaByUsuarioId(usuarioId))
                // Desempacota o Optional para um Mono<Empresa> ou Mono.empty()
                .flatMap(Mono::justOrEmpty);
    }

    public Mono<RelatorioBase64Response> emitirRelatorio(FiltroRelatorioRequest request) {
        Optional<Page<Empresa>> optional = findByFiltro(request.filtro(), null);
        if (optional.isPresent()) {
            Usuario usuarioRequest = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            try {
                MetadadosRelatorioRequest metadados = MetadadosRelatorioRequest.builder()
                        .nomeUsuario("%s %s".formatted(usuarioRequest.getFirstname(), usuarioRequest.getLastname()))
                        .titulo("Sistema de Gestão Escolar")
                        .subtitulo("Relatório de empresas")
                        .nomeArquivo("empresas.pdf")
                        .build();
                ObjectNode jsonNodes = reportService.generateReport(request.tipo(), optional.get().getContent(), metadados, Empresa.class);

                // Extrai os campos do ObjectNode e cria a resposta
                String nomeArquivo = jsonNodes.get("filename").asText();
                String conteudoBase64 = jsonNodes.get("content").asText();
                RelatorioBase64Response response = new RelatorioBase64Response(nomeArquivo, conteudoBase64);

                return Mono.just(response);
            } catch (BaseException e) {
                return Mono.error(e);
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Erro ao processar o relatório", e));
            }
        }
        // Caso a lista de empresas esteja vazia, retorna um Mono vazio ou um erro
        return Mono.empty();
    }

    private Mono<Void> validateRequest(EmpresaRequest request) {
        if (request.nomeFantasia() == null || request.nomeFantasia().isBlank()) {
            return Mono.error(new BaseException("O nome fantasia é obrigatório."));
        }
        if (request.razaoSocial() == null || request.razaoSocial().isBlank()) {
            return Mono.error(new BaseException("A razão social é obrigatória."));
        }
        if (request.cnpj() == null || request.cnpj().isBlank()) {
            return Mono.error(new BaseException("O CNPJ é obrigatório."));
        }
        if (request.email() == null || request.email().isBlank()) {
            return Mono.error(new BaseException("O e-mail é obrigatório."));
        }
        if (request.endereco() == null || request.endereco().isBlank()) {
            return Mono.error(new BaseException("O endereço é obrigatório."));
        }

        return Mono.empty(); // Retorna um Mono vazio para indicar sucesso
    }

    private Mono<Empresa> updateOrCreate(EmpresaRequest request) {
        return Mono.justOrEmpty(request.id())
                .flatMap(this::findById)
                .map(existingEntity -> {
                    log.info("Atualizando empresa existente com ID: {}", existingEntity.getId());
                    mapper.updateEntity(request, existingEntity);
                    return existingEntity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova empersa '{}'", request.nomeFantasia());
                    Empresa newEntity = mapper.toEntity(request);
                    return Mono.just(newEntity);
                }));
    }

    private Mono<Empresa> uploadFile(Empresa empresa, EmpresaRequest request) {
        String fileHash = HashUtils.calculateSha256Hash(request.logoBase64());

        // Cenário 1: Não há logo na empresa
        if (empresa.getLogo() == null) {
            return saveNewLogoAndUpdate(empresa, request, fileHash);
        }

        // Cenário 2: O hash do novo arquivo é igual ao do logo existente
        if (fileHash.equals(empresa.getLogo().getHash())) {
            log.info("O hash do logo para {} é o mesmo. Reutilizando arquivo.", empresa.getNomeFantasia());
            return Mono.just(empresa);
        }

        // Cenário 3: O hash é diferente, então o logo foi alterado
        // Deleta o arquivo antigo e salva o novo em um único fluxo reativo
        log.info("O hash do logo para {} é diferente. Apagando logo antigo e salvando novo arquivo.", empresa.getNomeFantasia());
        return storageService.deleteFile(empresa.getLogo().getUuid())
                .flatMap(success -> saveNewLogoAndUpdate(empresa, request, fileHash));
    }

    /**
     * Método auxiliar para salvar um novo arquivo de logo e atualizar a entidade Empresa.
     */
    private Mono<Empresa> saveNewLogoAndUpdate(Empresa empresa, EmpresaRequest request, String fileHash) {
        return storageService.saveFile(request.logoBase64())
                .flatMap(uuid -> {
                    log.info("Novo logo salvo com UUID: {}", uuid);
                    Logo logo;
                    if (empresa.getLogo() == null) {
                        logo = Logo.builder().build();
                        logo.setEmpresa(empresa);
                        empresa.setLogo(logo);
                    }
                    empresa.getLogo().setUuid(uuid);
                    empresa.getLogo().setMimeType(request.logoMimeType());
                    empresa.getLogo().setHash(fileHash);
                    return Mono.just(empresa);
                })
                .doOnError(ex -> log.error("Falha ao salvar arquivo para {}: {}", empresa.getNomeFantasia(), ex.getMessage()));
    }

    private Mono<Empresa> persist(Empresa entity) {
        return Mono.fromCallable(() -> repository.save(entity)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Transforma uma DataIntegrityViolationException em uma BaseException mais amigável.
     */
    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar empresa: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (nomeFantasia)")) {
                errorMessage = "Já existe uma empresa com este nome fantasia. Por favor, escolha outro.";
            } else if (message.contains("key (razaoSocial)")) {
                errorMessage = "Já existe uma empresa com este razão social. Por favor, escolha outro.";
            } else if (message.contains("key (cnpj)")) {
                errorMessage = "Já existe uma empresa com este cnpj social. Por favor, escolha outro.";
            } else if (message.contains("key (email)")) {
                errorMessage = "Já existe uma empresa com este email social. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o empresa.";
        }
        return new BaseException(errorMessage, e);
    }
}
