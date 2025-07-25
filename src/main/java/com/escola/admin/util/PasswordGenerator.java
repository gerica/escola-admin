package com.escola.admin.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Classe utilitária para gerar senhas aleatórias e seguras.
 * Utiliza SecureRandom para garantir uma forte aleatoriedade.
 */
public final class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;':,.<>/?~";
    private static final int DEFAULT_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
        // Classe utilitária não deve ser instanciada
    }

    /**
     * Gera uma senha com o comprimento padrão (12) e incluindo letras maiúsculas,
     * minúsculas, números e caracteres especiais.
     *
     * @return A senha gerada.
     */
    public static String generateDefaultPassword() {
        return generatePassword(DEFAULT_LENGTH, true, true, true, false);
    }

    /**
     * Gera uma senha aleatória com base nos parâmetros fornecidos.
     *
     * @param length     O comprimento da senha.
     * @param useUpper   Incluir letras maiúsculas.
     * @param useLower   Incluir letras minúsculas.
     * @param useDigits  Incluir números.
     * @param useSpecial Incluir caracteres especiais.
     * @return A senha gerada.
     */
    public static String generatePassword(int length, boolean useUpper, boolean useLower, boolean useDigits, boolean useSpecial) {
        if (length <= 0) {
            throw new IllegalArgumentException("O comprimento da senha deve ser positivo.");
        }

        // Constrói a lista de caracteres permitidos
        StringBuilder charPool = new StringBuilder();
        List<Character> requiredChars = new ArrayList<>();

        if (useUpper) {
            charPool.append(UPPER);
            requiredChars.add(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        }
        if (useLower) {
            charPool.append(LOWER);
            requiredChars.add(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        }
        if (useDigits) {
            charPool.append(DIGITS);
            requiredChars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        if (useSpecial) {
            charPool.append(SPECIAL);
            requiredChars.add(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));
        }

        if (charPool.length() == 0) {
            throw new IllegalArgumentException("Pelo menos um conjunto de caracteres deve ser selecionado.");
        }
        if (length < requiredChars.size()) {
            throw new IllegalArgumentException("O comprimento da senha é muito curto para incluir todos os tipos de caracteres necessários.");
        }

        // Gera os caracteres restantes da senha
        int remainingLength = length - requiredChars.size();
        IntStream.range(0, remainingLength).forEach(i -> {
            int randomIndex = RANDOM.nextInt(charPool.length());
            requiredChars.add(charPool.charAt(randomIndex));
        });

        // Embaralha a lista para garantir que os caracteres obrigatórios não fiquem no início
        Collections.shuffle(requiredChars, RANDOM);

        // Constrói a string final
        return requiredChars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}