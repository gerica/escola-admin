package com.escola.admin.service.impl;

import com.escola.admin.service.PdfConverterService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class PdfConverterServiceImpl implements PdfConverterService {

    /**
     * Converte uma string HTML em um arquivo PDF e retorna o conteúdo como uma string Base64.
     *
     * @param htmlContent A string contendo o HTML a ser convertido.
     * @return Uma string Base64 que representa o arquivo PDF.
     */
    public String convertHtmlToPdfBase64(String htmlContent) throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String cleanedHtmlContent = htmlContent.replaceAll("&nbsp;", " ");

        String styledHtmlContent = injectCssIntoHtml(cleanedHtmlContent);
        try {
            // 1. Analisar a string HTML para um objeto Document usando Jsoup
            // Jsoup é tolerante com HTML incompleto e entidades como &nbsp;
            org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(styledHtmlContent);
            Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

            // 2. Usar o objeto Document no PdfRendererBuilder
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withW3cDocument(w3cDoc, "/"); // O segundo parâmetro é a base URI
            builder.toStream(os);

            // 3. Criar o PDF
            builder.run();

        } catch (Exception e) {
            log.error("Erro ao converter HTML para PDF: {}", e.getMessage(), e);
            throw new IOException("Erro ao converter HTML para PDF.", e);
        }

        // 4. Codificar o PDF para Base64
        byte[] pdfBytes = os.toByteArray();
        return Base64.getEncoder().encodeToString(pdfBytes);
    }

    // O método injectCssIntoHtml que usamos anteriormente
    private String injectCssIntoHtml(String htmlContent) {
        String styleBlock = getStyleBlock();
        if (htmlContent.contains("</head>")) {
            return htmlContent.replaceFirst("</head>", styleBlock + "</head>");
        }
        if (htmlContent.contains("<body>")) {
            return htmlContent.replaceFirst("<body>", "<head>" + styleBlock + "</head><body>");
        }
        return "<html><head>" + styleBlock + "</head><body> " + htmlContent + " </body></html>";
    }

    private  String getStyleBlock() {
        String cssContent = """
                 body {
                    margin: 30px;
                    font-family: sans-serif;
                }
                .ql-align-center {
                    margin: 0 auto;
                    text-align: center;
                }
                .ql-align-right {
                    text-align: right;
                }

                """;
        // Cria a tag <style> com o conteúdo CSS
        return "<style>" + cssContent + "</style>";
    }
}
