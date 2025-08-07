package com.escola.admin.model.request.cliente;

import com.escola.admin.model.entity.cliente.StatusContaReceber;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record ContaReceberRequest(
        Long id,
        @NotBlank(message = "O id contrato é obrigatório.")
        Long idContrato,
        @NotNull(message = "O valor total do contrato é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor total do contrato deve ser maior que zero.")
        BigDecimal valorTotal,
        BigDecimal desconto,
        BigDecimal valorPago,
        @NotNull(message = "A data de vencimento é obrigatório.")
        @FutureOrPresent(message = "A data de vencimento não pode ser passada.")
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        StatusContaReceber status,
        String observacoes
) {
}
