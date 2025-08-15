package com.escola.admin.service.report;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ReportService<T> {

    public static final String APPLICATION_PDF = "application/pdf";
    public static final String APPLICATION_ODS = "application/vnd.oasis.opendocument.spreadsheet";

    ReportFactoryService<T> reportFactoryService;

    public ObjectNode generateReport(TipoArquivoEnum reportType, List<T> entities, MetadadosRelatorioRequest metadados, Class<T> entityClass) throws BaseException {
        // Get the appropriate report generator
        ReportGenerator<T> reportGenerator = reportFactoryService.getReportGenerator(reportType, entityClass);

        // Generate the report
        return reportGenerator.build(entities, metadados);
    }
}