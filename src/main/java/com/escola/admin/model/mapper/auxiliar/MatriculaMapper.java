package com.escola.admin.model.mapper.auxiliar;

import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.mapper.cliente.ClienteDependenteMapper;
import com.escola.admin.model.mapper.cliente.ClienteMapper;
import com.escola.admin.model.request.auxiliar.MatriculaRequest;
import com.escola.admin.model.response.auxiliar.MatriculaResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN,
        uses = {ClienteMapper.class, ClienteDependenteMapper.class}
)
public interface MatriculaMapper {

    @Mapping(target = "cliente.cidadeDesc", source = "cliente.cidade")
    MatriculaResponse toResponse(Matricula entity);

    List<MatriculaResponse> toResponseList(List<Matricula> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "turma", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "clienteDependente", ignore = true)
    Matricula toEntity(MatriculaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "turma", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "clienteDependente", ignore = true)
    Matricula updateEntity(MatriculaRequest source, @MappingTarget Matricula target);

}
