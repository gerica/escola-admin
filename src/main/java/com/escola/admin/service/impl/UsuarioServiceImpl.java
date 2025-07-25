package com.escola.admin.service.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Role;
import com.escola.admin.model.entity.TipoEmail;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.mapper.UsuarioMapper;
import com.escola.admin.model.request.UsuarioRequest;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.service.EmailService;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.UsuarioService;
import com.escola.admin.util.PasswordGenerator;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    String MUTATION_SEND_EMAIL = """
            mutation SendOnboardingEmail($request: EmailRequest!) {
              sendEmail(request: $request)
            }
            """;

    @Override
    @Transactional
    public Mono<Usuario> save(UsuarioRequest request) {
        // 1. Encontra ou cria um usuário
        Mono<Usuario> usuarioMono = Mono.justOrEmpty(request.id())
                .flatMap(id -> Mono.fromCallable(() -> repository.findById(id))
                        .flatMap(Mono::justOrEmpty))
                .map(existingUser -> mapper.updateEntity(request, existingUser))
                .switchIfEmpty(Mono.defer(() -> { // Usamos Mono.defer para lógica complexa
                    // --- LÓGICA PARA NOVOS USUÁRIOS ---
                    Usuario novoUsuario = mapper.toEntity(request);
                    String plainPassword = PasswordGenerator.generateDefaultPassword(); // <-- Guardamos a senha aqui
                    novoUsuario.setPassword(passwordEncoder.encode(plainPassword));
                    novoUsuario.setPrecisaAlterarSenha(true);

                    // Após salvar, dispara o envio do e-mail
                    return Mono.just(novoUsuario)
                            .flatMap(userToSave -> Mono.fromCallable(() -> repository.save(userToSave)))
                            .flatMap(savedUser -> sendOnboardingEmail(savedUser, plainPassword)
                                    .thenReturn(savedUser) // Retorna o usuário salvo após o e-mail ser enviado
                                    .onErrorResume(e -> {
                                        // Se o e-mail falhar, logamos o erro mas não quebramos a criação do usuário
                                        log.error("Criação do usuário {} bem-sucedida, mas o envio do e-mail falhou.", savedUser.getUsername(), e);
                                        return Mono.just(savedUser);
                                    }));
                }));
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

                // --- INÍCIO: Bloco para TESTAR o envio de e-mail em toda operação de salvar ---
                .flatMap(savedUser -> {
                    log.info("[TESTE] Disparando e-mail para o usuário: {}", savedUser.getUsername());
                    // ATENÇÃO: A senha real não está disponível aqui para usuários existentes.
                    // Usaremos uma senha fictícia apenas para o teste da chamada.
                    String dummyPasswordForTest = "senha-de-teste-123";

                    return sendOnboardingEmail(savedUser, dummyPasswordForTest)
                            .thenReturn(savedUser) // Importante: retorna o usuário para continuar o fluxo
                            .onErrorResume(e -> {
                                // Se o envio do e-mail falhar, apenas logamos o erro, mas não quebramos a operação.
                                log.error("[TESTE] Falha ao enviar e-mail para {}, mas o usuário foi salvo com sucesso.", savedUser.getUsername(), e);
                                return Mono.just(savedUser); // Continua o fluxo mesmo com erro no e-mail.
                            });
                })
                // --- FIM DO BLOCO DE TESTE ---

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

    // Em UsuarioServiceImpl.java

    /**
     * Encapsula exceções genéricas e inesperadas em uma BaseException.
     */
    private BaseException handleGenericException(Throwable e) {
        log.error("Ocorreu um erro inesperado ao salvar o usuário.", e);
        return new BaseException("Ocorreu um erro inesperado ao salvar o usuário.", e);
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

    // Em UsuarioServiceImpl.java

    /**
     * Envia um e-mail de boas-vindas para um novo usuário.
     *
     * @param usuario           O usuário recém-criado.
     * @param plainTextPassword A senha não criptografada gerada para o primeiro acesso.
     * @return um Mono<Void> que completa quando o e-mail é enviado, ou emite um erro.
     */
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

    /**
     * Envia um e-mail para resetar a senhao.
     *
     * @param usuario           O usuário que perdeu a senha.
     * @param plainTextPassword A senha não criptografada gerada para o reset.
     * @return um Mono<Void> que completa quando o e-mail é enviado, ou emite um erro.
     */
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
}
