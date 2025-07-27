package com.escola.admin.model.mapper.cliente;

import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.request.cliente.ClienteRequest;
import com.escola.admin.model.response.cliente.ClienteResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ClienteMapper {

    @Named("formatCPF")
    static String formatCpf(String cpf) {
        // Verifica se o CPF é nulo ou não tem 11 dígitos
        if (cpf == null || cpf.length() != 11) {
            return cpf; // Retorna o valor original se inválido
        }
        // Aplica a máscara XXX.XXX.XXX-XX
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    @Mapping(target = "cidadeDesc", source = "cidade")
    ClienteResponse toResponse(Cliente entity);

    List<ClienteResponse> toResponseList(List<Cliente> empresas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docCPF", source = "docCPF", qualifiedByName = "formatCPF")
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "contatos", ignore = true)
    @Mapping(target = "dependentes", ignore = true)
    @Mapping(target = "contratos", ignore = true)
    Cliente toEntity(ClienteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "contatos", ignore = true)
    @Mapping(target = "dependentes", ignore = true)
    @Mapping(target = "contratos", ignore = true)
    Cliente updateEntity(ClienteRequest source, @MappingTarget Cliente target);
}
