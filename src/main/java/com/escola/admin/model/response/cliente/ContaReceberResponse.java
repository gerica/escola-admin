package com.escola.admin.model.response.cliente;

import com.escola.admin.model.entity.cliente.StatusContaReceber;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaReceberResponse(
        Long id,
        BigDecimal valorTotal,
        BigDecimal desconto,
        StatusContaReceber status,
        BigDecimal valorPago,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        String observacoes,
        LocalDate dataCadastro,
        LocalDate dataAtualizacao
) {
}
