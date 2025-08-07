package com.escola.admin.model.entity.cliente;

public enum StatusContaReceber {
    ABERTA,   // A conta foi criada e aguarda pagamento
    PAGA,     // A conta foi quitada
    VENCIDA,  // A data de vencimento passou e não foi paga
    CANCELADA // A conta foi cancelada por algum motivo
}