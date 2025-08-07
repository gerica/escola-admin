package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.cliente.ContaReceber;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.mapper.cliente.ContaReceberMapper;
import com.escola.admin.model.request.cliente.ContaReceberRequest;
import com.escola.admin.repository.cliente.ContaReceberRepository;
import com.escola.admin.service.cliente.ContaReceberService;
import com.escola.admin.service.cliente.ContratoService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ContaReceberServiceImpl implements ContaReceberService {

    // Constante para o contexto matemático dos cálculos BigDecimal
    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    // TODO: Definir como o valor mensal será obtido.
    // Para o exemplo, usaremos um valor fixo. Você deve buscar ou calcular este valor
    // com base nas regras de negócio do seu sistema (ex: de um campo no Contrato).
    private static final BigDecimal VALOR_MENSAL_PADRAO = new BigDecimal("500.00");
    ContaReceberRepository repository;
    ContaReceberMapper mapper;
    ContratoService contratoService;

    @Override
    public Mono<ContaReceber> save(ContaReceberRequest request) {
        return validateRequest(request) // Passo 1: Valida a requisição de entrada
                .then(getRequiredEntities(request)) // Passo 2: Busca todas as entidades necessárias concorrentemente
                .flatMap(contrato -> updateOrCreate(request, contrato)) // Passo 3: Encontra ou cria a entidade ContaReceber
                .flatMap(this::persist) // Passo 4: Persiste a ContaReceber
                .doOnSuccess(savedEntity -> log.info("Conta a receber salvo com sucesso. ID: {}", savedEntity.getId()))
                .doOnError(e -> log.error("Falha na operação de salvar conta a receber: {}", e.getMessage(), e))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }

    @Override
    public Mono<ContaReceber> findById(Long id) {
        return Mono.fromCallable(() -> repository.findById(id).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Erro ao buscar conta a receber pelo id{}: {}", id, e.getMessage(), e));
    }

    @Override
    public Mono<List<ContaReceber>> findByFiltro(Long idContrato) {
        return Mono.fromCallable(() -> repository.findByIdContrato(idContrato).orElse(Collections.emptyList()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Erro ao buscar conta a receber pelo id{}: {}", idContrato, e.getMessage(), e));
    }

    @Override
    public Mono<Void> criar(Long idContrato) {
        return contratoService.findById(idContrato)
                .switchIfEmpty(Mono.error(new BaseException("Contrato não encontrado para o ID: " + idContrato)))
                .flatMap(contrato -> {
                    log.info("Iniciando criação de contas a receber para o contrato ID: {}", contrato.getId());
                    LocalDate dataInicioContrato = contrato.getDataInicio();
                    LocalDate dataFimContrato = contrato.getDataFim();
                    LocalDate hoje = LocalDate.now();

                    List<ContaReceber> contasAReceber = new ArrayList<>();
                    LocalDate dataProximaIteracao; // Renomeado para clareza

                    // Lógica para calcular a primeira conta a receber (proporcional ou mensal)
                    if (hoje.isAfter(dataInicioContrato)) {
                        dataProximaIteracao = handleProportionalOrInitialBilling(contrato, contasAReceber, dataFimContrato, hoje, VALOR_MENSAL_PADRAO);
                        if (dataProximaIteracao == null) { // Indica que nenhuma conta foi criada (ex: hoje > dataFim)
                            return Mono.empty();
                        }
                    } else {
                        dataProximaIteracao = dataInicioContrato;
                    }

                    // Loop para criar as contas a receber para os meses restantes
                    generateMonthlyContas(contrato, contasAReceber, dataProximaIteracao, dataFimContrato, VALOR_MENSAL_PADRAO);

                    // Persiste todas as contas a receber geradas
                    return Flux.fromIterable(contasAReceber)
                            .flatMap(this::persist)
                            .then()
                            .doOnSuccess(v -> log.info("Todas as contas a receber para o contrato ID {} foram criadas e salvas com sucesso.", contrato.getId()))
                            .doOnError(e -> log.error("Erro ao persistir contas a receber para o contrato ID {}: {}", contrato.getId(), e.getMessage(), e));
                });
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return Mono.fromRunnable(() -> repository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("Conta a receber com ID {} excluída com sucesso.", id))
                .doOnError(e -> log.error("Erro ao excluir conta a receber com ID {}: {}", id, e.getMessage(), e));
    }

    @Override
    public Mono<Void> delete(ContaReceber entity) {
        return Mono.fromRunnable(() -> repository.delete(entity))
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("Conta a receber com ID {} excluída com sucesso.", entity.getId()))
                .doOnError(e -> log.error("Erro ao excluir conta a receber {}: {}", entity.getId(), e.getMessage(), e));
    }

    /**
     * Valida a requisição de ContaReceber.
     *
     * @param request A requisição de ContaReceber.
     * @return Mono<Void> indicando sucesso ou erro.
     */
    private Mono<Void> validateRequest(ContaReceberRequest request) {
        if (request.idContrato() == null) {
            return Mono.error(new BaseException("O ID do contrato é obrigatório."));
        }
        return Mono.empty();
    }

    /**
     * Busca as entidades necessárias (Contrato) para a requisição.
     *
     * @param request A requisição de ContaReceber.
     * @return Mono<Contrato> com o contrato encontrado.
     */
    private Mono<Contrato> getRequiredEntities(ContaReceberRequest request) {
        return contratoService.findById(request.idContrato());
    }

    /**
     * Atualiza uma ContaReceber existente ou cria uma nova.
     *
     * @param request  A requisição de ContaReceber.
     * @param contrato O contrato associado.
     * @return Mono<ContaReceber> com a entidade atualizada ou criada.
     */
    private Mono<ContaReceber> updateOrCreate(ContaReceberRequest request, Contrato contrato) {
        return Mono.justOrEmpty(request.id())
                .flatMap(this::findById)
                .map(existingEntity -> {
                    log.info("Atualizando contrato existente com ID: {}", existingEntity.getId());
                    mapper.updateEntity(request, existingEntity);
                    return existingEntity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova conta a receber para o contrato '{}'", contrato.getId());
                    ContaReceber entity = mapper.toEntity(request);
                    entity.setContrato(contrato);
                    return Mono.just(entity);
                }));
    }

    /**
     * Persiste uma entidade ContaReceber no repositório.
     *
     * @param entity A entidade ContaReceber a ser persistida.
     * @return Mono<ContaReceber> com a entidade salva.
     */
    private Mono<ContaReceber> persist(ContaReceber entity) {
        return Mono.fromCallable(() -> repository.save(entity)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Trata exceções de violação de integridade de dados.
     *
     * @param e A exceção DataIntegrityViolationException.
     * @return Uma nova BaseException com uma mensagem de erro amigável.
     */
    private Throwable handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar conta a receber: {}", e.getMessage());
        String errorMessage = "Erro de integridade de dados ao salvar o conta a receber.";
        return new BaseException(errorMessage, e);
    }

    /**
     * Lida com a criação da primeira conta a receber, que pode ser proporcional
     * se a data atual for posterior à data de início do contrato.
     *
     * @param contrato        O contrato.
     * @param contasAReceber  A lista de contas a receber a ser preenchida.     *
     * @param dataFimContrato A data de fim do contrato.
     * @param hoje            A data atual.
     * @param valorMensal     O valor mensal padrão.
     * @return A data de início para a próxima iteração de meses completos, ou null se nenhuma conta for gerada.
     */
    private LocalDate handleProportionalOrInitialBilling(
            Contrato contrato,
            List<ContaReceber> contasAReceber,
            LocalDate dataFimContrato,
            LocalDate hoje,
            BigDecimal valorMensal) {

        if (hoje.isBefore(dataFimContrato) || hoje.isEqual(dataFimContrato)) {
            LocalDate dataVencimentoProporcional = hoje.with(TemporalAdjusters.lastDayOfMonth());
            BigDecimal diasNoMes = BigDecimal.valueOf(hoje.lengthOfMonth());
            BigDecimal diasRestantes = BigDecimal.valueOf(dataVencimentoProporcional.getDayOfMonth() - hoje.getDayOfMonth() + 1);

            BigDecimal valorDiario = valorMensal.divide(diasNoMes, MATH_CONTEXT);
            BigDecimal valorProporcional = valorDiario.multiply(diasRestantes, MATH_CONTEXT);

            ContaReceber contaProporcional = new ContaReceber();
            contaProporcional.setContrato(contrato);
            contaProporcional.setDesconto(contrato.getDesconto());
            contaProporcional.setDataVencimento(dataVencimentoProporcional);
            contaProporcional.setValorTotal(valorProporcional.setScale(2, RoundingMode.HALF_UP));
            contasAReceber.add(contaProporcional);
            log.info("Conta proporcional criada: Vencimento {}, Valor {}", dataVencimentoProporcional, valorProporcional);

            return hoje.plusMonths(1).withDayOfMonth(1);
        } else {
            log.warn("Data atual {} já é posterior à data fim do contrato {}. Nenhuma conta a receber será criada.", hoje, dataFimContrato);
            return null; // Indica que não há mais contas a serem criadas
        }
    }

    /**
     * Gera e adiciona as contas a receber mensais à lista.
     *
     * @param contrato        O contrato.
     * @param contasAReceber  A lista de contas a receber a ser preenchida.
     * @param dataIteracao    A data de início para a geração das contas mensais.
     * @param dataFimContrato A data de fim do contrato.
     * @param valorMensal     O valor mensal padrão.
     */
    private void generateMonthlyContas(
            Contrato contrato,
            List<ContaReceber> contasAReceber,
            LocalDate dataIteracao,
            LocalDate dataFimContrato,
            BigDecimal valorMensal) {

        while (dataIteracao.isBefore(dataFimContrato.plusDays(1))) {
            LocalDate dataVencimento = dataIteracao.with(TemporalAdjusters.lastDayOfMonth());

            if (dataVencimento.isAfter(dataFimContrato)) {
                dataVencimento = dataFimContrato;
            }

            if (dataIteracao.isAfter(dataFimContrato)) {
                break;
            }

            ContaReceber conta = new ContaReceber();
            conta.setContrato(contrato);
            conta.setDataVencimento(dataVencimento);
            conta.setDesconto(contrato.getDesconto());
            conta.setValorTotal(valorMensal.setScale(2, RoundingMode.HALF_UP));
            contasAReceber.add(conta);
            log.info("Conta mensal criada: Vencimento {}, Valor {}", dataVencimento, valorMensal);

            dataIteracao = dataIteracao.plusMonths(1).withDayOfMonth(1);
        }
    }


    public Mono<Void> criar2(Long idContrato) {
        return contratoService.findById(idContrato)
                // Se o contrato não for encontrado, emite um erro
                .switchIfEmpty(Mono.error(new BaseException("Contrato não encontrado para o ID: " + idContrato)))
                .flatMap(contrato -> {
                    log.info("Iniciando criação de contas a receber para o contrato ID: {}", contrato.getId());
                    LocalDate dataInicioContrato = contrato.getDataInicio();
                    LocalDate dataFimContrato = contrato.getDataFim();
                    LocalDate hoje = LocalDate.now();

                    List<ContaReceber> contasAReceber = new ArrayList<>();
                    LocalDate dataIteracao;

                    // TODO: Definir como o valor mensal será obtido.
                    // Para o exemplo, usaremos um valor fixo. Você deve buscar ou calcular este valor
                    // com base nas regras de negócio do seu sistema (ex: de um campo no Contrato).
                    // Agora usando BigDecimal para valorMensal
                    BigDecimal valorMensal = new BigDecimal("500.00"); // Exemplo de valor mensal

                    // Lógica para calcular a primeira conta a receber proporcionalmente
                    // se a data atual for posterior à data de início do contrato.
                    if (hoje.isAfter(dataInicioContrato)) {
                        // Verifica se a data atual ainda está dentro do período do contrato (ou no último dia)
                        if (hoje.isBefore(dataFimContrato) || hoje.isEqual(dataFimContrato)) {
                            // Calcula a data de vencimento para o último dia do mês atual
                            LocalDate dataVencimentoProporcional = hoje.with(TemporalAdjusters.lastDayOfMonth());
                            BigDecimal diasNoMes = BigDecimal.valueOf(hoje.lengthOfMonth());
                            // Calcula os dias restantes no mês, incluindo o dia atual
                            BigDecimal diasRestantes = BigDecimal.valueOf(dataVencimentoProporcional.getDayOfMonth() - hoje.getDayOfMonth() + 1);

                            // Usando BigDecimal para o cálculo proporcional
                            // Definindo um MathContext para precisão e RoundingMode para arredondamento
                            MathContext mc = new MathContext(10, RoundingMode.HALF_UP); // 10 casas de precisão, arredondamento para cima

                            BigDecimal valorDiario = valorMensal.divide(diasNoMes, mc);
                            BigDecimal valorProporcional = valorDiario.multiply(diasRestantes, mc);

                            ContaReceber contaProporcional = new ContaReceber();
                            contaProporcional.setContrato(contrato);
                            contaProporcional.setDataVencimento(dataVencimentoProporcional);
                            // Atribui o BigDecimal diretamente, sem converter para double
                            contaProporcional.setValorTotal(valorProporcional.setScale(2, RoundingMode.HALF_UP));
                            contasAReceber.add(contaProporcional);
                            log.info("Conta proporcional criada: Vencimento {}, Valor {}", dataVencimentoProporcional, valorProporcional);

                            // A próxima iteração de mês cheio começa no primeiro dia do próximo mês
                            dataIteracao = hoje.plusMonths(1).withDayOfMonth(1);
                        } else {
                            // Se hoje já passou da data fim do contrato, não há contas a criar a partir de hoje
                            log.warn("Data atual {} já é posterior à data fim do contrato {}. Nenhuma conta a receber será criada.", hoje, dataFimContrato);
                            return Mono.empty(); // Retorna um Mono vazio, indicando que nenhuma conta foi criada
                        }
                    } else {
                        // Se a data atual for igual ou anterior à data de início do contrato,
                        // a iteração começa a partir da data de início do contrato.
                        dataIteracao = dataInicioContrato;
                    }

                    // Loop para criar as contas a receber para os meses restantes até a data final do contrato
                    // A condição `isBefore(dataFimContrato.plusDays(1))` garante que o mês da dataFimContrato seja incluído.
                    while (dataIteracao.isBefore(dataFimContrato.plusDays(1))) {
                        LocalDate dataVencimento = dataIteracao.with(TemporalAdjusters.lastDayOfMonth());

                        // Ajusta a data de vencimento para não ultrapassar a dataFimContrato
                        if (dataVencimento.isAfter(dataFimContrato)) {
                            dataVencimento = dataFimContrato;
                        }

                        // Se a data de iteração já passou da data fim do contrato, para o loop
                        // (Essa condição é uma segurança extra, a condição do while já deveria cobrir)
                        if (dataIteracao.isAfter(dataFimContrato)) {
                            break;
                        }

                        ContaReceber conta = new ContaReceber();
                        conta.setContrato(contrato);
                        conta.setDataVencimento(dataVencimento);
                        // Atribui o BigDecimal diretamente, sem converter para double
                        conta.setValorTotal(valorMensal.setScale(2, RoundingMode.HALF_UP));
                        contasAReceber.add(conta);
                        log.info("Conta mensal criada: Vencimento {}, Valor {}", dataVencimento, valorMensal);

                        // Avança para o primeiro dia do próximo mês
                        dataIteracao = dataIteracao.plusMonths(1).withDayOfMonth(1);
                    }

                    // Persiste todas as contas a receber geradas usando Flux para processamento reativo
                    return Flux.fromIterable(contasAReceber)
                            .flatMap(this::persist) // Para cada ContaReceber, chama o método persist
                            .then() // Converte o Flux de volta para um Mono<Void> após todas as operações
                            .doOnSuccess(v -> log.info("Todas as contas a receber para o contrato ID {} foram criadas e salvas com sucesso.", contrato.getId()))
                            .doOnError(e -> log.error("Erro ao persistir contas a receber para o contrato ID {}: {}", contrato.getId(), e.getMessage(), e));
                });
    }
}
