package com.escola.admin.model.mapper.cliente;


import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.model.response.cliente.ContratoResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING, // Integra com o Spring para injeção de dependência
        unmappedTargetPolicy = ReportingPolicy.WARN // Ignora campos que não têm mapeamento explícito
)
public interface ContratoMapper {

    /**
     * Mapeia um ContratoRequest para uma entidade Contrato.
     * Deve-se buscar e atribuir a entidade Cliente separadamente no serviço.
     *
     * @param request O objeto ContratoRequest a ser mapeado.
     * @return A entidade Contrato mapeada.
     */
//    @Mapping(target = "idContrato", ignore = true) // ID é gerado pelo banco, não vem do request
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "turma", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Contrato toEntity(ContratoRequest request);

    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "turma", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Contrato updateEntity(ContratoRequest source, @MappingTarget Contrato target);

    /**
     * Mapeia uma entidade Contrato para um ContratoResponse.
     *
     * @param contrato A entidade Contrato a ser mapeada.
     * @return O objeto ContratoResponse mapeado.
     */
    @Mapping(source = "cliente.id", target = "idCliente") // Mapeia o ID do cliente da entidade para o response
    @Mapping(source = "cliente.nome", target = "nomeCliente")
    // Mapeia o ID do cliente da entidade para o response
    // Mapeia o nome do cliente da entidade para o response
    ContratoResponse toResponse(Contrato contrato);

    /**
     * Mapeia uma lista de entidades Contrato para uma lista de ContratoResponse.
     *
     * @param contratos A lista de entidades Contrato.
     * @return A lista de ContratoResponse mapeada.
     */
    List<ContratoResponse> toResponseList(List<Contrato> contratos);
}