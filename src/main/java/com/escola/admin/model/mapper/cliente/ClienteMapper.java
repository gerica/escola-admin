package com.escola.admin.model.mapper.cliente;

import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.request.cliente.ClienteRequest;
import com.escola.admin.model.response.cliente.ClienteResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN,
        uses = {ClienteDependenteMapper.class} // Make sure this is present and correct
)
public interface ClienteMapper {

    @Named("formatCPF")
    static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    @Named("toClienteResponseWithoutDependents")
    @Mapping(target = "cidadeDesc", source = "cidade")
    @Mapping(target = "dependentes", ignore = true)
    ClienteResponse toResponse(Cliente entity);

    @Named("toClienteResponseWithDependents")
    @Mapping(target = "cidadeDesc", source = "cidade")
    ClienteResponse toResponseComDependentes(Cliente entity);

    // If you want a list of clients WITHOUT dependents:
    @IterableMapping(qualifiedByName = "toClienteResponseWithoutDependents")
    List<ClienteResponse> toResponseList(List<Cliente> clientes);

    // If you want a separate method for a list of clients WITH dependents:
    @IterableMapping(qualifiedByName = "toClienteResponseWithDependents")
    List<ClienteResponse> toResponseListWithDependents(List<Cliente> clientes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docCPF", source = "docCPF", qualifiedByName = "formatCPF")
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "contatos", ignore = true)
    @Mapping(target = "dependentes", ignore = true)
    @Mapping(target = "contratos", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    Cliente toEntity(ClienteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "contatos", ignore = true)
    @Mapping(target = "dependentes", ignore = true)
    @Mapping(target = "contratos", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    Cliente updateEntity(ClienteRequest source, @MappingTarget Cliente target);
}