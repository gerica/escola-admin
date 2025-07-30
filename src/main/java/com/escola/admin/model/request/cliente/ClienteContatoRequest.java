package com.escola.admin.model.request.cliente;

import com.escola.admin.model.entity.cliente.TipoContato;

public record ClienteContatoRequest(
        Long id,
        Long idCliente,
        String numero,
        TipoContato tipoContato,
        String observacao
) {
}
