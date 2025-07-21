package com.escola.admin.model.entity;

public enum Role {
    SUPER_ADMIN,    // Administrador do sistema ERP (seu time)
    ADMIN_EMPRESA,  // Administrador de uma empresa específica (o cliente final)
    COORDENADOR,
    PROFESSOR,
    FINANCEIRO,
    RECEPCIONISTA   // Ou SECRETARIA, ou ATENDENTE, dependendo do termo mais comum para o cliente
}