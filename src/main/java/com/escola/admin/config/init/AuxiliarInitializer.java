package com.escola.admin.config.init;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.request.auxiliar.CargoRequest;
import com.escola.admin.model.request.auxiliar.CursoRequest;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.auxiliar.CargoService;
import com.escola.admin.service.auxiliar.CursoService;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuxiliarInitializer {

    PageableHelp pageableHelp;
    EmpresaService empresaService;
    CargoService cargoService; // Inject CargoService
    CursoService cursoService;   // Inject CursoService

    @PostConstruct // This annotation makes sure this method runs after dependency injection
    public void init() {
        carga();
    }

    void carga() {
        log.info("<INIT> Iniciando carga de dados auxiliares (Cargos e Cursos)...");

        // Tenta obter uma empresa existente (se necessário para futuras vinculações)
        Optional<Empresa> empresaParaVinculo = getAnyExistingEmpresa();

        if (empresaParaVinculo.isEmpty()) {
            log.warn("Nenhuma empresa encontrada. Isso pode ser um problema se " +
                    "Cargos ou Cursos precisarem de vinculação com Empresa. " +
                    "Certifique-se de que o EmpresaInitializer foi executado e criou empresas.");
        }

        // --- Criação de Cargos ---
        log.info("Criando 5 cargos de exemplo...");
        List<CargoRequest> cargosToCreate = List.of(
                CargoRequest.builder().nome("Professor(a)").descricao("Responsável por lecionar aulas.").ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CargoRequest.builder().nome("Coordenador(a)").descricao("Gerencia equipe pedagógica.").ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CargoRequest.builder().nome("Secretário(a)").descricao("Atendimento e organização administrativa.").ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CargoRequest.builder().nome("Auxiliar de Limpeza").descricao("Manutenção da limpeza e organização do ambiente.").ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CargoRequest.builder().nome("Diretor(a)").descricao("Gestão geral da instituição.").ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build()
        );

        cargosToCreate.forEach(request -> cargoService.save(request)
                .doOnSuccess(cargo -> log.info("Cargo '{}' salvo com sucesso.", cargo.getNome()))
                .doOnError(e -> log.error("Falha ao salvar cargo '{}': {}", request.nome(), e.getMessage()))
                .subscribe() // Subscribe to trigger the reactive flow
        );
        log.info("Criação de cargos concluída.");

        // --- Criação de Cursos ---
        log.info("Criando 5 cursos de exemplo...");
        List<CursoRequest> cursosToCreate = List.of(
                CursoRequest.builder().nome("Ballet Clássico Infantil").descricao("Aulas de ballet para crianças de 5 a 8 anos.").duracao("1 ano").categoria("Dança").valorMensalidade(150.00).ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CursoRequest.builder().nome("Jazz Dance Avançado").descricao("Técnicas avançadas de Jazz Dance.").duracao("6 meses").categoria("Dança").valorMensalidade(180.00).ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CursoRequest.builder().nome("Musicalização para Bebês").descricao("Introdução à música para bebês de 0 a 2 anos.").duracao("3 meses").categoria("Música").valorMensalidade(120.00).ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CursoRequest.builder().nome("Educação Infantil 1 (Pré-Escola)").descricao("Ensino fundamental para crianças de 4 anos.").duracao("1 ano").categoria("Educação").valorMensalidade(500.00).ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build(),
                CursoRequest.builder().nome("Dança de Rua (Hip Hop)").descricao("Aulas de Hip Hop para adolescentes.").duracao("8 meses").categoria("Dança").valorMensalidade(160.00).ativo(true).idEmpresa(empresaParaVinculo.get().getId()).build()
        );

        cursosToCreate.forEach(request -> cursoService.save(request)
                .doOnSuccess(curso -> log.info("Curso '{}' salvo com sucesso.", curso.getNome()))
                .doOnError(e -> log.error("Falha ao salvar curso '{}': {}", request.nome(), e.getMessage()))
                .subscribe() // Subscribe to trigger the reactive flow
        );
        log.info("Criação de cursos concluída.");

        log.info("<END> Carga de dados auxiliares concluída.");
    }

    private Optional<Empresa> getAnyExistingEmpresa() {
        log.info("Buscando uma empresa existente para referência...");
        // Use Mono.fromCallable para encapsular a chamada bloqueante do serviço
        Optional<Page<Empresa>> byFiltro = empresaService.findByFiltro("", pageableHelp.getPageable(0, 10, new ArrayList<>()));

        if (byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty()) {
            log.info("Empresa encontrada para referência.");
            return byFiltro.get().getContent().stream().findAny();
        }
        log.warn("Nenhuma empresa existente encontrada.");
        return Optional.empty();
    }
}