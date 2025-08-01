package com.escola.admin.model.response.auxiliar;

import com.escola.admin.model.entity.auxiliar.StatusTurma;
import com.escola.admin.model.response.EmpresaResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Builder
public record TurmaResponse(
        Long id,
        CursoResponse curso, // Referência à classe Curso
        EmpresaResponse empresa,
        String nome,
        String codigo,
        Integer capacidadeMaxima,
        StatusTurma status, // Usaremos um enum para o status da turma
        String anoPeriodo,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horarioInicio,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horarioFim,
        Set<DayOfWeek> diasDaSemana, // Conjunto de dias da semana (ex: SEGUNDA, QUARTA, SEXTA)
        String professor,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataInicio,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataFim
) {
}
