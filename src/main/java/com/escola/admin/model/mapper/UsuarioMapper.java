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

    @Mapping(target = "password", ignore = true)
    UsuarioResponse toResponse(Usuario entity);

    List<UsuarioResponse> toResponseList(List<Usuario> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "precisaAlterarSenha", ignore = true)
    Usuario toEntity(UsuarioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "precisaAlterarSenha", ignore = true)
    Usuario updateEntity(UsuarioRequest source, @MappingTarget Usuario target);

    // --- Se o problema persistir, considere este método auxiliar para debug ---
//    default UsuarioResponse mapUsuarioToResponseDebug(Usuario entity) {
//        System.out.println("DEBUG: Dentro do mapper manual - entity.enabled: " + entity.isEnabled());
//        UsuarioResponse response = new UsuarioResponse(
//                entity.getId(),
//                entity.getUsername(),
//                null, // password ignored
//                entity.getFirstname(),
//                entity.getLastname(),
//                entity.getEmail(),
//                entity.isEnabled(), // Use o valor direto do entity
//                null, // Empresa será mapeada separadamente ou null para teste
//                entity.getRoles()
//        );
//        System.out.println("DEBUG: Dentro do mapper manual - response.enabled: " + response.enabled());
//        return response;
//    }

}
