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

    private static BigDecimal getValorASerPago(ContaReceber existingEntity) {
        BigDecimal fatorDesconto = existingEntity.getDesconto().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal valorDoDesconto = existingEntity.getValorTotal().multiply(fatorDesconto);

        // 2. Calcula o valor final a ser pago, com o desconto aplicado e arredondado para 2 casas decimais (padrão para moeda).
        return existingEntity.getValorTotal().subtract(valorDoDesconto)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Mono<ContaReceber> save(ContaReceberRequest request) {
        return validateRequest(request)
                .then(getRequiredEntities(request))
                .flatMap(contrato -> updateOrCreate(request, contrato))
                .flatMap(this::persist)
                .doOnSuccess(savedEntity -> log.info("Conta a receber salva com sucesso. ID: {}", savedEntity.getId()))
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
        Mono<Contrato> contratoMono = contratoService.findById(idContrato)
                .switchIfEmpty(Mono.error(new BaseException("Contrato não encontrado para o ID: " + idContrato)));

        Mono<BigDecimal> valorMensalidadeMono = contratoService.getValorMensalidadePorContratoId(idContrato);

        return contratoMono.zipWith(valorMensalidadeMono)
                .flatMap(tuple -> {
                    Contrato contrato = tuple.getT1();
                    BigDecimal valorMensalidadeCurso = tuple.getT2();

                    log.info("Iniciando processo de geração de parcelas para o contrato ID: {} com mensalidade base de: {}", contrato.getId(), valorMensalidadeCurso);

                    List<ContaReceber> parcelas = gerarParcelas(contrato, valorMensalidadeCurso);

                    if (parcelas.isEmpty()) {
                        log.warn("Nenhuma parcela foi gerada para o contrato ID: {}. Verifique as datas.", contrato.getId());
                        return Mono.empty();
                    }

                    return persistirParcelas(parcelas, contrato.getId());
                });
    }

    /**
     * Gera a lista de todas as parcelas.
     * A primeira parcela inclui o valor proporcional do primeiro mês e do último.
     * As parcelas intermediárias são de valor cheio.
     */
    private List<ContaReceber> gerarParcelas(Contrato contrato, BigDecimal valorMensalidadeCurso) {
        List<ContaReceber> parcelas = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        // Validação para evitar divisão por zero, que lançaria uma ArithmeticException.
        if (valorMensalidadeCurso == null || valorMensalidadeCurso.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Valor da mensalidade do curso é inválido (nulo, zero ou negativo). Não é possível gerar parcelas para o contrato ID: {}", contrato.getId());
            return Collections.emptyList();
        }

        // divideAndRemainder é a forma ideal para obter o número de parcelas cheias e o valor restante.
        // resultado[0] = quociente (número de parcelas com valor cheio)
        // resultado[1] = resto (valor da última parcela parcial)
        BigDecimal[] resultadoDivisao = contrato.getValorTotal().divideAndRemainder(valorMensalidadeCurso);
        int numeroParcelasCheias = resultadoDivisao[0].intValue();
        BigDecimal valorParcelaPrimeira = resultadoDivisao[1];

        // Define a data de vencimento da primeira parcela (ex: 3 dias a partir de hoje).
        LocalDate dataVencimento = hoje.plusDays(3);

        // 2. Gera a parcela inicial com o valor fracionado, se houver.
        // O seu `if (parteFracionaria > 0)` é equivalente a esta verificação.
        if (valorParcelaPrimeira.compareTo(BigDecimal.ZERO) > 0) {
            parcelas.add(criarContaReceber(contrato, valorParcelaPrimeira, dataVencimento));
            log.info("Gerando parcela inicial com valor francionado de {}. Vencimento: {}", valorParcelaPrimeira, dataVencimento);
        }

        // 1. Gera as parcelas com valor cheio
        log.info("Gerando {} parcelas de valor cheio para o contrato ID: {}", numeroParcelasCheias, contrato.getId());
        for (int i = 0; i < numeroParcelasCheias; i++) {
            // Define o vencimento da próxima parcela para o final do mês seguinte.
            dataVencimento = dataVencimento.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

            parcelas.add(criarContaReceber(contrato, valorMensalidadeCurso, dataVencimento));
            log.debug("Gerada parcela {}/{}. Vencimento: {}, Valor: {}", i + 1, numeroParcelasCheias, dataVencimento, valorMensalidadeCurso);

        }

        if (parcelas.isEmpty()) {
            log.warn("Nenhuma parcela foi gerada para o contrato ID: {}. Verifique o valor total e o valor da mensalidade.", contrato.getId());
        }

        return parcelas;
    }

    /**
     * Método auxiliar para criar uma instância de ContaReceber.
     */
    private ContaReceber criarContaReceber(Contrato contrato, BigDecimal valor, LocalDate dataVencimento) {
        ContaReceber conta = new ContaReceber();
        conta.setStatus(StatusContaReceber.ABERTA);
        conta.setContrato(contrato);
        conta.setDesconto(contrato.getDesconto());
        conta.setDataVencimento(dataVencimento);
        conta.setValorTotal(valor.setScale(2, RoundingMode.HALF_UP));
        return conta;
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
                    alterarStatusParaPago(existingEntity);
                    return existingEntity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Criando nova conta a receber para o contrato '{}'", contrato.getId());
                    ContaReceber entity = mapper.toEntity(request);
                    entity.setContrato(contrato);
                    entity.setStatus(StatusContaReceber.ABERTA);
                    return Mono.just(entity);
                }));
    }


    private void alterarStatusParaPago(ContaReceber existingEntity) {
        // A conta é considerada PAGA se o valor pago for maior ou igual ao valor total.
        if (existingEntity.getValorPago() != null && existingEntity.getValorTotal() != null && existingEntity.getDesconto() != null) {

            // 1. Calcula o fator de desconto de forma segura (ex: 10% -> 0.10)
            //    Usamos 4 casas de precisão para o fator, o que é suficiente para a maioria dos casos.
            BigDecimal valorASerPago = getValorASerPago(existingEntity);

            // 3. Compara o valor pago com o valor que DEVERIA ser pago (já com o desconto).
            //    A comparação `compareTo >= 0` significa "valorPago é maior ou igual a valorASerPago".
            if (existingEntity.getValorPago().compareTo(valorASerPago) >= 0) {
                existingEntity.setStatus(StatusContaReceber.PAGA);
            } else {
                existingEntity.setStatus(StatusContaReceber.ABERTA);
            }
        }
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