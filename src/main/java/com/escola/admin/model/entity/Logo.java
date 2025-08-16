package com.escola.admin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "tb_logo")
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Logo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "uuid", unique = true) // Nome do arquivo
    String uuid;

    @Column(name = "mime_type")
    String mimeType;

    @Column(name = "hash", unique = true) // Hash para evitar duplicação
    String hash;

    // Relacionamento unidirecional de volta.
    // A coluna 'empresa_id' será criada na tabela tb_logo.
    @OneToOne
    @JoinColumn(name = "empresa_id", referencedColumnName = "id")
    Empresa empresa;
}