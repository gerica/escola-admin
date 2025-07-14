package com.escola.admin.config;

import com.escola.admin.model.request.ParametroRequest;
import com.escola.admin.service.ParametroService;
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

//    ParametroService service;

    @Override
    public void run(String... args) {
        log.info("Iniciando a verificação de dados iniciais...");
//        var req1 = ParametroRequest.builder()
//                .chave("CHAVE_CONTRATO_CIDADE_PADRAO")
//                .codigoMunicipio("12584")
//                .build();
//        service.salvar(req1);
//
//        var req2 = ParametroRequest.builder()
//                .chave("CHAVE_CONTRATO_MODELO_PADRAO")
//                .modeloContrato("12584")
//                .build();
//        service.salvar(req2);

        log.info("Verificação de dados iniciais concluída.");
    }

}