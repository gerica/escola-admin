package com.escola.admin.model.entity.auxiliar; // Ajuste o pacote conforme a estrutura do seu projeto

import com.escola.admin.model.entity.Empresa;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "tb_turma") // Nome da tabela no banco de dados
@FieldDefaults(level = AccessLevel.PRIVATE) // Campos privados por padrão
@AllArgsConstructor // Construtor com todos os argumentos
@NoArgsConstructor // Construtor sem argumentos
@Getter // Gerar getters para todos os campos
@Setter // Gerar setters para todos os campos
@Builder // Gerar o padrão de construção de objetos (Builder pattern)
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de ID (auto-incremento)
    Long id;

    @Column(nullable = false, unique = true) // Nome da turma (ex: "Ballet Clássico Iniciante - Turma A")
    String nome;

    @Column(nullable = false, unique = true) // (Opcional, mas útil para identificação interna, ex: "BLTINIA")
    String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false) // A turma está associada a um curso
    @ToString.Exclude
    Curso curso; // Referência à classe Curso

    @Column(nullable = false) // Capacidade máxima de alunos na turma
    Integer capacidadeMaxima;

    @Column(nullable = false) // Indica a situação da turma (ex: "Ativa", "Inativa", "Lotada", "Em Formação")
    @Enumerated(EnumType.STRING) // Armazena o enum como String no banco de dados
    StatusTurma status; // Usaremos um enum para o status da turma

    @Column(nullable = false) // Ano ou período letivo (ex: "2025", "2025/1", "2º Semestre 2025")
    String anoPeriodo;

    @Column(nullable = false) // Horário de início da aula/atividade (apenas hora e minuto)
    LocalTime horarioInicio;

    @Column(nullable = false) // Horário de término da aula/atividade (apenas hora e minuto)
    LocalTime horarioFim;

    @ElementCollection(targetClass = DayOfWeek.class)
    @CollectionTable(name = "tb_turma_dias_semana", joinColumns = @JoinColumn(name = "turma_id"))
    @Column(name = "dia_semana", nullable = false)
    @Enumerated(EnumType.STRING) // Armazena o enum como String no banco de dados
    Set<DayOfWeek> diasDaSemana; // Conjunto de dias da semana (ex: SEGUNDA, QUARTA, SEXTA)

    @Column
    String professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @ToString.Exclude
    Empresa empresa;

    @Column(nullable = false, updatable = false)
    LocalDateTime dataCadastro; // Data e hora de criação do registro

    @Column(nullable = false)
    LocalDateTime dataAtualizacao; // Data e hora da última atualização do registro

    // Métodos de callback JPA para gerenciar datas de criação e atualização
    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}