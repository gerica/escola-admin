package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.mapper.EmpresaMapper;
import com.escola.admin.model.request.AuthenticationRequest;
import com.escola.admin.model.response.AuthenticationResponse;
import com.escola.admin.model.response.EmpresaResponse;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.security.JwtService;
import com.escola.admin.service.AuthenticationService;
import com.escola.admin.service.EmpresaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {

    UsuarioRepository repository; // Assuming you have a User entity and repository
    JwtService jwtService;
    AuthenticationManager authenticationManager;
    EmpresaService empresaService;
    EmpresaMapper empresaMapper;

    @Override
    public Mono<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        // 1. Autenticar o usuário
        // Mono.fromCallable é usado para encapsular uma chamada bloqueante (authenticationManager.authenticate)
        // e executá-la em um thread separado, não bloqueando o thread principal do Reactor.
        return Mono.fromCallable(() -> {
                    // Esta parte do código ainda é bloqueante, mas é executada fora do Event Loop principal
                    Authentication authentication = authenticationManager.authenticate( // <--- Retorna org.springframework.security.core.Authentication
                            new UsernamePasswordAuthenticationToken(
                                    request.username(),
                                    request.password()
                            )
                    );
                    return authentication; // Retornamos o objeto Authentication
                })
                // 2. Usar flatMap para encadear a próxima operação reativa
                // flatMap é usado porque a próxima etapa (buscar usuário e gerar token)
                // também resulta em um Mono.
                .flatMap(authentication -> {
                    // Supondo que você precisa do objeto User do seu banco de dados para gerar o token.
                    // Se findByUsername retorna Optional<User>, usamos Mono.justOrEmpty para transformá-lo em Mono<User>
                    return Mono.justOrEmpty(repository.findByUsername(request.username()))
                            .switchIfEmpty(Mono.error(new UsernameNotFoundException("Usuário não encontrado após autenticação."))) // Lida com usuário não encontrado
                            .flatMap(user -> {
                                // Agora que temos o User, geramos o token e construímos a respostae
                                return empresaService.findEmpresaByUsuarioId(user.getId())
                                        .map(empresaMapper::toResponse) // Mapeia Empresa para EmpresaResponse
                                        .switchIfEmpty(Mono.just(EmpresaResponse.nenhumaAssociada())) // Fornece uma EmpresaResponse padrão se vazia
                                        .flatMap(empresaResponse -> {
                                            // 4. Agora que temos o User e a EmpresaResponse, podemos gerar o JWT e construir a AuthenticationResponse final
                                            var jwtToken = jwtService.generateToken(user);

                                            return Mono.just(AuthenticationResponse.builder()
                                                    .token(jwtToken)
                                                    .username(user.getUsername())
                                                    .firstName(user.getFirstname())
                                                    .lastName(user.getLastname())
                                                    .roles(user.getRoles())
                                                    .precisaAlterarSenha(user.isPrecisaAlterarSenha())
                                                    .empresa(empresaResponse) // Associa a EmpresaResponse aqui
                                                    .build());
                                        });
                            });
                })
                // 3. Adicionar tratamento de erros global se necessário
                .onErrorMap(Exception.class, e -> {
                    // Mapeie exceções para exceções mais específicas, se desejar
                    if (e instanceof BadCredentialsException) {
                        return new BaseException("Credenciais inválidas.", e);
                    }
                    return e; // Retorne a exceção original ou uma nova
                });
    }

}