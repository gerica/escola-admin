package com.escola.admin.service.cliente.impl;

import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.StatusCliente;
import com.escola.admin.model.mapper.cliente.ClienteMapper;
import com.escola.admin.model.request.cliente.ClienteRequest;
import com.escola.admin.repository.cliente.ClienteRepository;
import com.escola.admin.service.cliente.ClienteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    ClienteRepository clienteRepository;
    ClienteMapper clienteMapper;

    @Override
    public Cliente save(ClienteRequest request) {
        Cliente entity;
        Optional<Cliente> optional = Optional.empty();
        if (request.id() != null) {
            optional = clienteRepository.findById(request.id());
        }

        if (optional.isPresent()) {
            entity = clienteMapper.updateEntity(request, optional.get());
        } else {
            entity = clienteMapper.toEntity(request);
        }
        return clienteRepository.save(entity);
    }

    @Override
    public Optional<Page<Cliente>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable) {
        return clienteRepository.findByFiltro(filtro, idEmpresa, pageable);
    }

    @Override
    public Optional<Page<Cliente>> findAtivosByFiltro(String filtro, Long idEmpresa, Pageable pageable) {
        return clienteRepository.findByStatusClienteAndFiltro(filtro, idEmpresa, StatusCliente.ATIVO, pageable);
    }

    @Override
    public Optional<Page<Cliente>> findAllClientsByStatusAndFiltroWithDependents(String filtro, Long idEmpresa, Pageable pageable) {
        return clienteRepository.findAllClientsByStatusWithDependents(filtro, idEmpresa, StatusCliente.ATIVO, pageable);
    }

    @Override
    public Mono<Cliente> findById(Long id) {
        log.info("Buscando Cliente por ID: {}", id);
        return Mono.fromCallable(() -> clienteRepository.findById(id))
                .flatMap(optionalCargo -> {
                    if (optionalCargo.isPresent()) {
                        log.info("Cliente encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalCargo.get());
                    } else {
                        log.warn("Nenhum cargo Cliente para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar Cliente por ID {}: {}", id, e.getMessage(), e));
    }
}
