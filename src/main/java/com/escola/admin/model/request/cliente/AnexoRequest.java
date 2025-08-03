package com.escola.admin.model.request.cliente;

import jakarta.validation.constraints.NotBlank;

public record AnexoRequest(
        Long id,
        @NotBlank(message = "O id do contrato é obrigatório.")
        Long idContrato,
        @NotBlank(message = "O nome do arquivo é obrigatório.")
        String nomeArquivo,
        @NotBlank(message = "O conteúdo do arquivo é obrigatório.")
        String conteudoBase64
) {
}
