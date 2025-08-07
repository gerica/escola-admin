package com.escola.admin.model.response.cliente;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaReceberResponse(
        Long id,
        BigDecimal valorTotal,
        BigDecimal desconto,
        BigDecimal valorPago,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        String observacoes
) {
}
