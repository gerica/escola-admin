package com.escola.admin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "tb_parametro")
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Parametro {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    @Column(name = "DS_CHAVE")
    String chave;

    /**
     * Mapeamento moderno para JSONB com Hibernate 6+.
     * 1. @JdbcTypeCode(SqlTypes.JSON) informa ao Hibernate para usar seu manipulador de tipo JSON.
     * A biblioteca Hypersistence no classpath aprimora esse manipulador para ser mais eficiente.
     * 2. @Column(columnDefinition = "jsonb") garante que, ao gerar o DDL, a coluna seja criada
     * como o tipo 'jsonb' nativo e otimizado do PostgreSQL.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "JSON_DATA", columnDefinition = "jsonb")
    private Map<String, Object> jsonData;

}