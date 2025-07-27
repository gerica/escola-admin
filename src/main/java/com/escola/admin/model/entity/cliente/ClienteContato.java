package com.escola.admin.model.entity.cliente;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_cliente_contato")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class ClienteContato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Usar IDENTITY para auto-incremento
    Long id; // Usamos Long para IDs, conforme boa prática

    @ManyToOne(fetch = FetchType.LAZY) // Mudado para LAZY para melhor performance
    @JoinColumn(name = "id_cliente", nullable = false)
    @JsonIgnore // Mantido para evitar loops de serialização
    Cliente cliente;

    @Column(name = "numero_contato", nullable = false, length = 20, unique = true) // Renomeado e adicionado restrições de nulidade e tamanho
    String numero;

    @Enumerated(EnumType.STRING) // Salva o nome do enum (ex: "CELULAR") no banco
    @Column(name = "tipo_contato", nullable = false, length = 20) // Adicionado tipo de contato (CELULAR, RESIDENCIAL, etc.)
    TipoContato tipoContato;

    @Column(name = "observacao", length = 255) // Campo para observações adicionais sobre o contato
    String observacao;

    // Campos de Auditoria (boas práticas)
    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    LocalDateTime dataCadastro;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    LocalDateTime dataAtualizacao;
}