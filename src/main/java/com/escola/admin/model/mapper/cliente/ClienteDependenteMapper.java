package com.escola.admin.model.mapper.cliente;

import com.escola.admin.model.entity.cliente.ClienteDependente;
import com.escola.admin.model.request.cliente.ClienteDependenteRequest;
import com.escola.admin.model.response.cliente.ClienteDependenteResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ClienteDependenteMapper {

    @Mapping(target = "parentescoDescricao", source = "parentesco.descricao")
    ClienteDependenteResponse toResponse(ClienteDependente entity);

    List<ClienteDependenteResponse> toResponseList(List<ClienteDependente> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    ClienteDependente toEntity(ClienteDependenteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    ClienteDependente updateEntity(ClienteDependenteRequest source, @MappingTarget ClienteDependente target);
}
