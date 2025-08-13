package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.entity.cliente.StatusContrato;
import com.escola.admin.model.mapper.cliente.ContratoMapper;
import com.escola.admin.model.request.cliente.ContratoModeloRequest;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.model.response.cliente.ContratoBase64Response;
import com.escola.admin.repository.cliente.ContratoRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.ItextPdfConverterService;
import com.escola.admin.service.ParametroService;
import com.escola.admin.service.PdfConverterService;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    ItextPdfConverterService itextPdfConverterService;
    PdfConverterService pdfConverterService;
//    ArtificalInteligenceService chatgpt;
    //    ArtificalInteligenceService gemini;

    Contrato criarObjectoContrato(Matricula matricula, Cliente clienteAssociado, Empresa empresaAssociada, Curso cursoAssociado) {

        LocalDate dtInicio = getLocalDate(matricula);

        BigDecimal valorTotalCalculado = getValorTotalCalculado(cursoAssociado, dtInicio, matricula.getTurma().getDataFim());

        return Contrato.builder()
                .matricula(matricula)
                .cliente(clienteAssociado)
                .empresa(empresaAssociada)
                .numeroContrato(matricula.getCodigo())
                .dataInicio(dtInicio)
                .dataFim(matricula.getTurma().getDataFim())
                .valorTotal(valorTotalCalculado) // Acessando o valor do Mono<Curso>
                .desconto(new BigDecimal(5))
                .statusContrato(StatusContrato.PENDENTE)
                .periodoPagamento("Mensal")
                .descricao("Contrato de serviço educacional para a matrícula " + matricula.getId())
                .termosCondicoes("Termos padrão do contrato. Ver documento.")
                .observacoes("5% de desconto com o pagamento até o 5 dia útil do mês.")
                .build();
    }

    // Dentro da classe ContratoServiceImpl.java

    /**
     * Calcula o valor total do contrato de forma precisa, considerando a proporcionalidade
     * dos dias no primeiro e no último mês.
     *
     * @param cursoAssociado O curso que define o valor da mensalidade base.
     * @param dtInicio       A data de início efetiva do contrato.
     * @param dataFim        A data de término do contrato.
     * @return O valor total calculado para o período.
     */
    BigDecimal getValorTotalCalculado(Curso cursoAssociado, LocalDate dtInicio, LocalDate dataFim) {
        BigDecimal valorMensalidade = cursoAssociado.getValorMensalidade();

        // Caso 1: O contrato começa e termina no mesmo mês.
        if (dtInicio.getYear() == dataFim.getYear() && dtInicio.getMonth() == dataFim.getMonth()) {
            long diasNoPeriodo = ChronoUnit.DAYS.between(dtInicio, dataFim) + 1;
            long diasTotalNoMes = dtInicio.lengthOfMonth();

            log.info("Cálculo para contrato no mesmo mês. Dias no período: {}, Dias no mês: {}", diasNoPeriodo, diasTotalNoMes);

            return valorMensalidade
                    .multiply(BigDecimal.valueOf(diasNoPeriodo))
                    .divide(BigDecimal.valueOf(diasTotalNoMes), 2, RoundingMode.HALF_UP);
        }

        // Caso 2: O contrato abrange múltiplos meses.
        BigDecimal valorTotal = BigDecimal.ZERO;

        // --- Parte 1: Cálculo do primeiro mês (proporcional) ---
        long diasNoPrimeiroMes = dtInicio.lengthOfMonth();
        long diasRestantesPrimeiroMes = diasNoPrimeiroMes - dtInicio.getDayOfMonth() + 1;
        BigDecimal valorPrimeiroMes = valorMensalidade
                .multiply(BigDecimal.valueOf(diasRestantesPrimeiroMes))
                .divide(BigDecimal.valueOf(diasNoPrimeiroMes), 2, RoundingMode.HALF_UP);
        valorTotal = valorTotal.add(valorPrimeiroMes);
        log.info("Valor proporcional do primeiro mês ({} dias de {}): R$ {}", diasRestantesPrimeiroMes, diasNoPrimeiroMes, valorPrimeiroMes);


        // --- Parte 2: Cálculo dos meses intermediários (cheios) ---
        LocalDate inicioProximoMes = dtInicio.plusMonths(1).withDayOfMonth(1);
        LocalDate inicioUltimoMes = dataFim.withDayOfMonth(1);

        if (inicioProximoMes.isBefore(inicioUltimoMes)) {
            long mesesCheios = ChronoUnit.MONTHS.between(inicioProximoMes, inicioUltimoMes);
            BigDecimal valorMesesCheios = valorMensalidade.multiply(BigDecimal.valueOf(mesesCheios));
            valorTotal = valorTotal.add(valorMesesCheios);
            log.info("Valor dos meses intermediários ({} meses): R$ {}", mesesCheios, valorMesesCheios);
        }

        // --- Parte 3: Cálculo do último mês (proporcional) ---
        long diasNoUltimoMes = dataFim.lengthOfMonth();
        long diasUtilizadosUltimoMes = dataFim.getDayOfMonth();
        BigDecimal valorUltimoMes = valorMensalidade
                .multiply(BigDecimal.valueOf(diasUtilizadosUltimoMes))
                .divide(BigDecimal.valueOf(diasNoUltimoMes), 2, RoundingMode.HALF_UP);
        valorTotal = valorTotal.add(valorUltimoMes);
        log.info("Valor proporcional do último mês ({} dias de {}): R$ {}", diasUtilizadosUltimoMes, diasNoUltimoMes, valorUltimoMes);

        log.info("Valor total final calculado para o contrato: R$ {}", valorTotal);
        return valorTotal.setScale(2, RoundingMode.HALF_UP);
    }

    LocalDate getLocalDate(Matricula matricula) {
        LocalDate dtInicio;
        LocalDate hoje = LocalDate.now();
        if (hoje.isAfter(matricula.getTurma().getDataInicio())) {
            dtInicio = hoje;
        } else {
            dtInicio = matricula.getTurma().getDataInicio();
        }
        return dtInicio;
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
    @Transactional
    public Mono<Void> saveModelo(ContratoModeloRequest request) {
        return findById(request.id())
                .flatMap(context -> parseModelo(request)) // Step 3: Find or create the Matricula entity
                .flatMap(this::persist) // Step 4: Persist the Matricula
                .doOnSuccess(savedEntity -> log.info("Contrato salvo com sucesso. ID: {}", savedEntity.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar contrato: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException)
                .then();
    }

    @Override
    public Optional<Page<Contrato>> findByFiltro(String filtro, Long idEmpresa, List<StatusContrato> status, Pageable pageable) {
        List<StatusContrato> statusParaFiltro = (status != null && status.isEmpty()) ? null : status;
        return repository.findByFiltro(filtro, idEmpresa, statusParaFiltro, pageable);
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
                Mono<Parametro> parametroMono = parametroService.findByChave(CHAVE_CONTRATO_MODELO_PADRAO)
                        .switchIfEmpty(Mono.error(new BaseException("Não exsite nenhum modelo configurado!")));
                Mono<Contrato> contratoOptionalMono = findById(idContrato);

                return Mono.zip(contratoOptionalMono, parametroMono)
                        .flatMap(tuple -> {
                            Contrato contrato = tuple.getT1();
                            Parametro parametro = tuple.getT2();

                            converterComIA(contrato, parametro);
                            return Mono.just(contrato); // Retorna o contrato modificado

                        })
                        .doOnError(e -> log.error("Erro ao chamar o admin-service: {}", e.getMessage()));

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

                    Contrato novoContrato = criarObjectoContrato(matricula, clienteAssociado, empresaAssociada, cursoAssociado);

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
                    alterarStatusAtivo(existingEntity);
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

    private void alterarStatusAtivo(Contrato contrato) {
        if (contrato.getDataAssinatura() != null) {
            contrato.setStatusContrato(StatusContrato.ATIVO);
        }
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

    private Mono<Contrato> parseModelo(ContratoModeloRequest request) {
        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)
                        .switchIfEmpty(Mono.error(new BaseException("Contrato com ID " + id + " não encontrada para atualização.")))
                )
                .map(existingEntity -> {
                    existingEntity.setContratoDoc(request.contratoDoc());
                    return existingEntity;
                });
    }

    // Dentro da classe ContratoServiceImpl

    @Override
    public Mono<BigDecimal> getValorMensalidadePorContratoId(Long idContrato) {
        return Mono.fromCallable(() ->
                        repository.findValorMensalidadeByContratoId(idContrato)
                                .orElseThrow(() -> new BaseException("Não foi possível encontrar o valor da mensalidade para o contrato ID: " + idContrato +
                                        ". Verifique se o contrato e seu curso associado existem e possuem um valor definido."))
                )
                .subscribeOn(Schedulers.boundedElastic()) // Essencial para chamadas bloqueantes de banco de dados
                .doOnSuccess(valor -> log.info("Valor da mensalidade encontrado para o contrato {}: {}", idContrato, valor))
                .doOnError(e -> log.error("Falha ao obter valor da mensalidade para o contrato {}: {}", idContrato, e.getMessage()));
    }

    @Override
    public Mono<ContratoBase64Response> downloadDocContrato(Long id) {
        return Mono.fromCallable(() -> repository.findById(id).orElseThrow(() -> new BaseException("Anexo não encontrado.")))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entity -> {
                    log.info("Iniciando download do documento do contrato com ID: {}", entity.getId());
                    if (entity.getContratoDoc() == null) {
                        return Mono.error(new BaseException("O contrato não possui documento."));
                    }
                    try {
                        // Tenta a conversão, que pode lançar uma IOException
//                        var content = itextPdfConverterService.convertHtmlToPdfBase64(entity.getContratoDoc());
                        var content = pdfConverterService.convertHtmlToPdfBase64(entity.getContratoDoc());

                        // Se a conversão for bem-sucedida, retorna o Mono com o sucesso
                        return Mono.just(ContratoBase64Response.builder()
                                .conteudoBase64(content)
                                .nomeArquivo("Contrato-%s".formatted(entity.getNumeroContrato()))
                                .build());

                    } catch (IOException e) {
                        log.error("Falha ao converter HTML para PDF para o contrato {}: {}", entity.getNumeroContrato(), e.getMessage());
                        return Mono.error(new BaseException("Erro ao gerar o PDF do contrato.", e));
                    }
                })
                .doOnSuccess(v -> log.info("Download do documento do contrato concluído com sucesso."))
                .doOnError(e -> log.error("Falha ao baixar anexo: {}", e.getMessage(), e));
    }

    private record EntitiesContext(Matricula matricula, Empresa empresa
    ) {
    }

}
