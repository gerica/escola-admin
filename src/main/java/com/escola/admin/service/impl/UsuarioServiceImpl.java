package com.escola.admin.service.impl;

import com.escola.admin.model.entity.Role;
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
    public Mono<Usuario> save(UsuarioRequest request) {
        // 1. Inicia o fluxo: encontra um usuário existente para atualizar ou prepara um novo para criar.
        Mono<Usuario> usuarioMono = Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)) // Converte Mono<Optional<Usuario>> para Mono<Usuario> ou Mono.empty()
                .map(existingUser -> mapper.updateEntity(request, existingUser))
                .switchIfEmpty(Mono.fromSupplier(() -> mapper.toEntity(request)));

        return usuarioMono
                // 2. Valida as regras de negócio e associa a empresa, se aplicável.
                .flatMap(usuario -> {
                    var roles = request.roles(); // Supondo que request.roles() retorna um Set<String> ou List<String>

                    // --- INÍCIO DA NOVA LÓGICA DE VALIDAÇÃO ---

                    // Regra 1: Se o usuário tiver mais de uma role, a empresa é obrigatória.
                    if (roles != null && roles.size() > 1 && request.idEmpresa() == null) {
                        return Mono.error(new BaseException("A empresa é obrigatória quando mais de uma role é atribuída ao usuário."));
                    }

                    // Regra 2: Se o usuário tiver apenas uma role e não for SUPER_ADMIN, a empresa é obrigatória.
                    if (roles != null && roles.size() == 1 && !roles.contains(Role.SUPER_ADMIN) && request.idEmpresa() == null) {
                        return Mono.error(new BaseException("A empresa é obrigatória para a role informada. Apenas SUPER_ADMIN pode ser criado sem empresa."));
                    }

                    // --- FIM DA NOVA LÓGICA DE VALIDAÇÃO ---

                    // Se a validação passou, continua com a associação da empresa (se houver id).
                    if (request.idEmpresa() == null) {
                        return Mono.just(usuario); // Continua sem empresa (permitido apenas para SUPER_ADMIN único).
                    }

                    // Procura a empresa e a associa ao usuário.
                    return Mono.fromCallable(() -> empresaService.findById(request.idEmpresa()))
                            .flatMap(Mono::justOrEmpty) // Converte Optional<Empresa> para Mono<Empresa>
                            .map(empresa -> {
                                usuario.setEmpresa(empresa);
                                return usuario;
                            })
                            .switchIfEmpty(Mono.error(new BaseException("Não foi encontrada nenhuma empresa com o ID fornecido.")));
                })
                // 3. Salva o usuário (novo ou atualizado) no banco de dados.
                .flatMap(usuarioParaSalvar -> Mono.fromCallable(() -> repository.save(usuarioParaSalvar)))
                // 4. Trata erros de forma reativa.
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), this::handleGenericException);
    }

    /**
     * Transforma uma DataIntegrityViolationException em uma BaseException mais amigável.
     */
    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar usuário: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (username)")) {
                errorMessage = "Já existe um usuário com este nome de usuário. Por favor, escolha outro.";
            } else if (message.contains("key (email)")) {
                errorMessage = "Já existe um usuário com este endereço de e-mail. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o usuário.";
        }
        return new BaseException(errorMessage, e);
    }

    /**
     * Encapsula exceções genéricas e inesperadas em uma BaseException.
     */
    private BaseException handleGenericException(Throwable e) {
        log.error("Ocorreu um erro inesperado ao salvar o usuário.", e);
        return new BaseException("Ocorreu um erro inesperado ao salvar o usuário.", e);
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
