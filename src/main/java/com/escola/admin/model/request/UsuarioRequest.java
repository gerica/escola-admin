package com.escola.admin.model.request;

import com.escola.admin.model.entity.Role;

import java.util.Set;

public record UsuarioRequest(
        Long id,
        String username,
        String password,
        String firstname,
        String lastname,
        String email,
        boolean enabled,
        Long idEmpresa,
        Set<Role> roles
) {
}
