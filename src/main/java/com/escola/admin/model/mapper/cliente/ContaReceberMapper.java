package com.escola.admin.model.mapper.cliente;

import com.escola.admin.model.entity.cliente.ContaReceber;
import com.escola.admin.model.request.cliente.ContaReceberRequest;
import com.escola.admin.model.response.cliente.ContaReceberResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ContaReceberMapper {

    ContaReceberResponse toResponse(ContaReceber entity);

    List<ContaReceberResponse> toResponseList(List<ContaReceber> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "contrato", ignore = true)
    ContaReceber toEntity(ContaReceberRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "contrato", ignore = true)
    ContaReceber updateEntity(ContaReceberRequest source, @MappingTarget ContaReceber target);

}
