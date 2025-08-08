package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.cliente.ContaReceber;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.entity.cliente.StatusContaReceber;
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ContaReceberServiceImpl implements ContaReceberService {

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
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

    @Override
    public Mono<Void> criar(Long idContrato) {
        // 1. Buscar o Contrato e o Valor da Mensalidade do Curso em paralelo.
        Mono<Contrato> contratoMono = contratoService.findById(idContrato)
                .switchIfEmpty(Mono.error(new BaseException("Contrato não encontrado para o ID: " + idContrato)));

        Mono<BigDecimal> valorMensalidadeMono = contratoService.getValorMensalidadePorContratoId(idContrato);

        return contratoMono.zipWith(valorMensalidadeMono)
                .flatMap(tuple -> {
                    // 2. Extrair os resultados da Tupla
                    Contrato contrato = tuple.getT1();
                    BigDecimal valorMensalidadeCurso = tuple.getT2();

                    log.info("Iniciando processo de geração de parcelas para o contrato ID: {} com mensalidade base de: {}", contrato.getId(), valorMensalidadeCurso);

                    long duracaoEmMeses = calcularDuracaoEmMeses(contrato);
                    if (duracaoEmMeses <= 0) {
                        log.warn("Duração do contrato é zero ou negativa. Nenhuma conta será gerada.");
                        return Mono.empty();
                    }

                    // 3. Gerar as parcelas usando o valor da mensalidade vindo diretamente do curso.
                    List<ContaReceber> parcelas = gerarParcelas(contrato, valorMensalidadeCurso);

                    if (parcelas.isEmpty()) {
                        log.warn("Nenhuma parcela foi gerada para o contrato ID: {}. Verifique as datas.", contrato.getId());
                        return Mono.empty();
                    }

                    // 4. Ajustar a última parcela para garantir que a soma final bata com o valor TOTAL do contrato.
                    ajustarUltimaParcela(parcelas, contrato.getValorTotal());

                    return persistirParcelas(parcelas, contrato.getId());
                });
    }

    /**
     * Calcula a duração total do contrato em meses.
     */
    private long calcularDuracaoEmMeses(Contrato contrato) {
        // Adiciona 1 dia para incluir o mês final por completo no cálculo.
        return ChronoUnit.MONTHS.between(
                contrato.getDataInicio().withDayOfMonth(1),
                contrato.getDataFim().withDayOfMonth(1)
        ) + 1;
    }

    /**
     * Calcula o valor da mensalidade padrão dividindo o valor total pela duração.
     */
    private BigDecimal calcularValorMensalPadrao(Contrato contrato, long duracaoEmMeses) {
        BigDecimal valorTotal = contrato.getValorTotal();
        BigDecimal duracao = BigDecimal.valueOf(duracaoEmMeses);
        BigDecimal valorMensal = valorTotal.divide(duracao, MATH_CONTEXT);
        log.info("Valor mensal padrão calculado: {}", valorMensal);
        return valorMensal;
    }

    /**
     * Gera a lista de todas as parcelas (Contas a Receber), tratando a proporcionalidade da primeira.
     */
    private List<ContaReceber> gerarParcelas(Contrato contrato, BigDecimal valorMensalPadrao) {
        List<ContaReceber> parcelas = new ArrayList<>();
        LocalDate hoje = LocalDate.now();
        LocalDate dataInicioContrato = contrato.getDataInicio();
        LocalDate dataFimContrato = contrato.getDataFim();

        // A geração começa em 'hoje' ou na data de início do contrato, o que for mais tarde.
        LocalDate dataIteracao = hoje.isAfter(dataInicioContrato) ? hoje : dataInicioContrato;

        while (!dataIteracao.isAfter(dataFimContrato)) {
            BigDecimal valorParcela;
            LocalDate dataVencimento = dataIteracao.with(TemporalAdjusters.lastDayOfMonth());

            // Lógica para cálculo proporcional da primeira parcela
            if (dataIteracao.equals(hoje) && hoje.isAfter(dataInicioContrato) && hoje.getDayOfMonth() > 1) {
                BigDecimal diasNoMes = BigDecimal.valueOf(dataIteracao.lengthOfMonth());
                BigDecimal diasRestantes = BigDecimal.valueOf(dataVencimento.getDayOfMonth() - dataIteracao.getDayOfMonth() + 1);
                valorParcela = valorMensalPadrao.multiply(diasRestantes, MATH_CONTEXT).divide(diasNoMes, MATH_CONTEXT);
                log.info("Gerando primeira parcela proporcional. Venc: {}, Valor: {}", dataVencimento, valorParcela);
            } else {
                valorParcela = valorMensalPadrao;
                log.info("Gerando parcela mensal cheia. Venc: {}, Valor: {}", dataVencimento, valorParcela);
            }

            // Garante que a data de vencimento não ultrapasse o fim do contrato
            if (dataVencimento.isAfter(dataFimContrato)) {
                dataVencimento = dataFimContrato;
            }

            ContaReceber conta = new ContaReceber();
            conta.setStatus(StatusContaReceber.ABERTA);
            conta.setContrato(contrato);
            conta.setDesconto(contrato.getDesconto());
            conta.setDataVencimento(dataVencimento);
            conta.setValorTotal(valorParcela.setScale(2, RoundingMode.HALF_UP));
            parcelas.add(conta);

            // Avança para o primeiro dia do próximo mês
            dataIteracao = dataIteracao.plusMonths(1).withDayOfMonth(1);
        }
        return parcelas;
    }

    /**
     * Ajusta o valor da última parcela para corrigir diferenças de arredondamento,
     * garantindo que a soma das parcelas seja exatamente o valor total do contrato.
     */
    private void ajustarUltimaParcela(List<ContaReceber> parcelas, BigDecimal valorTotalContrato) {
        BigDecimal somaDasParcelas = parcelas.stream()
                .map(ContaReceber::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal diferenca = valorTotalContrato.subtract(somaDasParcelas);

        if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
            log.info("Ajustando última parcela em {} para bater com o valor total do contrato.", diferenca);
            ContaReceber ultimaConta = parcelas.get(parcelas.size() - 1);
            BigDecimal novoValor = ultimaConta.getValorTotal().add(diferenca);
            ultimaConta.setValorTotal(novoValor.setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * Persiste a lista de parcelas no banco de dados de forma reativa.
     */
    private Mono<Void> persistirParcelas(List<ContaReceber> parcelas, Long idContrato) {
        return Flux.fromIterable(parcelas)
                .flatMap(this::persist)
                .then()
                .doOnSuccess(v -> log.info("Todas as {} contas a receber para o contrato ID {} foram criadas e salvas.", parcelas.size(), idContrato))
                .doOnError(e -> log.error("Erro ao persistir contas a receber para o contrato ID {}: {}", idContrato, e.getMessage(), e));
    }

    // --- MÉTODOS PRIVADOS EXISTENTES ---
    private Mono<Void> validateRequest(ContaReceberRequest request) {
        if (request.idContrato() == null) {
            return Mono.error(new BaseException("O ID do contrato é obrigatório."));
        }
        return Mono.empty();
    }

    private Mono<Contrato> getRequiredEntities(ContaReceberRequest request) {
        return contratoService.findById(request.idContrato());
    }

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

    private Mono<ContaReceber> persist(ContaReceber entity) {
        return Mono.fromCallable(() -> repository.save(entity)).subscribeOn(Schedulers.boundedElastic());
    }

    private Throwable handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar conta a receber: {}", e.getMessage());
        String errorMessage = "Erro de integridade de dados ao salvar o conta a receber.";
        return new BaseException(errorMessage, e);
    }
}