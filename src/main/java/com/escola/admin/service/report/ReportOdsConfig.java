package com.escola.admin.service.report;

import com.escola.admin.model.entity.Empresa;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

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

}
