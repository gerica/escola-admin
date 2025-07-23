package com.escola.admin.service.impl;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.UsuarioMapper;
import com.escola.admin.model.request.UsuarioRequest;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.security.BaseException;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.UsuarioService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    UsuarioRepository repository;
    EmpresaService empresaService;
    UsuarioMapper mapper;

    @Override
    @Transactional
    public Mono<Usuario> save(UsuarioRequest request) { // No more 'throws BaseException' here
        return Mono.defer(() -> { // Use Mono.defer to defer execution until subscribed
            Usuario entity;
            Optional<Usuario> optional = Optional.empty();

            if (request.id() != null) {
                optional = repository.findById(request.id());
            }

            if (optional.isPresent()) {
                entity = mapper.updateEntity(request, optional.get());
            } else {
                entity = mapper.toEntity(request);
            }
            if (request.idEmpresa() != null) {
                Optional<Empresa> empresaOptional = empresaService.findById(request.idEmpresa());
                if (empresaOptional.isEmpty()) {
                    // If company not found, return a Mono.error
                    return Mono.error(new BaseException("Não foi encontrada nenhuma empresa com o ID fornecido."));
                }
                entity.setEmpresa(empresaOptional.get());
            }

            try {
                // Wrap the blocking save operation in a Mono.just or Mono.fromCallable
                return Mono.just(repository.save(entity));
            } catch (DataIntegrityViolationException e) {
                log.info("DataIntegrityViolationException caught in service: {}", e.getMessage());

                String errorMessage = "Erro de integridade de dados ao salvar o usuário.";
                if (e.getMessage() != null) {
                    if (e.getMessage().toLowerCase().contains("duplicate key") || e.getMessage().toLowerCase().contains("unique constraint")) {
                        if (e.getMessage().toLowerCase().contains("key (username)")) {
                            errorMessage = "Já existe um usuário com este nome de usuário. Por favor, escolha outro.";
                        } else if (e.getMessage().toLowerCase().contains("key (email)")) {
                            errorMessage = "Já existe um usuário com este endereço de e-mail. Por favor, escolha outro.";
                        } else {
                            errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
                        }
                    }
                }
                // Return a Mono.error for BaseException
                return Mono.error(new BaseException(errorMessage, e));
            } catch (Exception e) {
                log.info("Generic Exception caught in service: {}", e.getMessage());
                // Return a Mono.error for unexpected exceptions
                return Mono.error(new BaseException("Ocorreu um erro inesperado ao salvar o usuário.", e));
            }
        }); // End Mono.defer
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Page<Usuario>> findByFiltro(String filtro, Pageable pageable) {
        return repository.findByFiltro(filtro, pageable);
    }

    @Override
    public Optional<Page<Usuario>> findByFiltroAndEmpresa(String filtro, Long idEmpresa, Pageable pageable) {
        return repository.findByFiltroAndEmpresa(filtro, idEmpresa, pageable);
    }

    @Override
    public Optional<Void> deleteById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Void> delete(Usuario empresa) {
        return Optional.empty();
    }
}
