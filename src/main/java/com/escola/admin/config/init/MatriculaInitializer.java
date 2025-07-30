package com.escola.admin.config.init;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.auxiliar.StatusMatricula;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.ClienteDependente;
import com.escola.admin.model.request.auxiliar.MatriculaRequest;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.auxiliar.MatriculaService;
import com.escola.admin.service.auxiliar.TurmaService;
import com.escola.admin.service.cliente.ClienteDependenteService;
import com.escola.admin.service.cliente.ClienteService;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MatriculaInitializer {

    PageableHelp pageableHelp;
    EmpresaService empresaService;
    TurmaService turmaService;
    ClienteService clienteService; // Serviço para Cliente
    ClienteDependenteService clienteDependenteService; // Serviço para ClienteDependente
    MatriculaService matriculaService; // Serviço para Matricula

    @PostConstruct
    void carga() {
        log.info("<INIT> Carga matriculas");
        this.cargaMatriculas();
        log.info("<END> Carga matriculas");
    }

    void cargaMatriculas() {
        log.info("<INIT> Iniciando carga de dados de Matrículas...");

        Optional<Empresa> empresaParaVinculo = getAnyExistingEmpresa();

        if (empresaParaVinculo.isEmpty()) {
            log.warn("Nenhuma empresa encontrada. A carga de matrículas será pulada. " +
                    "Certifique-se de que o EmpresaInitializer foi executado e criou empresas.");
            return;
        }

        Empresa empresa = empresaParaVinculo.get();


        List<Turma> turmasExistentes = getExistingTurmas(empresa.getId());
        List<Cliente> clientesExistentes = getExistingClientes(empresa.getId());
        List<ClienteDependente> dependentesExistentes = getExistingDependentes(clientesExistentes.get(0).getId());

        if (turmasExistentes.isEmpty()) {
            log.warn("Carga de matrículas pulada: Nenhuma turma encontrada. Certifique-se de que AuxiliarInitializer rodou.");
            return;
        }

        if (clientesExistentes.isEmpty() && dependentesExistentes.isEmpty()) {
            log.warn("Carga de matrículas pulada: Nenhum cliente ou dependente encontrado.");
            return;
        }

        // Verifica se já existem matrículas para evitar duplicação em reinícios do app
        Optional<Page<Matricula>> existingMatriculas = matriculaService.findByTurma(turmasExistentes.get(0).getId(), pageableHelp.getPageable(0, 1, new ArrayList<>()));
        if (existingMatriculas.isPresent() && !existingMatriculas.get().getContent().isEmpty()) {
            log.info("Matrículas já cadastradas. Pulando carga de matrículas...");
            return;
        }

        log.info("Iniciando criação de matrículas de exemplo...");

        List<MatriculaRequest> matriculasToCreate = new ArrayList<>();

        // Exemplo 1: Matrícula de um Cliente principal em uma turma
        if (!clientesExistentes.isEmpty() && !turmasExistentes.isEmpty()) {
            matriculasToCreate.add(MatriculaRequest.builder()
                    .idTurma(turmasExistentes.get(0).getId())
                    .idCliente(clientesExistentes.get(0).getId())
                    .status(StatusMatricula.ATIVA)
                    .observacoes("Primeira matrícula do cliente principal.")
                    .build());
            log.info("Matrícula para cliente '{}' na turma '{}' adicionada para criação.", clientesExistentes.get(0).getNome(), turmasExistentes.get(0).getNome());
        }

        // Exemplo 2: Matrícula de um Cliente Dependente em outra turma
        if (dependentesExistentes.size() > 0 && turmasExistentes.size() > 1) {
            matriculasToCreate.add(MatriculaRequest.builder()
                    .idTurma(turmasExistentes.get(1).getId())
                    .idClienteDependente(dependentesExistentes.get(0).getId())
                    .status(StatusMatricula.ATIVA)
                    .observacoes("Matrícula de dependente.")
                    .build());
            log.info("Matrícula para dependente '{}' na turma '{}' adicionada para criação.", dependentesExistentes.get(0).getNome(), turmasExistentes.get(1).getNome());
        }

        // Exemplo 3: Outra matrícula de cliente, talvez em status diferente
        if (clientesExistentes.size() > 1 && turmasExistentes.size() > 2) {
            matriculasToCreate.add(MatriculaRequest.builder()
                    .idTurma(turmasExistentes.get(2).getId())
                    .idCliente(clientesExistentes.get(1).getId())
                    .status(StatusMatricula.ABERTA)
                    .observacoes("Matrícula aguardando formação da turma.")
                    .build());
            log.info("Matrícula para cliente '{}' na turma '{}' adicionada para criação.", clientesExistentes.get(1).getNome(), turmasExistentes.get(2).getNome());
        }


        if (matriculasToCreate.isEmpty()) {
            log.warn("Nenhuma matrícula de exemplo para criar. Verifique se há dados suficientes (Turmas, Clientes/Dependentes).");
            return;
        }

        matriculasToCreate.forEach(request -> matriculaService.save(request)
                .doOnSuccess(matricula -> log.info("Matrícula ID:{} (Turma: {}, Cliente:{}, Dependente:{}) salva com sucesso.",
                        matricula.getId(),
                        Optional.ofNullable(matricula.getTurma()).map(Turma::getNome).orElse("N/A"),
                        Optional.ofNullable(matricula.getCliente()).map(Cliente::getNome).orElse("N/A"),
                        Optional.ofNullable(matricula.getClienteDependente()).map(ClienteDependente::getNome).orElse("N/A")))
                .doOnError(e -> log.error("Falha ao salvar matrícula para Turma ID:{} Cliente ID:{} Dependente ID:{}: {}",
                        request.idTurma(), request.idCliente(), request.idClienteDependente(), e.getMessage()))
                .subscribe()
        );

        log.info("<END> Carga de dados de Matrículas concluída.");
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

    private List<Cliente> getExistingClientes(Long idEmpresa) {
        log.info("Buscando clientes existentes para a empresa ID: {}", idEmpresa);
        // Ajuste o pageable para buscar alguns clientes (ex: os 5 primeiros)
        Optional<Page<Cliente>> byFiltro = clienteService.findByFiltro("", idEmpresa, pageableHelp.getPageable(0, 5, new ArrayList<>()));
        return byFiltro.map(Page::getContent).orElse(Collections.emptyList());
    }

    private List<ClienteDependente> getExistingDependentes(Long idCliente) {
        log.info("Buscando dependentes existentes para o cliente ID: {}", idCliente);
        // Ajuste o pageable para buscar alguns dependentes (ex: os 5 primeiros)
        Optional<List<ClienteDependente>> byFiltro = clienteDependenteService.findAllByClienteId(idCliente);

        // Correctly extract the List from the Optional, or return an empty list if not present
        return byFiltro.orElse(Collections.emptyList());
    }
}