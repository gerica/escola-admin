package com.escola.admin.model.response.cliente;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaReceberPorMesResumeResponse(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataRef,
        BigDecimal totalEsperado,
        BigDecimal totalRecebido,
        BigDecimal totalEmAberto
) {
}
