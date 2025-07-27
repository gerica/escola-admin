package com.escola.admin.service.cliente.impl;

import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.mapper.cliente.ContratoMapper;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.model.response.cliente.ParametroResponse;
import com.escola.admin.repository.cliente.ContratoRepository;
import com.escola.admin.service.cliente.ArtificalInteligenceService;
import com.escola.admin.service.cliente.ContratoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service()
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ContratoServiceImpl implements ContratoService {


    final String QUERY = """
            query findByChave($chave: String!) {
                findByChave(chave: $chave) {
                    id
                    chave
                    modeloContrato
                }
            }
            """;
    ContratoRepository repository;
    ContratoMapper mapper;
    HttpGraphQlClient graphQlClient;
    ArtificalInteligenceService parseLocal;
//    ArtificalInteligenceService chatgpt;
    //    ArtificalInteligenceService gemini;

    @Override
    public Contrato save(ContratoRequest request) {
        Contrato entity;
        Optional<Contrato> optional = Optional.empty();
        if (request.idContrato() != null) {
            optional = repository.findById(request.idContrato());
        }

        if (optional.isPresent()) {
            entity = mapper.updateEntity(request, optional.get());
        } else {
            entity = mapper.toEntity(request);
//            entity.setdata(LocalDateTime.now());
        }
//        entity.setDataAtualizacao(LocalDateTime.now());
        return repository.save(entity);
    }

    @Override
    public Optional<Page<Contrato>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable) {
        return repository.findByFiltro(filtro, idEmpresa, pageable);
    }

    @Override
    public Optional<Contrato> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Void> deleteById(Integer id) {
        return Optional.empty();
    }

    @Override
    public Optional<Contrato> parseContrato(Long idContrato) {
        try {
            ParametroResponse response = graphQlClient.document(QUERY)
                    .variable("chave", CHAVE_CONTRATO_MODELO_PADRAO)
                    .retrieve("findByChave")
                    .toEntity(ParametroResponse.class)
                    .block();
            Optional<Contrato> optional = findById(idContrato);

            if (optional.isPresent() && response != null) {
                converterComIA(optional.get(), response);
            }
            return optional;
        } catch (Exception e) {
            log.error("Erro ao chamar o admin-service: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void converterComIA(Contrato contrato, ParametroResponse response) throws JsonProcessingException {


//        var resultSmartContrato = chatgpt.generateText("Estou passando para você um contrato: " + response.getModeloContrato() +
//                "E aqui está os dados de um cliente: " + jsonOutput + ", preencha os campos referente ao cliente nesse contrato e " +
//                "me retorno o contrato, no mesmo formato que te enviei.");

        var resultSmartContrato = parseLocal.generateText(response.getModeloContrato(), contrato);

        contrato.setContratoDoc(resultSmartContrato);
    }


    private String getCurrentRequestToken() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }

}
