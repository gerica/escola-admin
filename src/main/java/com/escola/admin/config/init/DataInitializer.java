package com.escola.admin.config.init;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class DataInitializer implements CommandLineRunner {

    final UsuarioInitializer usuarioInitializer;
    final EmpresaInitializer empresaInitializer;
    final ClienteInitializer clienteInitializer;
    final AuxiliarInitializer auxiliarInitializer;
    final MatriculaInitializer matriculaInitializer;
    final ContratoInitializer contratoInitializer;

    @Value("${modo.desenvolvimento.ligado}")
    private Boolean modoDesenvolvimentoLigado;

    @Override
    public void run(String... args) {
        log.info("Iniciando. Modo de desenvolvimento ligado: {}", modoDesenvolvimentoLigado);
        if (!modoDesenvolvimentoLigado) {
            return;
        }
        log.info("Iniciando a verificação de dados iniciais...");
        empresaInitializer.carga();
        usuarioInitializer.carga();
        clienteInitializer.carga();
        auxiliarInitializer.carga();
        matriculaInitializer.carga();
//        contratoInitializer.carga();
        log.info("Verificação de dados iniciais concluída.");
    }
}