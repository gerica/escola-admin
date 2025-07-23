package com.escola.admin.model.response;

import com.escola.admin.model.entity.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Set;

public record UsuarioResponse(
        Long id,
        String username,
        String password,
        String firstname,
        String lastname,
        String email,
        boolean enabled,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        EmpresaResponse empresa,
        Set<Role> roles,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataCadastro,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataAtualizacao

) {
}
