package com.escola.admin.service.impl;

import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.request.ParametroRequest;
import com.escola.admin.repository.ParametroRepository;
import com.escola.admin.service.ParametroService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ParametroSerivceImpl implements ParametroService {

    ParametroRepository repository;
    ObjectMapper objectMapper; // O Spring Boot fornece um bean pré-configurado


    @Override
    public Optional<Parametro> salvar(ParametroRequest request) {
        // 1. Converte o objeto de request inteiro em um Map.
        //    O Jackson usará os nomes dos campos como chaves do mapa.
        //    Graças à configuração 'non_null', campos nulos no request serão ignorados.
        Map<String, Object> dados = objectMapper.convertValue(request, new TypeReference<>() {
        });

        // 2. A chave "chave" não deve fazer parte do JSON, então a removemos do mapa.
        dados.remove("chave");

        // 3. Busca um parâmetro existente ou cria um novo.
        //    Isso evita criar duplicatas e permite a atualização.
        Parametro parametro = this.findByChave(request.getChave())
                .orElse(new Parametro()); // Se não existir, cria uma nova instância

        // 4. Atualiza os dados da entidade
        parametro.setChave(request.getChave());
        parametro.setJsonData(dados);

        return Optional.of(repository.save(parametro));
    }

    @Override
    public Optional<Parametro> findByChave(String chave) {
        return repository.findByChave(chave);
    }

}
