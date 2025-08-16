package com.escola.admin.config.init;

import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.service.EmpresaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmpresaInitializer {

    private static final int NUM_EMPRESAS_TO_CREATE = 2;
    EmpresaService service;
    private final Random random = new Random();
    PageableHelp pageableHelp;

    void carga() {
        log.info("<INIT> Carga empresa");
        this.criarEmpresasDeTeste();
        log.info("<END> Carga empresa");
    }

    void criarEmpresasDeTeste() {
        Optional<Page<Empresa>> byFiltro = service.findByFiltro("", pageableHelp.getPageable(0, 1, new ArrayList<>()));
        if (byFiltro.isPresent() && !byFiltro.get().getContent().isEmpty()) {
            log.info("Empresas '{}' já existes, pulando criação.", byFiltro.get().getSize());
            return;
        }

        for (int i = 1; i <= NUM_EMPRESAS_TO_CREATE; i++) {
            String nomeFantasia = "Empresa Teste " + i;
            // Verifica se a empresa já existe para evitar duplicatas em reinícios do app


            log.info("Criando empresa: {}", nomeFantasia);

            String cnpjBase = generateRandomDigits(8); // Gera 12 dígitos para o CNPJ base
            String filial = String.format("%04d", i % 10000); // Gera 4 dígitos para a filial
            String digito = String.format("%02d", random.nextInt(100)); // Gera 2 dígitos para o dígito verificador
            String rawCnpj = cnpjBase + filial + digito; // CNPJ com 18 dígitos formatados

            // Certifique-se de que o método formatCnpj esteja acessível,
            // idealmente como um método estático na classe Empresa ou em um utilitário.
            // Se o CNPJ no banco for armazenado formatado, use o formatCnpj.
            // Se for armazenado sem formatação, remova a chamada ao formatCnpj.
            String formattedCnpj = formatCnpjForInitializer(rawCnpj); // Usa um método auxiliar para formatação

            // URL da imagem a ser convertida
            String imageUrl = "https://placehold.co/150x50/000/FFF?text=Logo" + i;
            String logoBase64 = null;
            try {
                // Chama o novo método para converter a imagem para Base64
                logoBase64 = convertImageUrlToBase64(imageUrl);
            } catch (IOException e) {
                log.error("Erro ao converter imagem da URL '{}' para Base64: {}", imageUrl, e.getMessage());
                // Continua a criação da empresa, mas com a logo nula
            }

            EmpresaRequest request = new EmpresaRequest(
                    null,
                    nomeFantasia,
                    "Razao Social da Empresa " + i + " Ltda.",
                    formattedCnpj,
                    "IE" + String.format("%09d", i),
                    String.format("%02d", random.nextInt(99)) + "9" + String.format("%08d", random.nextInt(100000000)),
                    "contato" + i + "@empresateste.com",
                    "Rua das Flores, " + i + ", Centro, Cidade Teste, Estado Teste",
                    logoBase64,
                    "image/svg+xml",
                    true);

            try {
                service.save(request).block();
                log.info(">>> Empresa '{}' criada com sucesso.", nomeFantasia);
            } catch (Exception e) {
                log.error("Erro ao salvar a empresa '{}': {}", nomeFantasia, e.getMessage());
                // Em um ambiente real, você pode querer lançar uma exceção
                // ou ter uma estratégia de retry.
            }
        }
    }

    /**
     * Converte uma imagem de uma URL para uma string Base64.
     *
     * @param imageUrl A URL da imagem.
     * @return A string Base64 da imagem, incluindo o prefixo `data:image/...;base64,`.
     * @throws IOException se houver um erro ao ler a imagem.
     */
    private String convertImageUrlToBase64(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream is = url.openStream();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = os.toByteArray();
            String base64String = Base64.getEncoder().encodeToString(imageBytes);

            // Para garantir que o frontend renderize corretamente, adicione o prefixo
            // 'data:image/jpeg;base64,' ou 'data:image/png;base64,'.
            // Como o placehold.co usa PNG por padrão para texto, vamos usar PNG.
            return "data:image/png;base64," + base64String;
        }
    }

    // Método auxiliar para gerar dígitos aleatórios
    private String generateRandomDigits(int count) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < count; k++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    // Método de formatação de CNPJ adaptado para este inicializador
    // Este método é uma cópia do que você tem, para garantir que o CNPJ seja salvo formatado.
    // Em um projeto real, você teria uma classe Utilitária para isso.
    private String formatCnpjForInitializer(String cnpj) {
        if (cnpj != null) {
            cnpj = cnpj.replaceAll("[^0-9]", "");
        }

        if (cnpj == null || cnpj.length() != 14) {
            return cnpj; // Retorna o valor original se inválido
        }
        // Aplica a máscara XX.XXX.XXX/XXXX-XX
        return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }
}