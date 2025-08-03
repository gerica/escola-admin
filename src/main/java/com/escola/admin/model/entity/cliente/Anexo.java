package com.escola.admin.model.entity.cliente;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data // Gera getters, setters, toString, equals e hashCode
@Builder // Gera construtor de builder para criação fluente de objetos
@NoArgsConstructor // Gera construtor sem argumentos
@AllArgsConstructor // Gera construtor com todos os argumentos
@Entity // Marca a classe como uma entidade JPA
@Table(name = "tb_anexo") // Mapeia a classe para a tabela "tb_cliente" no banco de dados
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false) // Todos os campos privados e não finais
public class Anexo {

    @Id // Marca o campo como chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de valor para a chave primária (auto-incremento)
    Long id; // Usamos Long para IDs, pois Integer pode ser limitado para IDs de auto-incremento em sistemas maiores

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "uuid_arquivo", nullable = false)
    private String uuid;

    // Campos de Auditoria - Boas práticas para rastreabilidade
    @CreationTimestamp // Preenche automaticamente com a data e hora de criação
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    LocalDateTime dataCadastro;

    @UpdateTimestamp // Preenche automaticamente com a data e hora da última atualização
    @Column(name = "data_atualizacao", nullable = false)
    LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    @ToString.Exclude
    Contrato contrato;

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