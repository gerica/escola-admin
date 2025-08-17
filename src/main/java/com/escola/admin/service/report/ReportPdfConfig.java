package com.escola.admin.service.report;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.util.pdf.LocalPdfUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

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

    @Bean("reportClientePDF")
    public GenericReportPdf<Cliente> reportClientePdf(LocalPdfUtil pdfUtil) {
        return new GenericReportPdf<>(
                pdfUtil,
                new String[]{"Nome", "Data de Nascimento", "CPF", "RG", "Endereço", "E-mail", "Profissão", "Local de Trabalho", "Status"},
                new Integer[]{20, 10, 10, 10, 10, 10, 10, 10, 10},
                entity -> new String[]{
                        entity.getNome(),
                        entity.getDataNascimento().toString(),
                        entity.getDocCPF(),
                        entity.getDocRG(),
                        entity.getEndereco(),
                        entity.getEmail(),
                        entity.getProfissao(),
                        entity.getLocalTrabalho(),
                        entity.getStatusCliente().toString(),
                }
        );
    }

    @Bean("reportUsuarioPDF")
    public GenericReportPdf<Usuario> reportUsuarioPdf(LocalPdfUtil pdfUtil) {
        return new GenericReportPdf<>(
                pdfUtil,
                new String[]{"Nome", "Usuário", "Empresa", "E-mail", "Papeis"},
                new Integer[]{20, 20, 20, 10, 30},
                entity -> new String[]{
                        "%s %s".formatted(entity.getFirstname(), entity.getLastname()),
                        entity.getUsername(),
                        entity.getEmpresa() != null ? entity.getEmpresa().getNomeFantasia() : "",
                        entity.getEmail(),
                        entity.getRoles().stream()
                                .map(Enum::toString)
                                .collect(Collectors.joining(", "))
                }
        );
    }

    @Bean("reportTurmaPDF")
    public GenericReportPdf<Turma> reportTurmaPdf(LocalPdfUtil pdfUtil) {
        return new GenericReportPdf<>(
                pdfUtil,
                new String[]{"Código", "Nome", "Curso", "Período", "Professor(a)", "Status"},
                new Integer[]{20, 20, 20, 10, 20,10},
                entity -> new String[]{
                        entity.getCodigo(),
                        entity.getNome(),
                        entity.getCurso().getNome(),
                        entity.getAnoPeriodo(),
                        entity.getProfessor(),
                        entity.getStatus().toString(),

                }
        );
    }
}
