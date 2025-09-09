package com.escola.admin.model.mapper;

import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.response.ParametroResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        // Ignora campos no destino que não foram mapeados a partir da fonte.
        // Útil para não gerar avisos sobre campos que só existem no DTO.
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ParametroMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "chave", target = "chave")
    @Mapping(source = "jsonData.valor", target = "valor")
    ParametroResponse toResponse(Parametro parametro);

    /**
     * Método auxiliar que ensina o MapStruct a converter um Object para String.
     * O MapStruct irá encontrar e usar este método automaticamente para os mapeamentos acima.
     *
     * @param value O objeto vindo do Map.
     * @return O objeto convertido para String, ou null se a entrada for nula.
     */
    default String mapObjectToString(Object value) {
        if (value == null) {
            return null;
        }
        // Uma abordagem mais robusta que um simples cast.
        // Garante que qualquer objeto seja convertido para sua representação em String.
        return value.toString();
    }
}