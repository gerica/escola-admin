package com.escola.admin.model.entity.cliente;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id") // 2. Equals e HashCode baseados apenas no ID (mais seguro para entidades)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_cliente_dependente")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
// makeFinal = false é o padrão para Lombok, mas bom explicitar
public class ClienteDependente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Usar IDENTITY para auto-incremento
    Long id; // Usamos Long para IDs, conforme boa prática

    @ManyToOne(fetch = FetchType.LAZY) // Mudado para LAZY para melhor performance em muitos casos
    @JoinColumn(name = "id_cliente", nullable = false)
    @JsonIgnore // Mantido para evitar loops de serialização
    Cliente cliente;

    @Column(name = "nome_dependente", nullable = false, length = 255) // Renomeado e adicionado restrições
    String nome;

    @Column(name = "data_nascimento", nullable = false)
    LocalDate dataNascimento;

    @Enumerated(EnumType.STRING) // Garante que o nome do enum seja salvo
    @Column(name = "sexo", length = 10, nullable = false) // Sexo deve ser obrigatório
    Sexo sexo;

    @Column(name = "doc_cpf", length = 14, unique = true) // Pode ser null se dependente não tiver CPF, ou not null se for regra
    String docCPF;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentesco", length = 20, nullable = false) // Parentesco é obrigatório
    TipoParentesco parentesco; // Novo campo usando enum

    // Campos de Auditoria (boas práticas)
    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    LocalDateTime dataCadastro;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    LocalDateTime dataAtualizacao;
}