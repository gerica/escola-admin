package com.escola.admin.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EmpresaResponse(
        Long id,
        String nomeFantasia,
        String razaoSocial,
        String cnpj,
        String inscricaoEstadual,
        String telefone,
        String email,
        String endereco,
        LogoResponse logo,
        Boolean ativo,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataCadastro,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataAtualizacao
) {
    /**
     * Cria uma instância padrão para representar um usuário sem empresa associada.
     *
     * @return um EmpresaResponse com uma mensagem informativa.
     */
    public static EmpresaResponse nenhumaAssociada() {
        return new EmpresaResponse(
                null, // id
                "Nenhuma empresa associada",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null
                // ... outros campos como null
        );
    }
}
