package com.escola.admin.model.mapper.cliente;

import com.escola.admin.model.entity.cliente.Anexo;
import com.escola.admin.model.response.cliente.AnexoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AnexoMapper {

    AnexoResponse toResponse(Anexo entity);

    List<AnexoResponse> toResponseList(List<Anexo> entities);

//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "dataCadastro", ignore = true)
//    @Mapping(target = "dataAtualizacao", ignore = true)
//    @Mapping(target = "empresa", ignore = true)
//    Anexo toEntity(AnexoRequest request);
//
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "dataCadastro", ignore = true)
//    @Mapping(target = "dataAtualizacao", ignore = true)
//    @Mapping(target = "empresa", ignore = true)Ø
//    Anexo updateEntity(AnexoRequest source, @MappingTarget Anexo target);

}
