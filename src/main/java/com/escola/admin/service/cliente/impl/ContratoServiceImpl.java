package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.mapper.cliente.ContratoMapper;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.repository.cliente.ContratoRepository;
import com.escola.admin.service.ParametroService;
import com.escola.admin.service.auxiliar.TurmaService;
import com.escola.admin.service.cliente.ArtificalInteligenceService;
import com.escola.admin.service.cliente.ClienteService;
import com.escola.admin.service.cliente.ContratoService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

import static com.escola.admin.service.ParametroService.CHAVE_CONTRATO_MODELO_PADRAO_MAP;

@Service()
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ContratoServiceImpl implements ContratoService {

    ContratoRepository repository;
    TurmaService turmaService;
    ClienteService clienteService;
    ContratoMapper mapper;
    HttpGraphQlClient graphQlClient;
    ArtificalInteligenceService parseLocal;
    ParametroService parametroService;
//    ArtificalInteligenceService chatgpt;
    //    ArtificalInteligenceService gemini;

    @Override
    public Mono<Void> save(ContratoRequest request) {
//        Contrato entity;
//        Optional<Contrato> optional = Optional.empty();
//        if (request.idContrato() != null) {
//            optional = repository.findById(request.idContrato());
//        }
//
//        if (optional.isPresent()) {
//            entity = mapper.updateEntity(request, optional.get());
//        } else {
//            entity = mapper.toEntity(request);
//        }
//
//        return repository.save(entity);
        return validateRequest(request) // Step 1: Validate the incoming request
                .then(getRequiredEntities(request)) // Step 2: Fetch all necessary entities concurrently
                .flatMap(context -> findOrCreate(request, context.turma, context.cliente)) // Step 3: Find or create the Matricula entity
                .flatMap(this::persistMatricula) // Step 4: Persist the Matricula
                .doOnSuccess(savedMatricula -> log.info("Matrícula salva com sucesso. ID: {}", savedMatricula.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar matrícula: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException)
                .then();
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

    private Mono<Void> validateRequest(ContratoRequest request) {
        if (request.idMatricula() == null) {
            return Mono.error(new BaseException("O ID da matricula é obrigatório."));
        }
        return Mono.empty(); // Indicate success
    }

    private Mono<EntitiesContext> getRequiredEntities(ContratoRequest request) {
        Mono<Turma> monoTurma = turmaService.findById(request.idTurma())
                .switchIfEmpty(Mono.error(new BaseException("Turma não encontrada com o ID: " + request.idTurma())));

        Mono<Cliente> monoCliente = clienteService.findById(request.idCliente())
                .switchIfEmpty(Mono.error(new BaseException("Cliente não encontrado com o ID: " + request.idCliente())));

//        Mono.zip() combina os resultados de múltiplos Monos em uma única tupla,
//        permitindo que operações assíncronas independentes sejam executadas e
//        seus resultados combinados de forma reativa e paralela.
        return Mono.zip(monoTurma, monoCliente)
                .flatMap(tuple -> {
                    Turma turma = tuple.getT1();
                    Cliente cliente = tuple.getT2();
                    return Mono.just(new EntitiesContext(turma, cliente));
                });
    }

    private Mono<Matricula> findOrCreate(ContratoRequest request, Turma turma, Cliente cliente) {
        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)
                        .switchIfEmpty(Mono.error(new BaseException("Contrato com ID " + id + " não encontrada para atualização.")))
                )
                .map(existingEntity -> {
                    log.info("Atualizando contrato existente com ID: {}", existingEntity.getIdContrato());
                    mapper.updateEntity(request, existingEntity);
                    return existingEntity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova entidade de contrato para a turma '{}'", turma.getNome());
                    Contrato novaMatricula = mapper.toEntity(request);
                    novaMatricula.setTurma(turma);
                    novaMatricula.setCliente(cliente);

                    return Mono.fromCallable(() -> {
                                // 1. Busca a última matrícula para esta turma (bloqueante)
                                Optional<Matricula> lastMatriculaOpt = repository.findTopByTurmaIdOrderByCodigoDesc(turma.getId());
                                int nextSequenceNum = 1; // Começa em 1 se não houver matrículas anteriores

                                if (lastMatriculaOpt.isPresent()) {
                                    String lastCodigo = lastMatriculaOpt.get().getCodigo();
                                    // 2. Extrai o número do último código
                                    try {
                                        // Pega a parte após o último hífen, que deve ser o número
                                        String numPart = lastCodigo.substring(lastCodigo.lastIndexOf('-') + 1);
                                        nextSequenceNum = Integer.parseInt(numPart) + 1;
                                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                        log.warn("Formato de código de matrícula inesperado para incremento: {}. Reiniciando a sequência para 1.", lastCodigo);
                                        // Em caso de erro de formato, reinicia a sequência para garantir
                                        nextSequenceNum = 1;
                                    }
                                }

                                // 3. Formata o novo código (ex: MUS-B-24-001)
                                String newCodigo = String.format("%s-%03d", turma.getCodigo(), nextSequenceNum);
                                novaMatricula.setCodigo(newCodigo);
                                return novaMatricula;
                            })
                            .subscribeOn(Schedulers.boundedElastic());// Executa a operação de busca no banco em um pool de threads separado
                }));
    }

    private record EntitiesContext(Turma turma, Cliente cliente) {
    }


}
