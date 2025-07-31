package com.escola.admin.config.init;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.entity.cliente.StatusContrato;
import com.escola.admin.repository.cliente.ClienteRepository;
import com.escola.admin.repository.cliente.ContratoRepository;
import com.escola.admin.service.EmpresaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ContratoInitializer {

    ClienteRepository clienteRepository;
    ContratoRepository contratoRepository; // Injetar o repositório de Contrato
    Random random = new Random();
    PageableHelp pageableHelp;
    EmpresaService empresaService; // Injetado o repositório de empresas


    void carga() {
        log.info("Iniciando a verificação de dados (cliente) iniciais...");

        Optional<Empresa> empresaParaVinculo = getAnyExistingEmpresa();
        if (empresaParaVinculo.isEmpty()) {
            log.warn("Nenhuma empresa encontrada para vincular aos usuários. " +
                    "Certifique-se de que o EmpresaInitializer foi executado e criou empresas.");
        }

        criarContratos(empresaParaVinculo); // Novo método para contratos

        log.info("Verificação de dados iniciais (cliente) concluída.");
    }

    // --- NOVO MÉTODO: CRIAR CONTRATOS ---
    void criarContratos(Optional<Empresa> empresaParaVinculo) {
        if (contratoRepository.count() == 0) {
            log.info("Nenhum contrato encontrado. Iniciando a criação de 25 contratos de teste...");

            List<Cliente> clientes = (List<Cliente>) clienteRepository.findAll(); // Pega todos os clientes existentes

            if (clientes.isEmpty()) {
                log.warn("Nenhum cliente encontrado para associar contratos. Crie clientes primeiro.");
                return;
            }

            List<Contrato> contratos = new ArrayList<>();
            StatusContrato[] statusContratoValues = StatusContrato.values();

            for (int i = 0; i < 25; i++) {
                // Pega um cliente aleatório para associar o contrato
                Cliente clienteAleatorio = clientes.get(random.nextInt(clientes.size()));

                LocalDate dataInicio = LocalDate.now().minusDays(random.nextInt(365));
                LocalDate dataFim = dataInicio.plusMonths(random.nextInt(24) + 6); // Contratos de 6 a 30 meses

                Contrato contrato = Contrato.builder()
                        .cliente(clienteAleatorio)
                        .numeroContrato("CONTRATO-" + String.format("%05d", i + 1))
                        .dataInicio(dataInicio)
                        .dataFim(dataFim)
                        .valorTotal(BigDecimal.valueOf(1000 + random.nextDouble() * 9000).setScale(2, RoundingMode.HALF_UP))
                        .statusContrato(statusContratoValues[random.nextInt(statusContratoValues.length)])
                        .descricao("Contrato de prestação de serviços " + (i + 1))
                        .termosCondicoes("Termos e condições padrão do contrato " + (i + 1) + ".")
                        .dataAssinatura(LocalDateTime.now().minusDays(random.nextInt(30)))
                        .periodoPagamento(getPeriodoPagamentoAleatorio())
                        .dataProximoPagamento(LocalDate.now().plusDays(random.nextInt(30)))
                        .observacoes("Observações diversas para o contrato " + (i + 1) + ".")
                        .empresa(empresaParaVinculo.orElse(null))
                        .build();

                contratos.add(contrato);
            }

            contratoRepository.saveAll(contratos);
            log.info(">>> 25 contratos de teste criados com sucesso e associados a clientes.");
        } else {
            log.info("Contratos já existem no banco de dados. Nenhuma ação necessária para contratos.");
        }
    }

    // Helper para gerar período de pagamento aleatório
    private String getPeriodoPagamentoAleatorio() {
        String[] periodos = {"Mensal", "Trimestral", "Semestral", "Anual"};
        return periodos[random.nextInt(periodos.length)];
    }
    // --- FIM DO NOVO MÉTODO ---

    // Método auxiliar para buscar uma empresa existente
    private Optional<Empresa> getAnyExistingEmpresa() {
        Optional<Page<Empresa>> byFiltro = empresaService.findByFiltro("", pageableHelp.getPageable(0, 1, new ArrayList<>()));
        return byFiltro.get().getContent().stream().findAny();

    }
}