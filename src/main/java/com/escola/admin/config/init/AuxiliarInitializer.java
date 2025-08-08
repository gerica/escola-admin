package com.escola.admin.config.init;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.auxiliar.Cargo;
import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.entity.auxiliar.StatusTurma;
import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.request.auxiliar.CargoRequest;
import com.escola.admin.model.request.auxiliar.CursoRequest;
import com.escola.admin.model.request.auxiliar.TurmaRequest;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.auxiliar.CargoService;
import com.escola.admin.service.auxiliar.CursoService;
import com.escola.admin.service.auxiliar.TurmaService;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuxiliarInitializer {

    PageableHelp pageableHelp;
    EmpresaService empresaService;
    CargoService cargoService;
    CursoService cursoService;
    TurmaService turmaService; // 1. Dependência para o serviço de Turma

    @PostConstruct
    public void init() {
        carga();
    }

    void carga() {
        log.info("<INIT> Iniciando carga de dados auxiliares (Cargos, Cursos e Turmas)...");

        Optional<Empresa> empresaParaVinculo = getAnyExistingEmpresa();

        if (empresaParaVinculo.isEmpty()) {
            log.warn("Nenhuma empresa encontrada. A carga de dados auxiliares que dependem de empresa será pulada. " +
                    "Certifique-se de que o EmpresaInitializer foi executado e criou empresas.");
        }

        cargaCargos(empresaParaVinculo);
        cargaCursos(empresaParaVinculo);
        cargaTurmas(empresaParaVinculo); // 6. Chamada para o novo método de carga

        log.info("<END> Carga de dados auxiliares concluída.");
    }

    /**
     * Método corrigido para popular a entidade Turma com dados mais completos.
     */
    private void cargaTurmas(Optional<Empresa> empresaParaVinculo) {
        log.info("Iniciando carga de Turmas...");

        if (empresaParaVinculo.isEmpty()) {
            log.warn("Carga de turmas pulada: Nenhuma empresa de referência encontrada.");
            return;
        }
        Empresa empresa = empresaParaVinculo.get();

        Optional<Page<Turma>> byFiltro = turmaService.findByFiltro("", empresa.getId(), pageableHelp.getPageable(0, 1, new ArrayList<>()));
        if (byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty()) {
            log.info("Turmas já cadastradas...");
            return;
        }

        List<Curso> cursosExistentes = getExistingCursos(empresa.getId());
        if (cursosExistentes.size() < 5) {
            log.warn("Carga de turmas pulada: Não há cursos suficientes para criar as turmas de exemplo. Cursos encontrados: {}", cursosExistentes.size());
            return;
        }
        log.info("{} cursos encontrados para vincular às turmas.", cursosExistentes.size());

        LocalDate dtInicio = LocalDate.of(2025, 1, 31);
        // CORREÇÃO: Ajuste na criação das Turmas para usar os campos corretos do TurmaRequest
        // e adicionar dados mais realistas.
        List<TurmaRequest> turmasToCreate = List.of(
                TurmaRequest.builder()
                        .nome("Ballet Manhã 2024")
                        .codigo("BLT-M-24")
                        .anoPeriodo("2024")
                        .capacidadeMaxima(15)
                        .status(StatusTurma.ATIVA)
                        .horarioInicio(LocalTime.of(9, 0))
                        .horarioFim(LocalTime.of(10, 30))
                        .diasDaSemana(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                        .professor("Ana Clara")
                        .idEmpresa(empresa.getId())
                        .idCurso(cursosExistentes.get(0).getId())
                        .dataInicio(dtInicio)
                        .dataFim(calcularDataFim(dtInicio, cursosExistentes.get(0)))
                        .build(),
                TurmaRequest.builder()
                        .nome("Jazz Tarde 2024")
                        .codigo("JAZ-T-24")
                        .anoPeriodo("2024")
                        .capacidadeMaxima(20)
                        .status(StatusTurma.ATIVA)
                        .horarioInicio(LocalTime.of(14, 0))
                        .horarioFim(LocalTime.of(15, 30))
                        .diasDaSemana(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
                        .professor("Bruno Rocha")
                        .idEmpresa(empresa.getId())
                        .idCurso(cursosExistentes.get(1).getId())
                        .dataInicio(dtInicio)
                        .dataFim(calcularDataFim(dtInicio, cursosExistentes.get(0)))
                        .build(),
                TurmaRequest.builder()
                        .nome("Bebês Musicalizados 2024")
                        .codigo("MUS-B-24")
                        .anoPeriodo("2024")
                        .capacidadeMaxima(10)
                        .status(StatusTurma.EM_FORMACAO)
                        .horarioInicio(LocalTime.of(10, 0))
                        .horarioFim(LocalTime.of(10, 45))
                        .diasDaSemana(Set.of(DayOfWeek.FRIDAY))
                        .professor("Carla Dias")
                        .idEmpresa(empresa.getId())
                        .idCurso(cursosExistentes.get(2).getId())
                        .dataInicio(dtInicio)
                        .dataFim(calcularDataFim(dtInicio, cursosExistentes.get(2)))
                        .build(),
                TurmaRequest.builder()
                        .nome("Pré-Escola Integral 2024")
                        .codigo("PRE-I-24")
                        .anoPeriodo("2024")
                        .capacidadeMaxima(25)
                        .status(StatusTurma.LOTADA)
                        .horarioInicio(LocalTime.of(8, 0))
                        .horarioFim(LocalTime.of(17, 0))
                        .diasDaSemana(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                        .professor("Fernanda Lima")
                        .idEmpresa(empresa.getId())
                        .idCurso(cursosExistentes.get(3).getId())
                        .dataInicio(dtInicio)
                        .dataFim(calcularDataFim(dtInicio, cursosExistentes.get(2)))
                        .build(),
                TurmaRequest.builder()
                        .nome("Hip Hop Noite 2024")
                        .codigo("HIP-N-24")
                        .anoPeriodo("2024")
                        .capacidadeMaxima(18)
                        .status(StatusTurma.ATIVA)
                        .horarioInicio(LocalTime.of(19, 0))
                        .horarioFim(LocalTime.of(20, 30))
                        .diasDaSemana(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                        .professor("Lucas Mendes")
                        .idEmpresa(empresa.getId())
                        .idCurso(cursosExistentes.get(4).getId())
                        .dataInicio(dtInicio)
                        .dataFim(calcularDataFim(dtInicio, cursosExistentes.get(4)))
                        .build()
        );

        log.info("Criando 5 turmas de exemplo...");
        turmasToCreate.forEach(request -> turmaService.save(request)
                .doOnSuccess(turma -> log.info("Turma '{}' salva com sucesso.", turma.getNome()))
                .doOnError(e -> log.error("Falha ao salvar turma '{}': {}", request.nome(), e.getMessage()))
                .subscribe()
        );
        log.info("Criação de turmas concluída.");
    }

    private void cargaCursos(Optional<Empresa> empresaParaVinculo) {
        log.info("Iniciando carga de Cursos...");
        Optional<Page<Curso>> byFiltro = cursoService.findByFiltro("", pageableHelp.getPageable(0, 1, new ArrayList<>()));
        if (byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty()) {
            log.info("Cursos já cadastradas...");
            return;
        }
        // 5. Melhoria de robustez
        if (empresaParaVinculo.isEmpty()) {
            log.warn("Carga de cursos pulada: Nenhuma empresa de referência encontrada.");
            return;
        }
        var idEmpresa = empresaParaVinculo.get().getId();

        log.info("Criando 5 cursos de exemplo...");
        List<CursoRequest> cursosToCreate = List.of(
                CursoRequest.builder().nome("Ballet Clássico Infantil").descricao("Aulas de ballet para crianças de 5 a 8 anos.").duracaoValor(1).duracaoUnidade("Anos").categoria("Dança").valorMensalidade(150.00).ativo(true).idEmpresa(idEmpresa).build(),
                CursoRequest.builder().nome("Jazz Dance Avançado").descricao("Técnicas avançadas de Jazz Dance.").duracaoValor(6).duracaoUnidade("Meses").categoria("Dança").valorMensalidade(180.00).ativo(true).idEmpresa(idEmpresa).build(),
                CursoRequest.builder().nome("Musicalização para Bebês").descricao("Introdução à música para bebês de 0 a 2 anos.").duracaoValor(22).duracaoUnidade("Semanas").categoria("Música").valorMensalidade(120.00).ativo(true).idEmpresa(idEmpresa).build(),
                CursoRequest.builder().nome("Educação Infantil 1 (Pré-Escola)").descricao("Ensino fundamental para crianças de 4 anos.").duracaoValor(4).duracaoUnidade("Meses").categoria("Educação").valorMensalidade(500.00).ativo(true).idEmpresa(idEmpresa).build(),
                CursoRequest.builder().nome("Dança de Rua (Hip Hop)").descricao("Aulas de Hip Hop para adolescentes.").duracaoValor(8).duracaoUnidade("Meses").categoria("Dança").valorMensalidade(160.00).ativo(true).idEmpresa(idEmpresa).build()
        );

        cursosToCreate.forEach(request -> cursoService.save(request)
                .doOnSuccess(curso -> log.info("Curso '{}' salvo com sucesso.", curso.getNome()))
                .doOnError(e -> log.error("Falha ao salvar curso '{}': {}", request.nome(), e.getMessage()))
                .subscribe()
        );
        log.info("Criação de cursos concluída.");
    }

    private void cargaCargos(Optional<Empresa> empresaParaVinculo) {
        log.info("Iniciando carga de Cargos...");
        Optional<Page<Cargo>> byFiltro = cargoService.findByFiltro("", pageableHelp.getPageable(0, 1, new ArrayList<>()));
        if (byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty()) {
            log.info("Cargos já cadastradas...");
            return;
        }
        // 5. Melhoria de robustez
        if (empresaParaVinculo.isEmpty()) {
            log.warn("Carga de cargos pulada: Nenhuma empresa de referência encontrada.");
            return;
        }
        var idEmpresa = empresaParaVinculo.get().getId();

        log.info("Criando 5 cargos de exemplo...");
        List<CargoRequest> cargosToCreate = List.of(
                CargoRequest.builder().nome("Professor(a)").descricao("Responsável por lecionar aulas.").ativo(true).idEmpresa(idEmpresa).build(),
                CargoRequest.builder().nome("Coordenador(a)").descricao("Gerencia equipe pedagógica.").ativo(true).idEmpresa(idEmpresa).build(),
                CargoRequest.builder().nome("Secretário(a)").descricao("Atendimento e organização administrativa.").ativo(true).idEmpresa(idEmpresa).build(),
                CargoRequest.builder().nome("Auxiliar de Limpeza").descricao("Manutenção da limpeza e organização do ambiente.").ativo(true).idEmpresa(idEmpresa).build(),
                CargoRequest.builder().nome("Diretor(a)").descricao("Gestão geral da instituição.").ativo(true).idEmpresa(idEmpresa).build()
        );

        cargosToCreate.forEach(request -> cargoService.save(request)
                .doOnSuccess(cargo -> log.info("Cargo '{}' salvo com sucesso.", cargo.getNome()))
                .doOnError(e -> log.error("Falha ao salvar cargo '{}': {}", request.nome(), e.getMessage()))
                .subscribe()
        );
        log.info("Criação de cargos concluída.");
    }

    private Optional<Empresa> getAnyExistingEmpresa() {
        log.info("Buscando uma empresa existente para referência...");
        Optional<Page<Empresa>> byFiltro = empresaService.findByFiltro("", pageableHelp.getPageable(0, 1, new ArrayList<>()));

        if (byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty()) {
            log.info("Empresa encontrada para referência.");
            return byFiltro.get().getContent().stream().findFirst();
        }
        log.warn("Nenhuma empresa existente encontrada.");
        return Optional.empty();
    }

    /**
     * Método auxiliar para buscar os primeiros 5 cursos de uma empresa.
     * Adapte este método caso a assinatura do seu service seja diferente.
     */
    private List<Curso> getExistingCursos(Long idEmpresa) {
        log.info("Buscando cursos existentes para a empresa ID: {}", idEmpresa);

        Optional<Page<Curso>> byFiltro = cursoService.findByFiltro("", idEmpresa, pageableHelp.getPageable(0, 5, new ArrayList<>()));

        if (byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty()) {
            return byFiltro.get().getContent();
        }
        log.warn("Nenhum curso existente encontrado para a empresa ID: {}", idEmpresa);
        return Collections.emptyList();
    }

    // /Users/rogeriocardoso/pessoal/escola/admin-service/src/main/java/com/escola/admin/service/auxiliar/impl/TurmaServiceImpl.java

    /**
     * Calcula a data de término de uma turma com base na sua data de início e na duração do curso associado.
     *
     * @param dataInicio A data de início da turma.
     * @param curso      O curso ao qual a turma pertence, contendo a duração.
     * @return A data de término calculada, ou null se os dados de entrada forem insuficientes.
     */
    private LocalDate calcularDataFim(LocalDate dataInicio, Curso curso) {
        if (dataInicio == null || curso == null || curso.getDuracaoValor() == null || curso.getDuracaoUnidade() == null) {
            log.warn("Não foi possível calcular a data final. Dados de entrada insuficientes.");
            return null; // Ou poderia retornar o próprio dataInicio, dependendo da regra de negócio.
        }

        Integer valor = curso.getDuracaoValor();
        String unidade = curso.getDuracaoUnidade().toLowerCase().trim();

        // Usamos um switch para tratar as diferentes unidades de tempo
        return switch (unidade) {
            case "ano", "anos" -> dataInicio.plusYears(valor);
            case "mes", "meses" -> dataInicio.plusMonths(valor);
            case "semana", "semanas" -> dataInicio.plusWeeks(valor);
            case "dia", "dias" -> dataInicio.plusDays(valor);
            default -> {
                log.warn("Unidade de duração '{}' não reconhecida. Não foi possível calcular a data final.", curso.getDuracaoUnidade());
                yield null; // Retorna null se a unidade for desconhecida
            }
        };
    }
}
