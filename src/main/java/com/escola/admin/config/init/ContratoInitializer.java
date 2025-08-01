package com.escola.admin.config.init;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.entity.cliente.StatusContrato;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.auxiliar.MatriculaService;
import com.escola.admin.service.auxiliar.TurmaService;
import com.escola.admin.service.cliente.ContratoService;
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
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ContratoInitializer {

    EmpresaService empresaService;
    ContratoService contratoService;
    TurmaService turmaService;
    MatriculaService matriculaService;
    Random random = new Random();
    PageableHelp pageableHelp;

    public void carga() {
        log.info("Iniciando a verificação de dados (contrato) iniciais...");

        criarContratos();

        log.info("Verificação de dados iniciais (contrato) concluída.");
    }

    // --- NOVO MÉTODO: CRIAR CONTRATOS ---
    void criarContratos() {
        if (contratoService.count() == 0) {

            Optional<Empresa> empresaOptional = getAnyExistingEmpresa();
            if (empresaOptional.isEmpty()) {
                log.warn("Nenhuma empresa encontrada. A carga de contratos será pulada.");
                return;
            }
            Empresa empresa = empresaOptional.get();

            List<Turma> turmasExistentes = getExistingTurmas(empresa.getId());
            if (turmasExistentes.isEmpty()) {
                log.warn("Nenhuma turma encontrada. A carga de contratos será pulada.");
                return;
            }

            // Mapeia todas as matrículas existentes para as turmas encontradas
            List<Matricula> matriculasExistentes = turmasExistentes.stream()
                    .flatMap(turma -> getExistingMatriculas(turma.getId()).stream())
                    .collect(Collectors.toList());

            if (matriculasExistentes.isEmpty()) {
                log.warn("Nenhuma matrícula encontrada para vincular. A carga de contratos será pulada.");
                return;
            }

            log.info("Nenhum contrato encontrado. Iniciando a criação de contratos de teste...");
            StatusContrato[] statusContratoValues = StatusContrato.values();

            for (int i = 0; i < matriculasExistentes.size(); i++) {
                Matricula matriculaAleatoria = matriculasExistentes.get(i);

                LocalDate dataInicio = LocalDate.now().minusDays(random.nextInt(365));
                LocalDate dataFim = dataInicio.plusMonths(random.nextInt(24) + 6); // Contratos de 6 a 30 meses

                ContratoRequest contrato = ContratoRequest.builder()
                        .idMatricula(matriculaAleatoria.getId())
                        .idEmpresa(empresa.getId())
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
                        .build();

                contratoService.save(contrato).block();
            }

            log.info(">>> 5 contratos de teste criados com sucesso e associados a matrículas.");
        } else {
            log.info("Contratos já existem no banco de dados. Nenhuma ação necessária para contratos.");
        }
    }

    private String getPeriodoPagamentoAleatorio() {
        String[] periodos = {"Mensal", "Trimestral", "Semestral", "Anual"};
        return periodos[random.nextInt(periodos.length)];
    }

    private Optional<Empresa> getAnyExistingEmpresa() {
        log.info("Buscando uma empresa existente para referência para matrículas...");
        Optional<Page<Empresa>> byFiltro = empresaService.findByFiltro("", pageableHelp.getPageable(0, 1, new ArrayList<>()));
        return byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty() ?
                byFiltro.get().getContent().stream().findFirst() : Optional.empty();
    }

    private List<Turma> getExistingTurmas(Long idEmpresa) {
        log.info("Buscando turmas existentes para a empresa ID: {}", idEmpresa);
        // Ajuste o pageable para buscar algumas turmas (ex: as 5 primeiras)
        Optional<Page<Turma>> byFiltro = turmaService.findByFiltro("", idEmpresa, pageableHelp.getPageable(0, 5, new ArrayList<>()));
        return byFiltro.map(Page::getContent).orElse(Collections.emptyList());
    }

    private List<Matricula> getExistingMatriculas(Long idTurma) {
        log.info("Buscando matrículas para a turma ID: {}", idTurma);
        Optional<Page<Matricula>> byFiltro = matriculaService.findByTurma(idTurma, pageableHelp.getPageable(0, 10, new ArrayList<>()));
        return byFiltro.map(Page::getContent).orElse(Collections.emptyList());
    }
}