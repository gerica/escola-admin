package com.escola.admin.model.mapper;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.model.response.EmpresaResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface EmpresaMapper {

    @Named("formatCNPJ")
    static String formatCnpj(String cnpj) {
        // Remove todos os caracteres não numéricos
        if (cnpj != null) {
            cnpj = cnpj.replaceAll("[^0-9]", "");
        }

        // Verifica se o CNPJ é nulo ou não tem 14 dígitos
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj; // Retorna o valor original se inválido
        }
        // Aplica a máscara XX.XXX.XXX/XXXX-XX
        return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    EmpresaResponse toResponse(Empresa entity);

    List<EmpresaResponse> toResponseList(List<Empresa> empresas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "formatCNPJ")
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Empresa toEntity(EmpresaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Empresa updateEntity(EmpresaRequest source, @MappingTarget Empresa target);

}
