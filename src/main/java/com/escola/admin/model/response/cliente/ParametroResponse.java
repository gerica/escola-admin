package com.escola.admin.model.response.cliente;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Esta anotação é uma ótima prática: garante que campos nulos não sejam enviados no JSON de resposta.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParametroResponse {

    private Integer id;
    private String chave;
    private String codigoMunicipio;
    private String modeloContrato;
}