package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.mapper.EmpresaMapper;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.repository.EmpresaRepository;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.service.EmpresaService;
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
public class EmpresaServiceImpl implements EmpresaService {

    EmpresaRepository repository;
    UsuarioRepository usuarioRepository;
    EmpresaMapper mapper;

    @Override
    @Transactional
    public Mono<Empresa> save(EmpresaRequest request) {
        Mono<Empresa> empresaMono = Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id)))
                .flatMap(Mono::justOrEmpty).map((existingEmpresa) -> {
                    mapper.updateEntity(request, existingEmpresa);
                    return existingEmpresa;
                })
                .switchIfEmpty(Mono.defer(() -> Mono.just(mapper.toEntity(request))));

        return empresaMono.flatMap(uwp -> Mono.fromCallable(() -> repository.save(uwp)))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }

    @Override
    public Optional<Empresa> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Page<Empresa>> findByFiltro(String filtro, Pageable pageable) {
        return repository.findByFiltro(filtro, pageable);
    }

    @Override
    public Optional<Void> deleteById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Void> delete(Empresa empresa) {
        return Optional.empty();
    }

    /**
     * Busca a empresa associada a um usuário de forma reativa e segura.
     *
     * @param usuarioId O ID do usuário logado.
     * @return um Mono contendo a Empresa ou um Mono vazio se não houver associação.
     */
    public Mono<Empresa> findEmpresaByUsuarioId(Long usuarioId) {
        // Envolve a chamada bloqueante do repositório em um Mono
        return Mono.fromCallable(() -> usuarioRepository.findEmpresaByUsuarioId(usuarioId))
                // Desempacota o Optional para um Mono<Empresa> ou Mono.empty()
                .flatMap(Mono::justOrEmpty);
    }


    /**
     * Transforma uma DataIntegrityViolationException em uma BaseException mais amigável.
     */
    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar empresa: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (nomeFantasia)")) {
                errorMessage = "Já existe uma empresa com este nome fantasia. Por favor, escolha outro.";
            } else if (message.contains("key (razaoSocial)")) {
                errorMessage = "Já existe uma empresa com este razão social. Por favor, escolha outro.";
            } else if (message.contains("key (cnpj)")) {
                errorMessage = "Já existe uma empresa com este cnpj social. Por favor, escolha outro.";
            } else if (message.contains("key (email)")) {
                errorMessage = "Já existe uma empresa com este email social. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o empresa.";
        }
        return new BaseException(errorMessage, e);
    }
}
