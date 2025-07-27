package com.escola.admin.model.mapper.auxiliar;

import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.request.auxiliar.CursoRequest;
import com.escola.admin.model.response.auxiliar.CursoResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface CursoMapper {

    CursoResponse toResponse(Curso entity);

    List<CursoResponse> toResponseList(List<Curso> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    Curso toEntity(CursoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    Curso updateEntity(CursoRequest source, @MappingTarget Curso target);

}
