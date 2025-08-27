package com.escola.admin.service.report;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.util.DataUtils;
import com.escola.admin.util.MoedaUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class ReportOdsConfig {

    @Bean("reportEmpresaODS")
    public GenericReportOds<Empresa> reportEmpresaOds() {
        return new GenericReportOds<>(
                "Relatório de Empresas",
                Arrays.asList("Nome Fantasia", "Razão Social", "CNPJ", "Inscrição Estadual", "E-mail", "Telefone"),
                Arrays.asList("nome_fantasia", "razao_social", "cnpj", "inscricao_estadual", "email", "telefone"),

                empresa -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode lineNode = mapper.createObjectNode();
                    lineNode.put("nome_fantasia", empresa.getNomeFantasia());
                    lineNode.put("razao_social", empresa.getRazaoSocial());
                    lineNode.put("cnpj", empresa.getCnpj());
                    lineNode.put("inscricao_estadual", empresa.getInscricaoEstadual());
                    lineNode.put("email", empresa.getEmail());
                    lineNode.put("telefone", empresa.getTelefone());
                    return lineNode;
                }
        );
    }

    @Bean("reportClienteODS")
    public GenericReportOds<Cliente> reportClienteOds() {
        return new GenericReportOds<>(
                "Relatório de Empresas",
                Arrays.asList("Nome", "Data de Nascimento", "CPF", "RG", "Endereço", "E-mail", "Profissão", "Local de Trabalho", "Status"),
                Arrays.asList("nome", "data_nacimento", "cpf", "rg", "endereco", "email", "profissao", "local_trabalho", "status"),

                entity -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode lineNode = mapper.createObjectNode();
                    lineNode.put("nome", entity.getNome());
                    lineNode.put("data_nacimento", entity.getDataNascimento().toString());
                    lineNode.put("cpf", entity.getDocCPF());
                    lineNode.put("rg", entity.getDocRG());
                    lineNode.put("endereco", entity.getEndereco());
                    lineNode.put("email", entity.getEmail());
                    lineNode.put("profissao", entity.getProfissao());
                    lineNode.put("local_trabalho", entity.getLocalTrabalho());
                    lineNode.put("status", entity.getStatusCliente().toString());
                    return lineNode;
                }
        );
    }

    @Bean("reportUsuarioODS")
    public GenericReportOds<Usuario> reportUsuarioOds() {
        return new GenericReportOds<>(
                "Relatório de Usuários",
                Arrays.asList("Nome", "Usuároi", "Empresa", "E-mail", "Papeis"),
                Arrays.asList("nome", "usuario", "empresa", "email", "papeis"),

                entity -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode lineNode = mapper.createObjectNode();
                    lineNode.put("nome", "%s %s".formatted(entity.getFirstname(), entity.getLastname()));
                    lineNode.put("usuario", entity.getUsername());
                    lineNode.put("empresa", entity.getEmpresa() != null ? entity.getEmpresa().getNomeFantasia() : "");
                    lineNode.put("email", entity.getEmail());
                    lineNode.put("papeis", entity.getRoles().stream()
                            .map(Enum::toString)
                            .collect(Collectors.joining(", ")));

                    return lineNode;
                }
        );
    }

    @Bean("reportTurmaODS")
    public GenericReportOds<Turma> reportTurmaOds() {
        return new GenericReportOds<>(
                "Relatório de Usuários",
                Arrays.asList("Código", "Nome", "Curso", "Período", "Professor(a)", "Status"),
                Arrays.asList("codigo", "nome", "curso", "periodo", "professor", "status"),

                entity -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode lineNode = mapper.createObjectNode();
                    lineNode.put("codigo", entity.getCodigo());
                    lineNode.put("nome", entity.getNome());
                    lineNode.put("curso", entity.getCurso().getNome());
                    lineNode.put("periodo", entity.getAnoPeriodo());
                    lineNode.put("professor", entity.getProfessor());
                    lineNode.put("status", entity.getStatus().toString());
                    return lineNode;
                }
        );
    }

    @Bean("reportContratoODS")
    public GenericReportOds<Contrato> reportContratoOds() {
        return new GenericReportOds<>(
                "Relatório de Contratos",
                Arrays.asList("Número do Contrato",
                        "Cliente",
                        "Data início",
                        "Data fim",
                        "Valor",
                        "Status",
                        "Período Pagamento",
                        "Descrição"),
                Arrays.asList("numero_contrato",
                        "cliente",
                        "data_inicio",
                        "data_fim",
                        "valor",
                        "status",
                        "periodo_pagamento",
                        "descricao"),

                entity -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode lineNode = mapper.createObjectNode();
                    lineNode.put("numero_contrato", entity.getNumeroContrato());
                    lineNode.put("cliente", entity.getCliente().getNome());
                    lineNode.put("data_inicio", DataUtils.formatar(entity.getDataInicio()));
                    lineNode.put("data_fim", DataUtils.formatar(entity.getDataFim()));
                    lineNode.put("valor", MoedaUtils.formatarParaReal(entity.getValorTotal()));
                    lineNode.put("status", entity.getStatusContrato().toString());
                    lineNode.put("periodo_pagamento", entity.getPeriodoPagamento());
                    lineNode.put("descricao", entity.getObservacoes());
                    return lineNode;
                }
        );
    }

}
