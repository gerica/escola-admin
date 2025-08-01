package com.escola.admin.model.request.auxiliar;

import com.escola.admin.model.entity.auxiliar.StatusTurma;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Builder
public record TurmaRequest(
        Long id,
        Long idCurso, // Referência à classe Curso
        Long idEmpresa,
        String nome,
        String codigo,
        Integer capacidadeMaxima,
        StatusTurma status, // Usaremos um enum para o status da turma
        String anoPeriodo,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        Set<DayOfWeek> diasDaSemana, // Conjunto de dias da semana (ex: SEGUNDA, QUARTA, SEXTA)
        String professor,
        LocalDate dataInicio,
        LocalDate dataFim
) {
}
