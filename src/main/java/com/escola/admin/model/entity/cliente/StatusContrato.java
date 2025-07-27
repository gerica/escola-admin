package com.escola.admin.model.entity.cliente;

public enum StatusContrato {
    ATIVO("Ativo"),
    INATIVO("Inativo"),
    PENDENTE("Pendente"),
    CANCELADO("Cancelado"),
    CONCLUIDO("Concluído"),
    EM_NEGOCIACAO("Em Negociação");

    private final String descricao;

    StatusContrato(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}