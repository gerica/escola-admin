package com.escola.admin.model.mapper.auxiliar;

import com.escola.admin.model.entity.auxiliar.Turma;
import com.escola.admin.model.request.auxiliar.TurmaRequest;
import com.escola.admin.model.response.auxiliar.TurmaResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface TurmaMapper {

    @Mapping(target = "empresa.logoBase64", ignore = true)
    @Mapping(target = "curso.empresa.logoBase64", ignore = true)
    TurmaResponse toResponse(Turma entity);

    List<TurmaResponse> toResponseList(List<Turma> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "matriculas", ignore = true)
    Turma toEntity(TurmaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "matriculas", ignore = true)
    Turma updateEntity(TurmaRequest source, @MappingTarget Turma target);

}
