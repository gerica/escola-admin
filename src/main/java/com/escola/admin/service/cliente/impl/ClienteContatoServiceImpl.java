package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.cliente.ClienteContato;
import com.escola.admin.model.mapper.cliente.ClienteContatoMapper;
import com.escola.admin.model.request.cliente.ClienteContatoRequest;
import com.escola.admin.repository.cliente.ClienteContatoRepository;
import com.escola.admin.service.cliente.ClienteContatoService;
import com.escola.admin.service.cliente.ClienteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ClienteContatoServiceImpl implements ClienteContatoService {

    ClienteContatoRepository repository;
    ClienteService clienteService;
    ClienteContatoMapper mapper;

    @Override
    public Mono<ClienteContato> save(ClienteContatoRequest request) {
        Mono<ClienteContato> entityMono = Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id)))
                .flatMap(Mono::justOrEmpty)
                .map(existingEntity -> {
                            mapper.updateEntity(request, existingEntity);
                            return existingEntity;
                        }
                )
                .switchIfEmpty( // Se não encontrou pelo ID, então é um novo ClienteContato
                        Mono.defer(() -> {
                            ClienteContato newEntity = mapper.toEntity(request);
                            return clienteService.findById(request.idCliente()) // Assume que findById retorna Mono<Cliente>
                                    .switchIfEmpty(Mono.error(new BaseException("Cliente com o ID " + request.idCliente() + " não encontrado.")))
                                    .map(cliente -> {
                                        newEntity.setCliente(cliente);
                                        return newEntity;
                                    });
                        })
                );

        return entityMono.flatMap(clienteContatoToSave -> Mono.fromCallable(() -> repository.save(clienteContatoToSave))) // Salva a entidade
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }

    @Override
    public Optional<Boolean> apagar(Long id) {
        repository.deleteById(id);
        return Optional.of(true);
    }

    @Override
    public Optional<ClienteContato> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<List<ClienteContato>> findAllByClienteId(Long id) {
        return repository.findAllByClienteId(id);
    }

    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar contato: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (numero)")) {
                errorMessage = "Já existe um contato com este número. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o contato.";
        }
        return new BaseException(errorMessage, e);
    }
}
