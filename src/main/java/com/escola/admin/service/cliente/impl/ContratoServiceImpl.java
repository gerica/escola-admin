package com.escola.admin.service.cliente.impl;

import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.mapper.cliente.ContratoMapper;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.repository.cliente.ContratoRepository;
import com.escola.admin.service.ParametroService;
import com.escola.admin.service.cliente.ArtificalInteligenceService;
import com.escola.admin.service.cliente.ContratoService;
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
import reactor.core.publisher.Mono;

import java.util.Optional;

import static com.escola.admin.service.ParametroService.CHAVE_CONTRATO_MODELO_PADRAO_MAP;

@Service()
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ContratoServiceImpl implements ContratoService {

    ContratoRepository repository;
    ContratoMapper mapper;
    HttpGraphQlClient graphQlClient;
    ArtificalInteligenceService parseLocal;
    ParametroService parametroService;
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
    public Mono<Contrato> findById(Long id) {
        log.info("Buscando Contato por ID: {}", id);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(entity -> {
                    if (entity.isPresent()) {
                        log.info("Turma encontrado com sucesso para ID: {}", id);
                        return Mono.just(entity.get());
                    } else {
                        log.warn("Nenhum turma encontrado para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar contrato por ID {}: {}", id, e.getMessage(), e));

    }

    @Override
    public Optional<Void> deleteById(Integer id) {
        return Optional.empty();
    }

//    @Override
//    public Mono<Contrato> parseContrato(Long idContrato) {
//        try {
//
//            Optional<Parametro> response = parametroService.findByChave(CHAVE_CONTRATO_MODELO_PADRAO);
//            Optional<Contrato> optional = findById(idContrato);
//
//            if (optional.isPresent() && response != null) {
//                converterComIA(optional.get(), response);
//            }
//            return optional;
//        } catch (Exception e) {
//            log.error("Erro ao chamar o admin-service: {}", e.getMessage());
//            return Optional.empty();
//        }
//    }


    @Override
    public Mono<Contrato> parseContrato(Long idContrato) {
        return Mono.defer(() -> { // Mono.defer para garantir que a lógica seja executada apenas na subscrição
            try {
                Mono<Parametro> parametroMono = parametroService.findByChave(CHAVE_CONTRATO_MODELO_PADRAO);
                Mono<Contrato> contratoOptionalMono = findById(idContrato);

                return Mono.zip(contratoOptionalMono, parametroMono)
                        .flatMap(tuple -> {
                            Contrato contrato = tuple.getT1();
                            Parametro parametro = tuple.getT2();

                            converterComIA(contrato, parametro);
                            return Mono.just(contrato); // Retorna o contrato modificado

                        })
                        .doOnError(e -> log.error("Erro ao chamar o admin-service: {}", e.getMessage())) // Captura erros
                        .onErrorResume(e -> Mono.empty()); // Em caso de erro, retorna um Mono vazio
            } catch (Exception e) {
                log.error("Erro ao chamar o admin-service (inicialização): {}", e.getMessage());
                return Mono.error(e); // Retorna um Mono com erro para falhas na fase de defer
            }
        });
    }

    private void converterComIA(Contrato contrato, Parametro parametro) {


//        var resultSmartContrato = chatgpt.generateText("Estou passando para você um contrato: " + response.getModeloContrato() +
//                "E aqui está os dados de um cliente: " + jsonOutput + ", preencha os campos referente ao cliente nesse contrato e " +
//                "me retorno o contrato, no mesmo formato que te enviei.");

        String modelo = (String) parametro.getJsonData().get(CHAVE_CONTRATO_MODELO_PADRAO_MAP);
        var resultSmartContrato = parseLocal.generateText(modelo, contrato);

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
