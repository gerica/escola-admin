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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
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
            entity.setDataCadastro(LocalDateTime.now());
        }
        entity.setDataAtualizacao(LocalDateTime.now());
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
    public Optional<Cliente> findById(Integer id) {
        return clienteRepository.findById(id);
    }
}
