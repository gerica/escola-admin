package com.escola.admin.model.entity.cliente;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
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
@Table(name = "tb_conta_receber")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContaReceber {

    @Id // Marca o campo como chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de valor para a chave primária (auto-incremento)
    Long id; // Usamos Long para IDs, pois Integer pode ser limitado para IDs de auto-incremento em sistemas maiores

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    @ToString.Exclude
    Contrato contrato;

    // 3. Adicionamos o campo de status
    @Enumerated(EnumType.STRING) // Mapeia o Enum pelo nome ("ABERTA", "PAGA", etc.), que é mais robusto
    @Column(name = "status", nullable = false)
    StatusContaReceber status;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    BigDecimal valorTotal; // BigDecimal é ideal para valores monetários

    @Column(name = "desconto", nullable = false, precision = 10, scale = 2)
    BigDecimal desconto; // BigDecimal é ideal para valores monetários

    @Column(name = "valor_pago", precision = 10, scale = 2)
    BigDecimal valorPago; // BigDecimal é ideal para valores monetários

    @Column(name = "data_vencimanto", nullable = false, updatable = false)
    LocalDate dataVencimento;

    @Column(name = "data_pagamento", updatable = false)
    LocalDate dataPagamento;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    String observacoes;

    // Campos de Auditoria - Boas práticas para rastreabilidade
    @CreationTimestamp // Preenche automaticamente com a data e hora de criação
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    LocalDateTime dataCadastro;

    @UpdateTimestamp // Preenche automaticamente com a data e hora da última atualização
    @Column(name = "data_atualizacao", nullable = false)
    LocalDateTime dataAtualizacao;

}