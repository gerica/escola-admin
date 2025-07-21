package com.escola.admin.config;

import com.escola.admin.model.entity.Role;
import com.escola.admin.model.entity.User;
import com.escola.admin.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer implements CommandLineRunner {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
//    ParametroService service;

    @Override
    public void run(String... args) {
        log.info("Iniciando a verificação de dados iniciais...");

        criarUsuarios();

        log.info("Verificação de dados iniciais concluída.");
    }


    void criarUsuarios() {
        // Cria um usuário ADMIN se não existir
        if (userRepository.findByUsername("admin").isEmpty()) {
            log.info("Criar usuário admin");
            User adminUser = User.builder()
                    .firstname("Admin")
                    .lastname("User")
                    .username("admin")
                    .password(passwordEncoder.encode("6vkWITTQcIKO2y1PEP6mPM")) // Senha forte para admin
                    .roles(Set.of(Role.ADMIN, Role.USER))
                    .build();
            userRepository.save(adminUser);
            log.info(">>> Usuário 'admin' criado com sucesso. Senha: 'admin123'");
        }

        // Cria um usuário USER se não existir
        if (userRepository.findByUsername("user").isEmpty()) {
            log.info("Criar usuário admin");
            User regularUser = User.builder()
                    .firstname("Regular")
                    .lastname("User")
                    .username("user")
                    .password(passwordEncoder.encode("password")) // Senha simples para usuário de teste
                    .roles(Set.of(Role.USER))
                    .build();
            userRepository.save(regularUser);
            log.info(">>> Usuário 'user' criado com sucesso. Senha: 'password'");
        }
    }

}