package com.escola.admin.service.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TipoArquivoEnum {

    PDF(1, "pdf"), //
    ODS(2, "ods"), //
    ;

    private final String descricao;
    private final Integer id;

    TipoArquivoEnum(Integer id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    @JsonIgnore
    public static TipoArquivoEnum fromId(Integer value) {
        for (TipoArquivoEnum tipo : values()) {
            if (tipo.id.equals(value)) {
                return tipo;
            }
        }

        throw new IllegalArgumentException(
                "Tipo de enum desonhecido " + value + ", Allowed values are " + Arrays.toString(values()));
    }

    @JsonCreator
    public static TipoArquivoEnum fromValue(String valueEnum) {
        if (valueEnum != null) {
            for (TipoArquivoEnum tipo : values()) {
                if (tipo.getDescricao().equalsIgnoreCase(valueEnum.toLowerCase())) {
                    return tipo;
                }
            }
        }
        throw new IllegalArgumentException(
                "Tipo de enum desonhecido " + valueEnum + ", Allowed values are " + Arrays.toString(values()));
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    public Integer getId() {
        return id;
    }
}
