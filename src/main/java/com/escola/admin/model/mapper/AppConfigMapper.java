package com.escola.admin.model.mapper;


import com.escola.admin.model.response.AppConfigResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.boot.info.BuildProperties;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface AppConfigMapper {

    /**
     * Mapeia as propriedades de build e a descrição para o DTO de resposta.
     * O MapStruct usará o método 'instantToDate' abaixo automaticamente
     * para converter o campo 'time'.
     */
    @Mapping(source = "buildProperties.name", target = "name")
    @Mapping(source = "buildProperties.version", target = "version")
    @Mapping(source = "buildProperties.time", target = "time") // Mapeamento explícito para clareza
    @Mapping(source = "appDescription", target = "description")
    AppConfigResponse toOutput(BuildProperties buildProperties, String appDescription);

    /**
     * Converte um Instant para um OffsetDateTime no fuso horário UTC.
     * O MapStruct usará este método para o campo 'time'.
     * O serializador GraphQL 'DateTime' lida perfeitamente com este tipo.
     */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        // Converte o Instant para um OffsetDateTime com offset UTC (+00:00)
        return instant.atOffset(ZoneOffset.UTC);
    }
}
