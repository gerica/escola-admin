package com.escola.admin.model.mapper;

import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.request.UsuarioRequest;
import com.escola.admin.model.response.UsuarioResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario entity);

    List<UsuarioResponse> toResponseList(List<Usuario> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    Usuario toEntity(UsuarioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    Usuario updateEntity(UsuarioRequest source, @MappingTarget Usuario target);
}
