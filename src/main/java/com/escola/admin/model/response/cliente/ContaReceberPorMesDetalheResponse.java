package com.escola.admin.model.response.cliente;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record ContaReceberPorMesDetalheResponse(
        ContratoResponse contrato,
        String nome,
        BigDecimal valorTotal,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataVencimento,
        BigDecimal valorPago,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataPagamento,
        Integer diasAtraso
) {
}
