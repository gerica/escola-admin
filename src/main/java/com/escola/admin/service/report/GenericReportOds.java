package com.escola.admin.service.report;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.escola.admin.util.OpenDocumentUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GenericReportOds<T> implements ReportGenerator<T> {

    private final String titulo;
    private final List<String> cabecalhos;
    private final List<String> colunas;
    private final Function<T, JsonNode> entityToJsonMapper;

    @Override
    public ObjectNode build(List<T> entities, MetadadosRelatorioRequest metadados) throws BaseException {
        ObjectMapper mapper = new ObjectMapper();

        // Convert entities to JSON nodes
        List<JsonNode> dados = new ArrayList<>();
        entities.forEach(entity -> dados.add(entityToJsonMapper.apply(entity)));

        // Generate the ODS file
        byte[] planilha = OpenDocumentUtil.gerarPlanilha(titulo, cabecalhos, colunas, dados);

        // Create the response object
        ObjectNode arquivo = mapper.createObjectNode();
        arquivo.put("filename", titulo + ".ods");
        arquivo.put("filetype", ReportService.APPLICATION_ODS);
        arquivo.set("content", new BinaryNode(planilha));
        return arquivo;
    }
}
