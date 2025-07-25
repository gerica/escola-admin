package com.escola.admin.model.response;

import com.escola.admin.model.entity.Role;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationResponse {
    String token;
    String username;
    String firstName;
    String lastName;
    Set<Role> roles;
    boolean precisaAlterarSenha;
    EmpresaResponse empresa;
}