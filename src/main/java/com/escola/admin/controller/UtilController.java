package com.escola.admin.controller;

import com.escola.admin.model.entity.Role;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UtilController {

    /**
     * Retorna a lista de todas as Roles (funções) disponíveis no sistema,
     * de forma reativa.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()") // Garante que apenas usuários logados possam chamar
    public List<Role> getAvailableRoles() {
        log.info("Buscando roles disponíveis para o usuário logado.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Verifica se o usuário tem a permissão de SUPER_ADMIN
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> Role.SUPER_ADMIN.name().equals(grantedAuthority.getAuthority()));

        // 2. Se for SUPER_ADMIN, retorna a lista completa de roles
        if (isSuperAdmin) {
            return Arrays.asList(Role.values());
        }

        // 3. Se não for, retorna todas as roles EXCETO SUPER_ADMIN
        return Arrays.stream(Role.values())
                .filter(role -> role != Role.SUPER_ADMIN)
                .collect(Collectors.toList());

    }
}
