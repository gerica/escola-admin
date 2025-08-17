package com.escola.admin.service.report;


import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.FileStorageService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RelatorioBaseService {

    EmpresaService empresaService;
    FileStorageService storageService;
    ReportService reportService;

    public RelatorioBaseService(@Lazy EmpresaService empresaService, FileStorageService storageService, ReportService reportService) {
        this.empresaService = empresaService;
        this.storageService = storageService;
        this.reportService = reportService;
    }

    public <T> Mono<RelatorioBase64Response> emitirRelatorioGenerico(
            Mono<Page<T>> entitiesMono,
            FiltroRelatorioRequest request,
            Usuario usuario,
            String subtitulo,
            String nomeArquivo,
            Class<T> entityClass) {

        return entitiesMono
                .flatMap(entitiesPage -> {
                    // 1. Cria um Mono para o ID da empresa, usando justOrEmpty para tratar o nulo
                    return Mono.justOrEmpty(usuario.getEmpresaIdFromToken())
                            .flatMap(empresaId -> empresaService.findById(empresaId)
                                    .flatMap(empresa -> {
                                        // 3. Se a empresa for encontrada, busca o logo e gera o relatório
                                        return buscarLogoEGerarRelatorio(request, entitiesPage.getContent(), usuario, empresa.getNomeFantasia(), subtitulo, nomeArquivo, entityClass, empresa);
                                    })
                                    .switchIfEmpty(
                                            // 4. Se a empresa não for encontrada, gera o relatório com dados genéricos
                                            Mono.defer(() -> gerarRelatorioGenerico(request, entitiesPage.getContent(), usuario, subtitulo, nomeArquivo, entityClass))
                                    )
                            )
                            .switchIfEmpty(
                                    // 2. Se o ID for nulo, gera o relatório com dados genéricos
                                    Mono.defer(() -> gerarRelatorioGenerico(request, entitiesPage.getContent(), usuario, subtitulo, nomeArquivo, entityClass))
                            );
                })
                .switchIfEmpty(Mono.empty());
    }

    // Método para buscar o logo e gerar o relatório (refatorado para clareza)
    private <T> Mono<RelatorioBase64Response> buscarLogoEGerarRelatorio(
            FiltroRelatorioRequest request,
            List<T> entities,
            Usuario usuario,
            String empresaNome,
            String subtitulo,
            String nomeArquivo,
            Class<T> entityClass,
            Empresa empresa) {

        Mono<String> logoMono = (empresa.getLogo() != null)
                ? storageService.getFileAsBase64(empresa.getLogo().getUuid())
                .doOnError(e -> log.error("Arquivo do logo não encontrado para a empresa com ID {}: {}", empresa.getId(), e.getMessage(), e))
                .onErrorResume(e -> Mono.just(""))
                : Mono.just("");

        return logoMono.flatMap(logoBase64 ->
                generateReport(request, entities, usuario, empresaNome, logoBase64, subtitulo, nomeArquivo, entityClass)
        );
    }

    // Método para gerar o relatório com dados genéricos (sem logo e com nome padrão)
    private <T> Mono<RelatorioBase64Response> gerarRelatorioGenerico(
            FiltroRelatorioRequest request,
            List<T> entities,
            Usuario usuario,
            String subtitulo,
            String nomeArquivo,
            Class<T> entityClass) {
        return generateReport(request, entities, usuario, "Sistema de Gestão", null, subtitulo, nomeArquivo, entityClass);
    }

    // Método privado para gerar o relatório
    private <T> Mono<RelatorioBase64Response> generateReport(
            FiltroRelatorioRequest request,
            List<T> entities,
            Usuario usuario,
            String empresaNome,
            String logoBase64,
            String subtitulo,
            String nomeArquivo,
            Class<T> entityClass) {

        return Mono.fromCallable(() -> {
                    MetadadosRelatorioRequest metadados = MetadadosRelatorioRequest.builder()
                            .nomeUsuario("%s %s".formatted(usuario.getFirstname(), usuario.getLastname()))
                            .titulo("Sistema de Gestão: " + empresaNome)
                            .subtitulo(subtitulo)
                            .logoBase64(logoBase64 != null && !logoBase64.isBlank() ? logoBase64 : null)
                            .nomeArquivo(nomeArquivo)
                            .build();

                    ObjectNode jsonNodes = reportService.generateReport(request.tipo(), entities, metadados, entityClass);

                    String nome = jsonNodes.get("filename").asText();
                    String conteudo = jsonNodes.get("content").asText();
                    return new RelatorioBase64Response(nome, conteudo);

                }).onErrorResume(BaseException.class, Mono::error)
                .onErrorResume(Exception.class, e -> Mono.error(new RuntimeException("Erro ao processar o relatório", e)));
    }
}