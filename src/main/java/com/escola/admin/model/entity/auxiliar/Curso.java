package com.escola.admin.model.entity.auxiliar;

import com.escola.admin.model.entity.Empresa;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_curso") // Nome da tabela no banco de dados
@FieldDefaults(level = AccessLevel.PRIVATE) // Campos privados por padrão
@AllArgsConstructor // Construtor com todos os argumentos
@NoArgsConstructor // Construtor sem argumentos
@Getter // Gerar getters para todos os campos
@Setter // Gerar setters para todos os campos
@Builder // Gerar o padrão de construção de objetos (Builder pattern)
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de ID (auto-incremento)
    Long id;

    @Column(nullable = false, unique = true) // Nome do curso é obrigatório e único
    String nome;

    @Column(nullable = false) // Descrição detalhada do curso
    String descricao;

    @Column(nullable = false) // Duração do curso (ex: "1 ano", "6 meses", "30 horas")
    String duracao;

    @Column(nullable = true) // Categoria do curso (ex: "Dança Clássica", "Educação Infantil", "Musicalização")
    String categoria;

    @Column(nullable = false) // Preço mensal ou total do curso
    Double valorMensalidade;

    @Column(nullable = false)
    @Builder.Default
    Boolean ativo = true; // Indica se o curso está ativo para matrículas (padrão: true)

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