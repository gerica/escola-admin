package com.escola.admin.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtils {

    /**
     * Calcula o hash SHA-256 de uma string Base64.
     *
     * @param base64String A string em formato Base64.
     * @return O hash em formato hexadecimal.
     */
    public static String calculateSha256Hash(String base64String) {
        if (base64String == null || base64String.isBlank()) {
            return null;
        }

        try {

            // 1. Verifique e remova o prefixo Data URI
            if (base64String.contains(",")) {
                base64String = base64String.substring(base64String.indexOf(",") + 1);
            }

            // 1. Decodifica a string Base64 para um array de bytes
            byte[] fileBytes = Base64.getDecoder().decode(base64String);

            // 2. Obtém a instância do algoritmo SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 3. Calcula o hash
            byte[] hashBytes = digest.digest(fileBytes);

            // 4. Converte o array de bytes do hash para uma representação hexadecimal
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            // Trata a exceção caso o algoritmo não seja encontrado ou a string Base64 seja inválida
            throw new RuntimeException("Erro ao calcular o hash da string Base64.", e);
        }
    }

    /**
     * Converte um array de bytes em uma string hexadecimal.
     *
     * @param bytes O array de bytes.
     * @return A string hexadecimal.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}