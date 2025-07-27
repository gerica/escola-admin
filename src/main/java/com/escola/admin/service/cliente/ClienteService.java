package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.request.cliente.ClienteRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ClienteService {
    Cliente save(ClienteRequest request);

    Optional<Page<Cliente>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable);

    Optional<Cliente> findById(Integer id);
}
