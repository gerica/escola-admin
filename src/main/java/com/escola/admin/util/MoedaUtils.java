package com.escola.admin.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoedaUtils {

    public static String formatarParaReal(BigDecimal valor) {
        if (valor == null) {
            return "R$ 0,00";
        }

        // Define a localidade para o Brasil
        Locale localeBrasil = new Locale("pt", "BR");

        // Cria uma instância de NumberFormat para moedas
        NumberFormat nf = NumberFormat.getCurrencyInstance(localeBrasil);

        // Formata o valor e retorna a string
        return nf.format(valor);
    }
}