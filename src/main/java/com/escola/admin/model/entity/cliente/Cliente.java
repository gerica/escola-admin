package com.escola.admin.model.entity.cliente;

import com.escola.admin.model.entity.Empresa;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data // Gera getters, setters, toString, equals e hashCode
@Builder // Gera construtor de builder para criação fluente de objetos
@NoArgsConstructor // Gera construtor sem argumentos
@AllArgsConstructor // Gera construtor com todos os argumentos
@Entity // Marca a classe como uma entidade JPA
@Table(name = "tb_cliente") // Mapeia a classe para a tabela "tb_cliente" no banco de dados
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false) // Todos os campos privados e não finais
public class Cliente {

    @Id // Marca o campo como chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de valor para a chave primária (auto-incremento)
    Long id; // Usamos Long para IDs, pois Integer pode ser limitado para IDs de auto-incremento em sistemas maiores

    @Column(name = "nome_cliente", nullable = false, length = 255)
    String nome;

    @Column(name = "data_nascimento", nullable = false)
    LocalDate dataNascimento;

    @Column(name = "cidade", length = 100)
    String cidade;

    @Column(name = "uf", length = 2) // UF geralmente tem 2 caracteres (e.g., "GO", "SP")
    String uf;

    @Column(name = "codigo_cidade", length = 20) // Ajuste o tamanho conforme o padrão do código da cidade
    String codigoCidade;

    @Column(name = "doc_cpf", length = 14, unique = true, nullable = false) // CPF deve ser único e não nulo
    String docCPF;

    @Column(name = "doc_rg", length = 20, unique = true) // RG também pode ser único, dependendo da regra de negócio
    String docRG;

    @Column(name = "endereco", columnDefinition = "TEXT") // Mapeia para um tipo TEXT no banco de dados
    String endereco;

    @Column(name = "email", length = 255, unique = true) // Email deve ser único
    String email;

    @Column(name = "profissao", length = 100)
    String profissao;

    @Column(name = "local_trabalho", length = 255)
    String localTrabalho;

    // Usaremos um enum para o status do cliente, similar ao status do contrato
    @Enumerated(EnumType.STRING)
    @Column(name = "status_cliente", nullable = false, length = 50)
    StatusCliente statusCliente; // Agora é um enum

    // Campos de Auditoria - Boas práticas para rastreabilidade
    @CreationTimestamp // Preenche automaticamente com a data e hora de criação
    @Column(name = "data_cadastro", nullable = false, updatable = false) // Não pode ser atualizado após a criação
            LocalDateTime dataCadastro;

    @UpdateTimestamp // Preenche automaticamente com a data e hora da última atualização
    @Column(name = "data_atualizacao", nullable = false)
    LocalDateTime dataAtualizacao;

    // Relacionamentos One-to-Many existentes
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "cliente", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    List<ClienteContato> contatos;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "cliente", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    List<ClienteDependente> dependentes;

    // NOVO: Relacionamento com Contratos
    // Um cliente pode ter muitos contratos
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "cliente", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JsonIgnore // Evita serialização recursiva em JSON
    @ToString.Exclude
    List<Contrato> contratos; // Referência à lista de contratos deste cliente

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @ToString.Exclude
    Empresa empresa;
}