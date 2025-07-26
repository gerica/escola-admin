package com.escola.admin.service;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.request.UsuarioRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public interface UsuarioService {

    Mono<Usuario> save(UsuarioRequest request);

    Optional<Usuario> findById(Long id);

    Optional<Page<Usuario>> findByFiltro(String filtro, Pageable pageable);

    Optional<Page<Usuario>> findByFiltroAndEmpresa(String filtro, Long idEmpresa, Pageable pageable);

    Optional<Void> deleteById(Long id);

    Optional<Void> delete(Usuario empresa);

    Mono<Void> changePassword(String newPassword);

    Mono<Void> resetPassword(String email);

    Mono<Map<String, Object>> impersonate(Long targetUserId, Authentication impersonatorAuth);

}
