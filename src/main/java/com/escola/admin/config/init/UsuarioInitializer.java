package com.escola.admin.config.init;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Role;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.service.EmpresaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UsuarioInitializer {

    UsuarioRepository userRepository;
    PasswordEncoder passwordEncoder;
    PageableHelp pageableHelp;
    EmpresaService empresaService; // Injetado o repositório de empresas

    void carga() {
        log.info("<INIT> Carga de usuários...");
        // Tenta obter uma empresa existente para vincular aos usuários
        // Garanta que EmpresaInitializer seja executado antes para ter empresas no BD
        Optional<Empresa> empresaParaVinculo = getAnyExistingEmpresa();

        if (empresaParaVinculo.isEmpty()) {
            log.warn("Nenhuma empresa encontrada para vincular aos usuários. " +
                    "Certifique-se de que o EmpresaInitializer foi executado e criou empresas.");
        }

        criarUsuarioSuperAdmin(); // SUPER_ADMIN não terá empresa
        criarUsuarioAdminEmpresa(empresaParaVinculo);
        criarUsuarioCoordenador(empresaParaVinculo);
        criarUsuarioProfessor(empresaParaVinculo);
        criarUsuarioFinanceiro(empresaParaVinculo);
        criarUsuarioRecepcionista(empresaParaVinculo);
        log.info("<END> Carga de usuários concluída.");
    }

    // Método auxiliar para buscar uma empresa existente
    private Optional<Empresa> getAnyExistingEmpresa() {
        // Busca a primeira empresa cadastrada. Se houver 25, pega a primeira.
        // PageRequest.of(0, 1) busca 1 elemento na primeira página.
        Optional<Page<Empresa>> byFiltro = empresaService.findByFiltro("", pageableHelp.getPageable(Optional.empty(), Optional.empty(), Optional.empty()));
        return byFiltro.get().getContent().stream().findAny();

    }

    private void criarUsuarioSuperAdmin() {
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            log.info("Criando usuário 'superadmin'...");
            Usuario superAdminUser = Usuario.builder()
                    .firstname("Super")
                    .lastname("Admin")
                    .username("superadmin")
                    .email("superadmin@esc.com.br")
                    .password(passwordEncoder.encode("6vkWITTQcIKO2y1PEP6mPM"))
                    .roles(Set.of(Role.SUPER_ADMIN))
                    .enabled(true)
                    .empresa(null) // SUPER_ADMIN não tem empresa associada
                    .build();
            userRepository.save(superAdminUser);
            log.info(">>> Usuário 'superadmin' criado com sucesso. Senha: 'superadmin123'");
        }
    }

    private void criarUsuarioAdminEmpresa(Optional<Empresa> empresaOptional) {
        if (userRepository.findByUsername("adminempresa").isEmpty()) {
            log.info("Criando usuário 'adminempresa'...");
            Usuario adminEmpresaUser = Usuario.builder()
                    .firstname("Admin")
                    .lastname("Empresa")
                    .username("adminempresa")
                    .email("admin@esc.com.br")
                    .password(passwordEncoder.encode("adminempresa123"))
                    .roles(Set.of(Role.ADMIN_EMPRESA))
                    .enabled(true)
                    .empresa(empresaOptional.orElse(null)) // Vincula à empresa encontrada ou null
                    .build();
            userRepository.save(adminEmpresaUser);
            log.info(">>> Usuário 'adminempresa' criado com sucesso. Senha: 'adminempresa123'");
        }
    }

    private void criarUsuarioCoordenador(Optional<Empresa> empresaOptional) {
        if (userRepository.findByUsername("coordenador").isEmpty()) {
            log.info("Criando usuário 'coordenador'...");
            Usuario coordenadorUser = Usuario.builder()
                    .firstname("Coordenador")
                    .lastname("Teste")
                    .username("coordenador")
                    .email("coordenador@esc.com.br")
                    .password(passwordEncoder.encode("coordenador123"))
                    .roles(Set.of(Role.COORDENADOR))
                    .enabled(true)
                    .empresa(empresaOptional.orElse(null)) // Vincula à empresa encontrada ou null
                    .build();
            userRepository.save(coordenadorUser);
            log.info(">>> Usuário 'coordenador' criado com sucesso. Senha: 'coordenador123'");
        }
    }

    private void criarUsuarioProfessor(Optional<Empresa> empresaOptional) {
        if (userRepository.findByUsername("professor").isEmpty()) {
            log.info("Criando usuário 'professor'...");
            Usuario professorUser = Usuario.builder()
                    .firstname("Professor")
                    .lastname("Teste")
                    .username("professor")
                    .email("professor@esc.com.br")
                    .password(passwordEncoder.encode("professor123"))
                    .roles(Set.of(Role.PROFESSOR))
                    .enabled(true)
                    .empresa(empresaOptional.orElse(null)) // Vincula à empresa encontrada ou null
                    .build();
            userRepository.save(professorUser);
            log.info(">>> Usuário 'professor' criado com sucesso. Senha: 'professor123'");
        }
    }

    private void criarUsuarioFinanceiro(Optional<Empresa> empresaOptional) {
        if (userRepository.findByUsername("financeiro").isEmpty()) {
            log.info("Criando usuário 'financeiro'...");
            Usuario financeiroUser = Usuario.builder()
                    .firstname("Financeiro")
                    .lastname("Teste")
                    .username("financeiro")
                    .email("financeiro@esc.com.br")
                    .password(passwordEncoder.encode("financeiro123"))
                    .roles(Set.of(Role.FINANCEIRO))
                    .enabled(true)
                    .empresa(empresaOptional.orElse(null)) // Vincula à empresa encontrada ou null
                    .build();
            userRepository.save(financeiroUser);
            log.info(">>> Usuário 'financeiro' criado com sucesso. Senha: 'financeiro123'");
        }
    }

    private void criarUsuarioRecepcionista(Optional<Empresa> empresaOptional) {
        if (userRepository.findByUsername("recepcionista").isEmpty()) {
            log.info("Criando usuário 'recepcionista'...");
            Usuario recepcionistaUser = Usuario.builder()
                    .firstname("Recepcionista")
                    .lastname("Teste")
                    .username("recepcionista")
                    .email("recepcionista@esc.com.br")
                    .password(passwordEncoder.encode("recepcionista123"))
                    .roles(Set.of(Role.RECEPCIONISTA))
                    .enabled(false)
                    .empresa(empresaOptional.orElse(null)) // Vincula à empresa encontrada ou null
                    .build();
            userRepository.save(recepcionistaUser);
            log.info(">>> Usuário 'recepcionista' criado com sucesso. Senha: 'recepcionista123'");
        }
    }
}