package com.escola.admin.model.entity.cliente;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoParentesco {

    CONJUGE("Cônjuge"),
    COMPANHEIRO("Companheiro(a)"),
    FILHO("Filho"),
    FILHA("Filha"),
    ENTEADO("Enteado"),
    ENTEADA("Enteada"),
    PAI("Pai"),
    MAE("Mãe"),
    IRMAO("Irmão"),
    IRMA("Irmã"),
    AVO("Avô"),
    AVO_FEM("Avó"),
    NETO("Neto"),
    NETA("Neta"),
    TUTORADO("Tutorado(a)"),
    OUTRO("Outro");

    private final String descricao;
}