package com.escola.admin.service.cliente.impl;


import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.service.cliente.ArtificalInteligenceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("parseLocal")
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class ParseContratoServiceImpl implements ArtificalInteligenceService {
    String preencherContrato(String template, Map<String, Object> entidades) {
        Pattern pattern = Pattern.compile("&lt;([a-zA-Z]+):([a-zA-Z0-9_]+)&gt;");
        Matcher matcher = pattern.matcher(template);
        StringBuffer resultado = new StringBuffer();

        while (matcher.find()) {
            String prefixo = matcher.group(1);   // Ex: "c"
            String atributo = matcher.group(2); // Ex: "nome"

            Object valorEntidade = entidades.get(prefixo);
            String valor;

            if (valorEntidade == null) {
                valor = "<indisponível>";
            } else if (isSimples(valorEntidade)) {
                // Se for valor direto (ex: String ou LocalDate), ignora o atributo
                valor = valorEntidade.toString();
            } else {
                valor = recuperarValor(valorEntidade, atributo);
            }

            matcher.appendReplacement(resultado, Matcher.quoteReplacement(valor));
        }

        matcher.appendTail(resultado);
        return resultado.toString();
    }

    String recuperarValor(Object entidade, String atributo) {
        try {
            Field field = entidade.getClass().getDeclaredField(atributo);
            field.setAccessible(true);
            Object valor = field.get(entidade);
            return valor != null ? valor.toString() : "";
        } catch (Exception e) {
            return "<erro>";
        }
    }

    boolean isSimples(Object obj) {
        return obj instanceof String
                || obj instanceof Number
                || obj instanceof Boolean
                || obj instanceof java.time.temporal.Temporal; // inclui LocalDate, LocalDateTime, etc.
    }

    @Override
    public String generateText(String template, Contrato contrato) {
//        Pattern pattern = Pattern.compile("&lt;([a-zA-Z]+):([a-zA-Z0-9_]+)&gt;");
        return preencherContrato(template,
                Map.of(
                        "c", contrato.getCliente(),
                        "l", "Goiânia - GO",
                        "d", LocalDate.now()
                ));


//        Pattern pattern = Pattern.compile("<c:([a-zA-Z0-9_]+)>");
//        Matcher matcher = pattern.matcher(template);
//        StringBuffer result = new StringBuffer();
//
//        while (matcher.find()) {
//            String atributo = matcher.group(1);
//            String valor = recuperarValorAtributo(contrato, atributo);
//            matcher.appendReplacement(result, Matcher.quoteReplacement(valor));
//        }
//        matcher.appendTail(result);
//        return result.toString();

    }
}
