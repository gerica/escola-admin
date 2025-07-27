package com.escola.admin.model.mapper.cliente;

import com.escola.admin.model.entity.cliente.ClienteContato;
import com.escola.admin.model.request.cliente.ClienteContatoRequest;
import com.escola.admin.model.response.cliente.ClienteContatoResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ClienteContatoMapper {

    ClienteContatoResponse toResponse(ClienteContato entity);

    List<ClienteContatoResponse> toResponseList(List<ClienteContato> empresas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    ClienteContato toEntity(ClienteContatoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    ClienteContato updateEntity(ClienteContatoRequest source, @MappingTarget ClienteContato target);
}
