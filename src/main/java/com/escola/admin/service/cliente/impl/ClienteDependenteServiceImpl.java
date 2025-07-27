package com.escola.admin.service.cliente.impl;

import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.ClienteDependente;
import com.escola.admin.model.mapper.cliente.ClienteDependenteMapper;
import com.escola.admin.model.request.cliente.ClienteDependenteRequest;
import com.escola.admin.repository.cliente.ClienteDependenteRepository;
import com.escola.admin.service.cliente.ClienteDependenteService;
import com.escola.admin.service.cliente.ClienteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ClienteDependenteServiceImpl implements ClienteDependenteService {

    ClienteDependenteRepository repository;
    ClienteService clienteService;
    ClienteDependenteMapper mapper;

    @Override
    public ClienteDependente save(ClienteDependenteRequest request) {
        ClienteDependente entity;
        Optional<ClienteDependente> optional = Optional.empty();
        if (request.id() != null) {
            optional = repository.findById(request.id());
        }

        if (optional.isPresent()) {
            entity = mapper.updateEntity(request, optional.get());
        } else {
            Optional<Cliente> optCli = clienteService.findById(request.idCliente());
            if (optCli.isEmpty()) {
                throw new RuntimeException("Cliente não encontrado");
            }
            entity = mapper.toEntity(request);
            entity.setCliente(optCli.get());
        }
        return repository.save(entity);
    }

    @Override
    public Optional<Boolean> apagar(Integer id) {
        repository.deleteById(id);
        return Optional.of(true);
    }

    @Override
    public Optional<ClienteDependente> findById(Integer id) {
        return repository.findById(id);
    }

    @Override
    public Optional<List<ClienteDependente>> findAllByClienteId(Integer id) {
        return repository.findAllByClienteId(id);
    }
}
