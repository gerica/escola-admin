package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.mapper.EmpresaMapper;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.model.request.FiltroRelatorioRequest;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.repository.EmpresaRepository;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.report.ReportService;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    @Override
    @Transactional
    public Mono<Empresa> save(EmpresaRequest request) {
        Mono<Empresa> empresaMono = Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id)))
                .flatMap(Mono::justOrEmpty).map((existingEmpresa) -> {
                    mapper.updateEntity(request, existingEmpresa);
                    return existingEmpresa;
                })
                .switchIfEmpty(Mono.defer(() -> Mono.just(mapper.toEntity(request))));

        return empresaMono.flatMap(uwp -> Mono.fromCallable(() -> repository.save(uwp)))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
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
                .doOnError(e -> log.error("Erro ao buscar cargo por ID {}: {}", id, e.getMessage(), e));
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
            try {
                ObjectNode jsonNodes = reportService.generateReport(request.tipo(), optional.get().getContent(), Empresa.class);

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
