package com.escola.admin.model.request.cliente;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ContratoModeloRequest(
        @NotBlank(message = "O id do contrato é obrigatório.")
        Long id,
        @NotBlank(message = "O modelo do contrato é obrigatório.")
        String contratoDoc
) {
}