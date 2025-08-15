package com.escola.admin.service.report;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.util.pdf.LocalPdfUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportPdfConfig {
    public static final String TITULO_SISTEMA = "Sistema de Gestão";

    @Bean("reportEmpresaPDF")
    public GenericReportPdf<Empresa> reportEmpresaPdf(LocalPdfUtil pdfUtil) {
        return new GenericReportPdf<>(
                pdfUtil,
                new String[]{"Nome Fantasia", "Razão Social", "CNPJ", "Inscrição Estadual", "E-mail", "Telefone"},
                new Integer[]{25, 30, 15, 10, 10, 10},
                entity -> new String[]{
                        entity.getNomeFantasia(),
                        entity.getRazaoSocial(),
                        entity.getCnpj(),
                        entity.getInscricaoEstadual(),
                        entity.getEmail(),
                        entity.getTelefone()
                }
        );
    }


}
