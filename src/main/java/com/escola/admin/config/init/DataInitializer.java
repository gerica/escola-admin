package com.escola.admin.config.init;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer implements CommandLineRunner {

    UsuarioInitializer usuarioInitializer;
    EmpresaInitializer empresaInitializer;

    @Override
    public void run(String... args) {
        log.info("Iniciando a verificação de dados iniciais...");
        usuarioInitializer.carga();
        empresaInitializer.carga();
        log.info("Verificação de dados iniciais concluída.");
    }
}