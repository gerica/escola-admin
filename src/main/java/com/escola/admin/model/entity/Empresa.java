package com.escola.admin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_empresa") // Nome da tabela no banco de dados
@FieldDefaults(level = AccessLevel.PRIVATE) // Campos privados por padrão
@AllArgsConstructor // Construtor com todos os argumentos
@NoArgsConstructor // Construtor sem argumentos
@Getter // Gerar getters para todos os campos
@Setter // Gerar setters para todos os campos
@Builder // Gerar o padrão de construção de objetos (Builder pattern)
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de ID (auto-incremento)
    Long id;

    @Column(nullable = false, unique = true) // Nome fantasia é obrigatório e único
    String nomeFantasia;

    @Column(nullable = false, unique = true) // Razão social é obrigatória e única
    String razaoSocial;

    @Column(nullable = false, unique = true, length = 18) // CNPJ é obrigatório, único e tem tamanho fixo (XX.XXX.XXX/XXXX-XX)
    String cnpj;

    @Column(length = 20) // Inscrição Estadual (opcional para algumas empresas, como MEI)
    String inscricaoEstadual;

    @Column(length = 20) // Telefone de contato
    String telefone;

    @Column(nullable = false, unique = true) // E-mail é obrigatório e único
    String email;

    @Column(nullable = false) // Endereço é obrigatório
    String endereco;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "logo_id", referencedColumnName = "id")
    Logo logo;

    @Column(nullable = false)
    @Builder.Default
    Boolean ativo = true; // Indica se a empresa está ativa no sistema (padrão: true)

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