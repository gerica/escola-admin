package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.entity.cliente.StatusContrato;
import com.escola.admin.model.mapper.cliente.ContratoMapper;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.repository.cliente.ContratoRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.ParametroService;
import com.escola.admin.service.auxiliar.CursoService;
import com.escola.admin.service.auxiliar.MatriculaService;
import com.escola.admin.service.cliente.ArtificalInteligenceService;
import com.escola.admin.service.cliente.ContratoService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.escola.admin.service.ParametroService.CHAVE_CONTRATO_MODELO_PADRAO_MAP;

@Service()
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ContratoServiceImpl implements ContratoService {

    ContratoRepository repository;
    ContratoMapper mapper;
    ArtificalInteligenceService parseLocal;
    ParametroService parametroService;
    EmpresaService empresaService;
    MatriculaService matriculaService;
    CursoService cursoService;
//    ArtificalInteligenceService chatgpt;
    //    ArtificalInteligenceService gemini;

    private static Contrato getContrato(Matricula matricula, Cliente clienteAssociado, Empresa empresaAssociada, BigDecimal valorTotalCalculado) {
        Contrato novoContrato = Contrato.builder()
                .matricula(matricula)
                .cliente(clienteAssociado)
                .empresa(empresaAssociada)
                .numeroContrato(matricula.getCodigo())
                .dataInicio(matricula.getTurma().getDataInicio())
                .dataFim(matricula.getTurma().getDataFim())
                .valorTotal(valorTotalCalculado) // Acessando o valor do Mono<Curso>
                .desconto(new BigDecimal(5))
                .statusContrato(StatusContrato.PENDENTE)
                .periodoPagamento("Mensal")
                .descricao("Contrato de serviço educacional para a matrícula " + matricula.getId())
                .termosCondicoes("Termos padrão do contrato. Ver documento.")
                .observacoes("5% de desconto com o pagamento até o 5 dia útil do mês.")
                .build();
        return novoContrato;
    }

    @Override
    @Transactional
    public Mono<Void> save(ContratoRequest request) {
        return getRequiredEntities(request) // Step 2: Fetch all necessary entities concurrently
                .flatMap(context -> findOrCreate(request, context)) // Step 3: Find or create the Matricula entity
                .flatMap(this::persist) // Step 4: Persist the Matricula
                .doOnSuccess(savedEntity -> log.info("Contrato salvo com sucesso. ID: {}", savedEntity.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar contrato: {}", e.getMessage(), e))
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
    public Mono<Contrato> findByIdMatricula(Long id) {
        log.info("Buscando Contato por ID: {}", id);
        return Mono.fromCallable(() -> repository.findByIdMatricula(id))
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
    public Mono<Void> deleteByIdMatricula(Long id) {
        log.info("Apagar Contato por ID matricula: {}", id);
        return Mono.fromRunnable(() -> repository.deleteByMatriculaId(id))
                .then() // Retorna um Mono<Void> para indicar a conclusão
                .doOnSuccess(v -> log.info("Contratos excluídos com sucesso para o ID da matrícula: {}", id))
                .doOnError(e -> log.error("Erro ao excluir contratos para o ID da matrícula {}: {}", id, e.getMessage(), e));
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

    @Override
    public Long count() {
        return repository.count();
    }

    @Override
    public Mono<Matricula> criarContrato(Matricula matricula) {
        if (matricula == null || (matricula.getCliente() == null && matricula.getClienteDependente() == null)) {
            return Mono.error(new IllegalArgumentException("A matrícula não é válida para a criação de um contrato."));
        }

        var clienteAssociado = matricula.getCliente() != null ?
                matricula.getCliente() :
                matricula.getClienteDependente().getCliente();

        var empresaAssociada = matricula.getTurma().getEmpresa();
        if (empresaAssociada == null) {
            return Mono.error(new IllegalArgumentException("A turma da matrícula não possui uma empresa associada."));
        }

        // 1. Inicia o fluxo reativo a partir do ID do curso na matrícula.
        // Como 'cursoService.findById' retorna um Mono, usamos 'flatMap' para
        // esperar a resposta antes de prosseguir.
        Long cursoId = matricula.getTurma().getCurso().getId();

        return cursoService.findById(cursoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Curso não encontrado para a turma da matrícula.")))
                .flatMap(cursoAssociado -> {

                    BigDecimal valorTotalCalculado;
                    Optional<Long> numeroDeMeses = getNumeroMesesDaDuracao(cursoAssociado.getDuracao());

                    // Se a duração for em anos ou meses, calcula o valor total.
                    // Se a duração não for em meses/anos (ex: horas), usa o valor mensal
                    // como valor total do contrato (pode ser ajustado conforme a regra de negócio).
                    valorTotalCalculado = numeroDeMeses.map(aLong -> cursoAssociado.getValorMensalidade()
                            .multiply(BigDecimal.valueOf(aLong))
                            .setScale(2, RoundingMode.HALF_UP)).orElseGet(cursoAssociado::getValorMensalidade);

                    // 2. Dentro deste flatMap, já temos o objeto 'Curso' resolvido.
                    // Agora podemos usar seus dados para construir o Contrato.
                    Contrato novoContrato = getContrato(matricula, clienteAssociado, empresaAssociada, valorTotalCalculado);

                    return Mono.fromCallable(() -> repository.save(novoContrato))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(this::recuperarModeloContrato)
                            .flatMap(contratoProcessado -> Mono.fromCallable(() -> repository.save(contratoProcessado)))
                            .thenReturn(matricula);
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

//    private Mono<Void> validateRequest(ContratoRequest request) {
//        if (request.idMatricula() == null) {
//            return Mono.error(new BaseException("O ID da matricula é obrigatório."));
//        }
//        return Mono.empty(); // Indicate success
//    }

    private Mono<EntitiesContext> getRequiredEntities(ContratoRequest request) {
        if (request.idMatricula() == null) {
            return Mono.just(new EntitiesContext(null, null));
        }

        Mono<Matricula> monoMatricua = matriculaService.findByIdWithClienteAndDependente(request.idMatricula())
                .switchIfEmpty(Mono.error(new BaseException("Matricula não encontrada com o ID: " + request.idMatricula())));
        Mono<Empresa> monoEmpresa = empresaService.findById(request.idEmpresa())
                .switchIfEmpty(Mono.error(new BaseException("Empesa não encontrada com o ID: " + request.idMatricula())));


        return Mono.zip(monoMatricua, monoEmpresa) // Zipando apenas 2 Monos agora
                .flatMap(tuple -> {
                    Matricula matricula = tuple.getT1();
                    Empresa empresa = tuple.getT2();

                    return Mono.just(new EntitiesContext(matricula, empresa));
                });
    }

    private Mono<Contrato> findOrCreate(ContratoRequest request, EntitiesContext context) {
        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)
                        .switchIfEmpty(Mono.error(new BaseException("Contrato com ID " + id + " não encontrada para atualização.")))
                )
                .map(existingEntity -> {
                    log.info("Atualizando contrato existente com ID: {}", existingEntity.getId());
                    mapper.updateEntity(request, existingEntity);
                    return existingEntity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova entidade de contrato para a turma '{}'", context.matricula.getTurma().getNome());
                    Contrato entity = mapper.toEntity(request);
                    entity.setMatricula(context.matricula);

                    if (context.matricula.getCliente() != null) {
                        entity.setCliente(context.matricula.getCliente());
                    } else {
                        entity.setCliente(context.matricula.getClienteDependente().getCliente());
                    }
                    entity.setEmpresa(context.empresa);

                    entity.setNumeroContrato(context.matricula.getCodigo());
                    return Mono.just(entity);
                }));
    }

    private Mono<Contrato> persist(Contrato entity) {
        return Mono.fromCallable(() -> repository.save(entity)).subscribeOn(Schedulers.boundedElastic());
    }

    private Throwable handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar contato: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (numero_contrato)")) {
                errorMessage = "Já existe um contato com este número. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o contato.";
        }
        return new BaseException(errorMessage, e);
    }

    /**
     * Tenta extrair o número de meses a partir de uma string de duração.
     * Retorna um Optional.empty() se a string não corresponder a um padrão de meses ou anos.
     * Ex: "1 ano" -> 12, "6 meses" -> 6, "30 horas" -> Optional.empty()
     */
    private Optional<Long> getNumeroMesesDaDuracao(String duracaoTexto) {
        if (duracaoTexto == null || duracaoTexto.trim().isEmpty()) {
            return Optional.empty();
        }

        // Padrão para anos (ex: "1 ano", "2 anos")
        Pattern anoPattern = Pattern.compile("(\\d+)\\s*ano(s?)", Pattern.CASE_INSENSITIVE);
        Matcher anoMatcher = anoPattern.matcher(duracaoTexto);
        if (anoMatcher.find()) {
            long anos = Long.parseLong(anoMatcher.group(1));
            return Optional.of(anos * 12);
        }

        // Padrão para meses (ex: "6 meses", "12 mes")
        Pattern mesPattern = Pattern.compile("(\\d+)\\s*mes(es?)", Pattern.CASE_INSENSITIVE);
        Matcher mesMatcher = mesPattern.matcher(duracaoTexto);
        if (mesMatcher.find()) {
            long meses = Long.parseLong(mesMatcher.group(1));
            return Optional.of(meses);
        }

        return Optional.empty();
    }

    private Mono<Contrato> recuperarModeloContrato(Contrato contrato) {
        return Mono.defer(() -> {
            Mono<Parametro> parametroMono = parametroService.findByChave(CHAVE_CONTRATO_MODELO_PADRAO);
            return parametroMono
                    .flatMap(parametro -> {
                        converterComIA(contrato, parametro);
                        return Mono.just(contrato);
                    })
                    .doOnError(e -> log.error("Erro ao chamar o serviço de IA: {}", e.getMessage()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private record EntitiesContext(Matricula matricula, Empresa empresa
    ) {
    }

}
