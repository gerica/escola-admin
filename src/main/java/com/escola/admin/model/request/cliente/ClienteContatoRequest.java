package com.escola.admin.model.request.cliente;

import com.escola.admin.model.entity.cliente.TipoContato;

public record ClienteContatoRequest(
        Integer id,
        Integer idCliente,
        String numero,
        TipoContato tipoContato,
        String observacao
) {
}
