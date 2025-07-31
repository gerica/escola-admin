package com.escola.admin.model.entity.auxiliar;

import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.ClienteDependente;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_matricula", uniqueConstraints = {
        // Restrição para garantir que um Cliente não seja matriculado mais de uma vez na mesma Turma
        @UniqueConstraint(columnNames = {"turma_id", "cliente_id"}, name = "uc_matricula_turma_cliente"),

        // Restrição para garantir que um ClienteDependente não seja matriculado mais de uma vez na mesma Turma
        @UniqueConstraint(columnNames = {"turma_id", "cliente_dependente_id"}, name = "uc_matricula_turma_dependente")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString // Adicionado para facilitar o debug
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id", nullable = false)
    @ToString.Exclude
    Turma turma; // A qual turma o aluno está matriculado

    @Column(nullable = false, unique = true) // (Opcional, mas útil para identificação interna, ex: "BLTINIA")
    String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true) // Pode ser nulo se for um dependente
    @ToString.Exclude
    Cliente cliente; // Se o aluno for o cliente principal

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_dependente_id", nullable = true) // Pode ser nulo se for o cliente principal
    @ToString.Exclude
    ClienteDependente clienteDependente; // Se o aluno for um dependente

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    StatusMatricula status; // Ex: ATIVA, INATIVA, CONCLUIDA, TRANCADA

    @Column(name = "observacoes", columnDefinition = "TEXT")
    String observacoes; // Campo para observações adicionais sobre a matrícula

    @Column(nullable = false, updatable = false)
    LocalDateTime dataCadastro; // Data e hora de criação do registro

    @Column(nullable = false)
    LocalDateTime dataAtualizacao; // Data e hora da última atualização do registro

    // Métodos de callback JPA para gerenciar datas de criação e atualização
    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
        validateStudentSource(); // Call validation on persist
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
        validateStudentSource(); // Call validation on update
    }

    // A validação é um método auxiliar agora, sem anotação de ciclo de vida própria
    // É chamada pelos métodos onCreate e onUpdate
    private void validateStudentSource() {
        if (cliente == null && clienteDependente == null) {
            throw new IllegalArgumentException("A matrícula deve estar associada a um Cliente ou a um ClienteDependente.");
        }
        if (cliente != null && clienteDependente != null) {
            throw new IllegalArgumentException("A matrícula não pode estar associada a um Cliente E a um ClienteDependente simultaneamente.");
        }
    }
}