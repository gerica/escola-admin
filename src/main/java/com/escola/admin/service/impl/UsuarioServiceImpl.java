package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Role;
import com.escola.admin.model.entity.TipoEmail;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.EmpresaMapper;
import com.escola.admin.model.mapper.UsuarioMapper;
import com.escola.admin.model.request.UsuarioRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.escola.admin.model.response.AuthenticationResponse;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.security.JwtService;
import com.escola.admin.service.EmailService;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.FileStorageService;
import com.escola.admin.service.UsuarioService;
import com.escola.admin.service.report.ReportService;
import com.escola.admin.util.PasswordGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    UsuarioRepository repository;
    EmpresaService empresaService;
    UsuarioMapper mapper;
    EmailService emailService;
    PasswordEncoder passwordEncoder;
    FileStorageService storageService;
    EmpresaMapper empresaMapper;
    JwtService jwtService;
    ReportService<Usuario> reportService;

    String MUTATION_SEND_EMAIL = """
            mutation SendOnboardingEmail($request: EmailRequest!) {
              sendEmail(request: $request)
            }
            """;

    @Override
    @Transactional
    public Mono<Usuario> save(UsuarioRequest request) {
        // 1. Resolve a entidade base (nova ou existente) e a senha (se aplicável).
        Mono<UserWithPassword> userWithPasswordMono = resolveUserWithPassword(request);

        // 2. Valida as regras de negócio e associa a empresa.
        Mono<UserWithPassword> validatedMono = userWithPasswordMono
                .flatMap(uwp -> validateAndAssociateEmpresa(uwp.usuario(), request)
                        .map(validatedUser -> new UserWithPassword(validatedUser, uwp.plainPassword())));

        // 3. Persiste o usuário no banco de dados (PONTO ÚNICO DE PERSISTÊNCIA).
        Mono<UserWithPassword> savedMono = validatedMono
                .flatMap(uwp -> Mono.fromCallable(() -> repository.save(uwp.usuario()))
                        .map(savedUser -> new UserWithPassword(savedUser, uwp.plainPassword())));

        // 4. Lida com efeitos colaterais (envio de e-mail) e retorna o usuário final.
        return savedMono
                .flatMap(this::sendOnboardingEmailIfNew)
                .map(UserWithPassword::usuario) // Extrai apenas o usuário para o retorno final
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
    }

    /**
     * Passo 1: Determina se é uma criação ou atualização e prepara a entidade Usuario.
     * Para novos usuários, gera e armazena a senha em texto plano temporariamente.
     */
    private Mono<UserWithPassword> resolveUserWithPassword(UsuarioRequest request) {
        return Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty)
                        .map(existingUser -> {
                            mapper.updateEntity(request, existingUser);
                            return new UserWithPassword(existingUser, null); // Senha nula para atualizações
                        }))
                .switchIfEmpty(Mono.defer(() -> {
                    Usuario novoUsuario = mapper.toEntity(request);
                    String plainPassword = PasswordGenerator.generateDefaultPassword();
                    novoUsuario.setPassword(passwordEncoder.encode(plainPassword));
                    novoUsuario.setPrecisaAlterarSenha(true);
                    return Mono.just(new UserWithPassword(novoUsuario, plainPassword));
                }));
    }

    /**
     * Passo 2: Aplica as regras de negócio de validação e associa a empresa se necessário.
     */
    private Mono<Usuario> validateAndAssociateEmpresa(Usuario usuario, UsuarioRequest request) {
        var roles = request.roles();

        // Regra 1: Se o usuário tiver mais de uma role, a empresa é obrigatória.
        if (roles != null && roles.size() > 1 && request.idEmpresa() == null) {
            return Mono.error(new BaseException("A empresa é obrigatória quando mais de uma role é atribuída ao usuário."));
        }

        // Regra 2: Se o usuário tiver apenas uma role e não for SUPER_ADMIN, a empresa é obrigatória.
        if (roles != null && roles.size() == 1 && !roles.contains(Role.SUPER_ADMIN) && request.idEmpresa() == null) {
            return Mono.error(new BaseException("A empresa é obrigatória para a role informada. Apenas SUPER_ADMIN pode ser criado sem empresa."));
        }

        // Se a validação passou, continua com a associação da empresa (se houver id).
        if (request.idEmpresa() == null) {
            usuario.setEmpresa(null); // Garante que a empresa seja nula se nenhum ID for passado
            return Mono.just(usuario); // Permitido apenas para SUPER_ADMIN único.
        }

        return empresaService.findById(request.idEmpresa())
                .switchIfEmpty(Mono.error(new BaseException("Não foi encontrada nenhuma empresa com o ID fornecido.")))
                .flatMap(empresa -> {
                    usuario.setEmpresa(empresa);
                    return Mono.just(usuario); // You need to return a Mono from flatMap
                });
    }

    /**
     * Passo 4: Envia o e-mail de boas-vindas se for um novo usuário.
     */
    private Mono<UserWithPassword> sendOnboardingEmailIfNew(UserWithPassword uwp) {
        // A presença de uma senha em texto plano é o indicador de que é um novo usuário.
        if (uwp.plainPassword() != null) {
            return sendOnboardingEmail(uwp.usuario(), uwp.plainPassword())
                    .thenReturn(uwp) // Retorna o objeto original após o e-mail ser enviado
                    .onErrorResume(e -> {
                        // Se o e-mail falhar, logamos o erro mas não quebramos a operação.
                        log.error("Criação do usuário {} bem-sucedida, mas o envio do e-mail de boas-vindas falhou.", uwp.usuario().getUsername(), e);
                        return Mono.just(uwp); // Continua o fluxo mesmo com erro no e-mail.
                    });
        }
        // Se não for um novo usuário, apenas repassa o objeto.
        return Mono.just(uwp);
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

    @Override
    public Mono<Void> changePassword(String newPassword) {
        return Mono.defer(() -> {
            // Pega o usuário autenticado do contexto de segurança
            return Mono.just(SecurityContextHolder.getContext().getAuthentication().getName())
                    .flatMap(username -> Mono.fromCallable(() -> repository.findByUsername(username)) // Supondo que você tenha esse método
                            .flatMap(Mono::justOrEmpty))
                    .switchIfEmpty(Mono.error(new BaseException("Usuário não encontrado, token inválido.")))
                    .flatMap(usuario -> {
                        // Atualiza a senha e o status
                        usuario.setPassword(passwordEncoder.encode(newPassword));
                        usuario.setPrecisaAlterarSenha(false); // <-- Ponto chave!

                        // Salva as alterações
                        return Mono.fromCallable(() -> repository.save(usuario));
                    })
                    .then(); // Converte para Mono<Void> no sucesso
        });
    }

    @Override
    public Mono<Void> resetPassword(String email) {
        return Mono.defer(() ->
                // 1. Inicia a cadeia buscando o usuário pelo e-mail de forma reativa.
                //    Não use Mono.just() para envolver outro Mono.
                Mono.fromCallable(() -> repository.findByEmail(email))
                        // 2. Desempacota o Optional<Usuario> para um Mono<Usuario>.
                        .flatMap(Mono::justOrEmpty)
                        // 3. Se o Mono estiver vazio (usuário não encontrado), lança um erro.
                        .switchIfEmpty(Mono.error(new BaseException("Usuário não encontrado com o e-mail informado.")))
                        // 4. Se o usuário foi encontrado, executa a lógica de reset.
                        .flatMap(usuario -> {
                            // Gera uma nova senha temporária.
                            String plainPassword = PasswordGenerator.generateDefaultPassword();
                            usuario.setPassword(passwordEncoder.encode(plainPassword));
                            usuario.setPrecisaAlterarSenha(true);

                            // Salva o usuário e, em seguida, envia o e-mail.
                            return Mono.fromCallable(() -> repository.save(usuario))
                                    .flatMap(savedUser -> sendResetPasswordEmail(savedUser, plainPassword)
                                            // Se o e-mail falhar, logamos, mas não quebramos a operação.
                                            .onErrorResume(e -> {
                                                log.error("Reset de senha para {} bem-sucedido, mas o envio do e-mail falhou.", savedUser.getEmail(), e);
                                                return Mono.empty(); // Continua o fluxo sem erro.
                                            })
                                    );
                        })
                        // 5. Converte o resultado final para Mono<Void> para indicar sucesso.
                        .then()
        );
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
        Pageable effectivePageable = (pageable != null) ? pageable : Pageable.unpaged();

        if (idEmpresa == null) {
            return findByFiltro(filtro, effectivePageable);
        }
        return repository.findByFiltroAndEmpresa(filtro, idEmpresa, effectivePageable);
    }

    @Override
    public Optional<Void> deleteById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Void> delete(Usuario empresa) {
        return Optional.empty();
    }

    public Mono<Void> sendOnboardingEmail(Usuario usuario, String plainTextPassword) {
        // 1. Monta a lista de variáveis para o template de e-mail.
        var variaveis = List.of(
                Map.of("key", "nomeUsuario", "value", usuario.getFirstname()),
                Map.of("key", "senhaTemporaria", "value", plainTextPassword),
                Map.of("key", "urlLogin", "value", "http://localhost:4200/login"), // Idealmente, viria de uma configuração
                Map.of("key", "nomeSistema", "value", "Escola") // Idealmente, viria de uma configuração
        );

        // 2. Monta o objeto de requisição que corresponde ao `EmailRequest` no GraphQL.
        var emailRequest = Map.of(
                "to", usuario.getEmail(),
                "subject", "Seu primeiro acesso ao sistema",
                "tipo", TipoEmail.ONBOARDING, // ou ONBOARDING, conforme seu Enum
                "variaveis", variaveis
        );

        // 3. Executa a chamada GraphQL
        log.info("Enviando e-mail de boas-vindas para: {}", usuario.getEmail());
        return emailService.executeMutation(MUTATION_SEND_EMAIL, Map.of("request", emailRequest))
                .doOnError(ex -> log.error("Falha ao enviar e-mail de boas-vindas para {}: {}", usuario.getEmail(), ex.getMessage()));
    }

    public Mono<Void> sendResetPasswordEmail(Usuario usuario, String plainTextPassword) {
        // 1. Monta a lista de variáveis para o template de e-mail.
        var variaveis = List.of(
                Map.of("key", "nomeUsuario", "value", usuario.getFirstname()),
                Map.of("key", "senhaTemporaria", "value", plainTextPassword),
                Map.of("key", "urlLogin", "value", "http://localhost:4200/login"), // Idealmente, viria de uma configuração
                Map.of("key", "nomeSistema", "value", "Escola") // Idealmente, viria de uma configuração
        );

        // 2. Monta o objeto de requisição que corresponde ao `EmailRequest` no GraphQL.
        var emailRequest = Map.of(
                "to", usuario.getEmail(),
                "subject", "Reiniciar Senha do usuário",
                "tipo", TipoEmail.RESET_PASSWORD,
                "variaveis", variaveis
        );

        // 3. Executa a chamada GraphQL
        log.info("Enviando e-mail de resetar a senha para: {}", usuario.getEmail());
        return emailService.executeMutation(MUTATION_SEND_EMAIL, Map.of("request", emailRequest))
                .doOnError(ex -> log.error("Falha ao enviar e-mail de reset de senha para {}: {}", usuario.getEmail(), ex.getMessage()));
    }

    @Override
    public Mono<Map<String, Object>> impersonate(Long targetUserId, Authentication impersonatorAuth) {
        return Mono.fromCallable(() -> repository.findById(targetUserId))
                .flatMap(Mono::justOrEmpty)
                .switchIfEmpty(Mono.error(new BaseException("Usuário alvo para impersonação não encontrado.")))
                .flatMap(targetUser -> {
                    // Regra de negócio: SUPER_ADMIN só pode impersonar ADMIN_EMPRESA
//                    if (!targetUser.getRoles().contains(Role.ADMIN_EMPRESA)) {
//                        throw new BaseException("Impersonação só é permitida para usuários com a role ADMIN_EMPRESA.");
//                    }
                    // Gera o token especial
                    String token = jwtService.generateImpersonationToken(targetUser, impersonatorAuth);
                    return storageService.getFileAsBase64(targetUser.getEmpresa().getLogo().getUuid())
                            .doOnError(e -> log.error("Arquivo do logo não encontrado para a empresa com ID {} (UUID: {}): {}",
                                    targetUser.getEmpresa().getId(), targetUser.getEmpresa().getLogo().getUuid(), e.getMessage()))
                            // RESILIÊNCIA: Em caso de erro, "resgata" o fluxo retornando um Mono vazio.
                            .onErrorResume(e -> Mono.just(""))
                            // Mapeia a resposta com o Base64 obtido (ou a string vazia)
                            .map(logoBase64 -> {
                                AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                                        .token(token)
                                        .username(targetUser.getUsername())
                                        .firstName(targetUser.getFirstname())
                                        .lastName(targetUser.getLastname())
                                        .roles(targetUser.getRoles())
                                        .precisaAlterarSenha(targetUser.isPrecisaAlterarSenha())
                                        .empresa(empresaMapper.toResponseWithLogo(targetUser.getEmpresa(), logoBase64, targetUser.getEmpresa().getLogo().getMimeType())) // Associa a EmpresaResponse aqui
                                        .build();

                                // Retorna o token e o usuário para o controller
                                return Map.of("token", token, "user", authenticationResponse);
                            });

                });
    }

    public Mono<RelatorioBase64Response> emitirRelatorio(FiltroRelatorioRequest request, Usuario usuario) {
        // 1. O fluxo reativo começa aqui. Lida com a possibilidade de clientes não existirem.
        return Mono.justOrEmpty(findByFiltroAndEmpresa(request.filtro(), usuario.getEmpresaIdFromToken(), null))
                .switchIfEmpty(Mono.empty()) // Retorna um Mono.empty() se não houver clientes
                .flatMap(entitiesPage -> {

                    if (usuario.getEmpresaIdFromToken() == null) {
                        return this.generateReport(request, entitiesPage.getContent(), usuario, "Escolar", null);
                    }

                    // 2. Tenta buscar a empresa do usuário. Se o ID for nulo ou a empresa não for encontrada,
                    // o fluxo vai para o switchIfEmpty.
                    return empresaService.findById(usuario.getEmpresaIdFromToken())
                            .flatMap(empresa -> {
                                // 3. Define a busca do logo de forma resiliente.
                                Mono<String> logoMono = (empresa.getLogo() != null)
                                        ? storageService.getFileAsBase64(empresa.getLogo().getUuid())
                                        .doOnError(e -> log.error("Arquivo do logo não encontrado para a empresa com ID {}: {}", empresa.getId(), e.getMessage(), e))
                                        .onErrorResume(e -> Mono.just(""))
                                        : Mono.just("");

                                // 4. Combina os resultados do logo e gera o relatório com os dados da empresa.
                                return logoMono.flatMap(logoBase64 ->
                                        this.generateReport(request, entitiesPage.getContent(), usuario, empresa.getNomeFantasia(), logoBase64)
                                );
                            })
                            .switchIfEmpty(
                                    // 5. Se a empresa não for encontrada ou o ID for nulo, gera o relatório com um nome genérico e sem logo.
                                    // Isso lida com o caso em que usuario.getEmpresaIdFromToken() é nulo
                                    this.generateReport(request, entitiesPage.getContent(), usuario, "Escolar", null)
                            )
                            .doOnError(e -> log.error("Erro ao buscar a empresa: {}", e.getMessage(), e));
                });
    }

    private Mono<RelatorioBase64Response> generateReport(
            FiltroRelatorioRequest request,
            List<Usuario> entities,
            Usuario usuario,
            String empresaNome,
            String logoBase64) {
        // 5. Unifica a lógica de geração do relatório em um método separado.
        return Mono.fromCallable(() -> {
            MetadadosRelatorioRequest metadados = MetadadosRelatorioRequest.builder()
                    .nomeUsuario("%s %s".formatted(usuario.getFirstname(), usuario.getLastname()))
                    .titulo("Sistema de Gestão: " + empresaNome)
                    .subtitulo("Relatório de usuários")
                    .logoBase64(logoBase64 == null || logoBase64.isBlank() ? null : logoBase64) // Garante que a string vazia seja tratada como nula
                    .nomeArquivo("usuarios")
                    .build();

            // 6. A lógica de geração de relatório continua a mesma, mas agora está em um só lugar.
            ObjectNode jsonNodes = reportService.generateReport(request.tipo(), entities, metadados, Usuario.class);

            String nomeArquivo = jsonNodes.get("filename").asText();
            String conteudoBase64 = jsonNodes.get("content").asText();
            return new RelatorioBase64Response(nomeArquivo, conteudoBase64);

        }).onErrorResume(BaseException.class, e -> {
            // Trata exceções específicas
            log.error("Erro no processamento do relatório: {}", e.getMessage(), e);
            return Mono.error(e);
        }).onErrorResume(Exception.class, e -> {
            // Trata outras exceções
            log.error("Erro desconhecido no processamento do relatório: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Erro ao processar o relatório", e));
        });
    }

    /**
     * Classe auxiliar interna para carregar o usuário e a senha em texto plano (se for novo)
     * através do fluxo reativo.
     */
    private record UserWithPassword(Usuario usuario, String plainPassword) {
    }


}