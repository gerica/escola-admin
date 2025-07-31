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
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ParametroSerivceImpl implements ParametroService {

    ParametroRepository repository;
    ObjectMapper objectMapper; // O Spring Boot fornece um bean pré-configurado

    @Override
    public Mono<Parametro> salvar(ParametroRequest request) {
        // 1. Converte o objeto de request inteiro em um Map.
        Map<String, Object> dados = objectMapper.convertValue(request, new TypeReference<>() {
        });

        // 2. A chave "chave" não deve fazer parte do JSON, então a removemos do mapa.
        dados.remove("chave");

        // 3. Busca um parâmetro existente ou cria um novo de forma reativa.
        //    Usamos flatMap para trabalhar com o resultado do Mono<Parametro>
        return this.findByChave(request.getChave())
                .defaultIfEmpty(new Parametro()) // Se o Mono for vazio (parâmetro não encontrado), emite um novo Parametro
                .flatMap(parametro -> {
                    // Agora 'parametro' nunca será nulo aqui.
                    // Atualiza os dados da entidade
                    parametro.setChave(request.getChave());
                    parametro.setJsonData(dados);

                    // O repository.save() provavelmente retorna um Parametro salvo
                    // Se o seu repository.save() retorna um Mono<Parametro>, use-o diretamente.
                    // Se retorna Parametro, você precisa envolvê-lo em Mono.just().
                    // Assumindo que o repository.save() retorna o Parametro salvo.
                    return Mono.just(repository.save(parametro));
                });
    }

    @Override
    public Mono<Parametro> findByChave(String chave) {
        return Mono.justOrEmpty(repository.findByChave(chave));
    }

}
