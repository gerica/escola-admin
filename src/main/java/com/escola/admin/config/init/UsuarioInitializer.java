package com.escola.admin.config.init;

import com.escola.admin.model.entity.Role;
import com.escola.admin.model.entity.User;
import com.escola.admin.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UsuarioInitializer {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    void carga() {
        criarUsuarioSuperAdmin();
        criarUsuarioAdminEmpresa();
        criarUsuarioCoordenador();
        criarUsuarioProfessor();
        criarUsiarioFinanceiro();
        criarUsuarioRecepcionista();
    }

    private void criarUsuarioRecepcionista() {
        // Usuário RECEPCIONISTA
        if (userRepository.findByUsername("recepcionista").isEmpty()) {
            log.info("Criando usuário 'recepcionista'...");
            User recepcionistaUser = User.builder()
                    .firstname("Recepcionista")
                    .lastname("Teste")
                    .username("recepcionista")
                    .password(passwordEncoder.encode("recepcionista123")) // Senha para recepcionista
                    .roles(Set.of(Role.RECEPCIONISTA))
                    .build();
            userRepository.save(recepcionistaUser);
            log.info(">>> Usuário 'recepcionista' criado com sucesso. Senha: 'recepcionista123'");
        }
    }

    private void criarUsiarioFinanceiro() {
        // Usuário FINANCEIRO
        if (userRepository.findByUsername("financeiro").isEmpty()) {
            log.info("Criando usuário 'financeiro'...");
            User financeiroUser = User.builder()
                    .firstname("Financeiro")
                    .lastname("Teste")
                    .username("financeiro")
                    .password(passwordEncoder.encode("financeiro123")) // Senha para financeiro
                    .roles(Set.of(Role.FINANCEIRO))
                    .build();
            userRepository.save(financeiroUser);
            log.info(">>> Usuário 'financeiro' criado com sucesso. Senha: 'financeiro123'");
        }
    }

    private void criarUsuarioProfessor() {
        // Usuário PROFESSOR
        if (userRepository.findByUsername("professor").isEmpty()) {
            log.info("Criando usuário 'professor'...");
            User professorUser = User.builder()
                    .firstname("Professor")
                    .lastname("Teste")
                    .username("professor")
                    .password(passwordEncoder.encode("professor123")) // Senha para professor
                    .roles(Set.of(Role.PROFESSOR))
                    .build();
            userRepository.save(professorUser);
            log.info(">>> Usuário 'professor' criado com sucesso. Senha: 'professor123'");
        }
    }

    private void criarUsuarioCoordenador() {
        // Usuário COORDENADOR
        if (userRepository.findByUsername("coordenador").isEmpty()) {
            log.info("Criando usuário 'coordenador'...");
            User coordenadorUser = User.builder()
                    .firstname("Coordenador")
                    .lastname("Teste")
                    .username("coordenador")
                    .password(passwordEncoder.encode("coordenador123")) // Senha para coordenador
                    .roles(Set.of(Role.COORDENADOR))
                    .build();
            userRepository.save(coordenadorUser);
            log.info(">>> Usuário 'coordenador' criado com sucesso. Senha: 'coordenador123'");
        }
    }

    private void criarUsuarioAdminEmpresa() {
        // Usuário ADMIN_EMPRESA (administrador de uma empresa específica)
        if (userRepository.findByUsername("adminempresa").isEmpty()) {
            log.info("Criando usuário 'adminempresa'...");
            User adminEmpresaUser = User.builder()
                    .firstname("Admin")
                    .lastname("Empresa")
                    .username("adminempresa")
                    .password(passwordEncoder.encode("adminempresa123")) // Senha para adminempresa
                    .roles(Set.of(Role.ADMIN_EMPRESA))
                    .build();
            userRepository.save(adminEmpresaUser);
            log.info(">>> Usuário 'adminempresa' criado com sucesso. Senha: 'adminempresa123'");
        }
    }

    private void criarUsuarioSuperAdmin() {
        // Usuário SUPER_ADMIN (administrador do sistema)
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            log.info("Criando usuário 'superadmin'...");
            User superAdminUser = User.builder()
                    .firstname("Super")
                    .lastname("Admin")
                    .username("superadmin")
                    .password(passwordEncoder.encode("6vkWITTQcIKO2y1PEP6mPM")) // Senha para superadmin
                    .roles(Set.of(Role.SUPER_ADMIN))
                    .build();
            userRepository.save(superAdminUser);
            log.info(">>> Usuário 'superadmin' criado com sucesso. Senha: 'superadmin123'");
        }
    }

}