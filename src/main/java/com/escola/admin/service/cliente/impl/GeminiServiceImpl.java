package com.escola.admin.service.cliente.impl;


import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.service.cliente.ArtificalInteligenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ThinkingConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("gemini")
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements ArtificalInteligenceService {

    @Override // Certifique-se de que o método implementa a interface
    public String generateText(String prompt, Contrato contrato) {

        // Criar um ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // Registrar o módulo JavaTimeModule para lidar com LocalDate e LocalDateTime
        objectMapper.registerModule(new JavaTimeModule());
        // Opcional: Para formatar datas como "yyyy-MM-dd" e "yyyy-MM-ddTHH:mm:ss"
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Converter o objeto Contrato para uma string JSON
        try {
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contrato);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Client client = new Client();

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .thinkingConfig(
                                ThinkingConfig.builder()
                                        .thinkingBudget(0)
                                        .build())
                        .build();


        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        prompt,
                        config);

        return response.text();
    }
}