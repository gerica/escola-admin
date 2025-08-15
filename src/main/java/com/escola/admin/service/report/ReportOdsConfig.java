package com.escola.admin.service.report;

import com.escola.admin.model.entity.Empresa;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class ReportOdsConfig {

    public static final String DESCRICAO_TITULO = "Descrição";
    public static final String DESCRICAO_VALUE = "descricao";
    public static final String OBSERVACAO_TITULO = "Observação";
    public static final String OBSERVACAO_VALUE = "observacao";
    public static final String FUNCIONARIO = "funcionario";
    public static final String CONTRATO_TITULO = "Contrato";
    public static final String CONTRATO_VALUE = "contrato";
    public static final String E_MAIL_VALUE = "e_mail";
    public static final String COMPLEMENTO_VALUE = "complemento";
    public static final String LOGRADOURO_VALUE = "logradouro";
    public static final String BAIRRO_VALUE = "bairro";


    @Bean("reportEmpresaODS")
    public GenericReportOds<Empresa> reportEmpresaOds() {
        return new GenericReportOds<>(
                "Relatório de Empresas",
//                Arrays.asList("Nome", "Razão Social", "Responsável", "E-mail", "CNPJ", "Logradouro", "Complemento", "Bairro", "CEP"),
//                Arrays.asList("nome", "razao_social", "responsavel", E_MAIL_VALUE, "cnpj", LOGRADOURO_VALUE, COMPLEMENTO_VALUE, BAIRRO_VALUE, "cep"),

                Arrays.asList("Razão Social", "CNPJ"),
                Arrays.asList("razao_social", "cnpj"),

                empresa -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode lineNode = mapper.createObjectNode();
                    lineNode.put("razao_social", empresa.getRazaoSocial());
//                    lineNode.put("responsavel", empresa.getPessoaResponsavel());
//                    lineNode.put(E_MAIL_VALUE, empresa.getEndereco().getEmail());
                    lineNode.put("cnpj", empresa.getCnpj());
//                    lineNode.put(LOGRADOURO_VALUE, empresa.getEndereco().getEnderecoLogradouro());
//                    lineNode.put(COMPLEMENTO_VALUE, empresa.getEndereco().getEnderecoComplemento());
//                    lineNode.put(BAIRRO_VALUE, empresa.getEndereco().getEnderecoBairro());
//                    lineNode.put("cep", empresa.getEndereco().getEnderecoCEP());
                    return lineNode;
                }
        );
    }

}
