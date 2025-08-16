package com.escola.admin.service.report;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.escola.admin.util.pdf.LocalPdfParameters;
import com.escola.admin.util.pdf.LocalPdfTable;
import com.escola.admin.util.pdf.LocalPdfUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GenericReportPdf<T> implements ReportGenerator<T> {

    LocalPdfUtil pdfUtil;
    String[] cabecalhos;
    Integer[] colunas;
    Function<T, String[]> entityToRowMapper;

    @Override
    public ObjectNode build(List<T> entities, MetadadosRelatorioRequest metadados) throws BaseException {
        pdfUtil.iniciarRelatorio(criarParameters(metadados));
        pdfUtil.addLineSeparator();

        pdfUtil.addLinhaEmBranco();
        pdfUtil.addParagrafo(metadados.subtitulo());
        pdfUtil.addLinhaEmBranco(1.0f);

        addCorpo(entities);
        addRodape(metadados.nomeUsuario());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode arquivo = mapper.createObjectNode();
        arquivo.put("filename", metadados.nomeArquivo() + ".pdf");
        arquivo.put("filetype", ReportService.APPLICATION_PDF);
        arquivo.set("content", new BinaryNode(pdfUtil.encerrarRelatorio()));
        return arquivo;
    }

    private void addCorpo(List<T> entities) {
        LocalPdfTable paramTable = LocalPdfTable.builder()
                .colunas(colunas)
                .cabecalhos(cabecalhos)
                .bold(false)
                .build();

        pdfUtil.iniciarTabela(paramTable);
        try {

            entities.forEach(entity -> {
                String[] rowData = entityToRowMapper.apply(entity);
                for (String data : rowData) {
                    pdfUtil.addCelula(data);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        pdfUtil.encerrarTabela();
    }

    private LocalPdfParameters criarParameters(MetadadosRelatorioRequest metadados) {
        return LocalPdfParameters.builder()
                .metadataTitle(metadados.titulo())
                .metadataSubject(metadados.subtitulo())
                .metadataAuthor(metadados.nomeUsuario())
                .metadataKeywords("Relatório")
                .metadataCreator(metadados.nomeUsuario())
                .retrato(false)
                .immediateFlush(false)
                .tituloRelatorio(metadados.titulo())
                .subtituloRelatorio(metadados.subtitulo())
                .logoBase64(metadados.logoBase64())
                .build();
    }

    private void addRodape(String usuario) {
        pdfUtil.addRodape(usuario);
    }
}