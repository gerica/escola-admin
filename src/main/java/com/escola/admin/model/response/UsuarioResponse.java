package com.escola.admin.model.response;

import com.escola.admin.model.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

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
        Set<Role> roles

) {
}
