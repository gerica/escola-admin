package com.escola.admin.service.report;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.util.pdf.LocalPdfParameters;
import com.escola.admin.util.pdf.LocalPdfTable;
import com.escola.admin.util.pdf.LocalPdfUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GenericReportPdf<T> implements ReportGenerator<T> {

    LocalPdfUtil pdfUtil;
    String titulo;
    String subtitulo;
    String[] cabecalhos;
    Integer[] colunas;
    Function<T, String[]> entityToRowMapper;

    @Override
    public ObjectNode build(List<T> entities) throws BaseException {
        pdfUtil.iniciarRelatorio(criarParameters());
        pdfUtil.addLineSeparator();

        pdfUtil.addLinhaEmBranco();
        pdfUtil.addParagrafo(subtitulo);
        pdfUtil.addLinhaEmBranco(1.0f);

        addCorpo(entities);
        addRodape();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode arquivo = mapper.createObjectNode();
        arquivo.put("filename", subtitulo + ".pdf");
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

    private LocalPdfParameters criarParameters() {
        Usuario usuarioRequest = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return LocalPdfParameters.builder()
                .metadataTitle(titulo)
                .metadataSubject(subtitulo)
                .metadataAuthor(usuarioRequest.getFirstname())
                .metadataKeywords("Relatório")
                .metadataCreator(usuarioRequest.getFirstname())
                .retrato(false)
                .immediateFlush(false)
                .tituloRelatorio(titulo)
                .subtituloRelatorio(subtitulo)
                .build();
    }

    private void addRodape() {
        Usuario usuarioRequest = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        pdfUtil.addRodape(usuarioRequest.getFirstname());
    }
}