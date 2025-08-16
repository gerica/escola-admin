package com.escola.admin.model.mapper.auxiliar;

import com.escola.admin.model.entity.auxiliar.Cargo;
import com.escola.admin.model.request.auxiliar.CargoRequest;
import com.escola.admin.model.response.auxiliar.CargoResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface CargoMapper {

    @Mapping(target = "empresa.logo", ignore = true)
    CargoResponse toResponse(Cargo entity);

    List<CargoResponse> toResponseList(List<Cargo> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    Cargo toEntity(CargoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    Cargo updateEntity(CargoRequest source, @MappingTarget Cargo target);

}
