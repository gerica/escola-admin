package com.escola.admin.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Slf4j // Adicionado para logar erros inesperados
public class GraphQlExceptionHandlerAdvice {

    /**
     * Captura falhas de autenticação por credenciais incorretas (usuário/senha).
     * Retorna um erro do tipo UNAUTHORIZED.
     */
    @GraphQlExceptionHandler
    public GraphQLError handleBadCredentials(BadCredentialsException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Credenciais inválidas. Por favor, verifique seu e-mail e senha.")
                .errorType(ErrorType.UNAUTHORIZED)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }

    /**
     * Captura tentativas de login de usuários que estão desabilitados no sistema.
     * Retorna um erro do tipo UNAUTHORIZED.
     */
    @GraphQlExceptionHandler
    public GraphQLError handleUserDisabled(DisabledException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Esta conta de usuário está desabilitada ou bloqueada. Por favor, contate o administador do sistema.")
                .errorType(ErrorType.UNAUTHORIZED)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }

    /**
     * Captura falhas de autorização, quando um usuário autenticado tenta acessar um recurso
     * para o qual não tem permissão (ex: um USER tentando acessar uma rota de ADMIN).
     * Retorna um erro do tipo FORBIDDEN.
     */
    @GraphQlExceptionHandler
    public GraphQLError handleAccessDenied(AccessDeniedException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Acesso negado. Você não tem permissão para executar esta ação.")
                .errorType(ErrorType.FORBIDDEN)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }

    /**
     * Captura exceções de negócio personalizadas (BaseException).
     * Essas são falhas "esperadas", como erros de validação (ex: e-mail duplicado).
     * Retorna a mensagem exata da exceção e um erro do tipo BAD_REQUEST.
     */
    @GraphQlExceptionHandler
    public GraphQLError handleBaseException(BaseException ex, DataFetchingEnvironment env) {
        // Loga como um aviso, pois é um erro de negócio, não uma falha inesperada do sistema.
        log.warn("Erro de negócio tratado: Path [{}], Mensagem [{}]", env.getExecutionStepInfo().getPath(), ex.getMessage());

        return GraphqlErrorBuilder.newError()
                // Pega a mensagem exata da sua exceção!
                .message(ex.getMessage())
                // BAD_REQUEST é o tipo ideal para erros de validação e regras de negócio.
                .errorType(ErrorType.BAD_REQUEST)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }

    /**
     * Handler genérico (fallback) para qualquer outra exceção não tratada.
     * ANTES de tratar como um erro interno, ele verifica se a CAUSA da exceção
     * é uma BaseException, delegando para o handler correto se for o caso.
     */
    @GraphQlExceptionHandler
    public GraphQLError handleGenericException(Exception ex, DataFetchingEnvironment env) {
        // --- INÍCIO DA ALTERAÇÃO ROBUSTA ---
        Throwable cause = ex;
        // Loop para "desembrulhar" as exceções aninhadas
        while (cause != null) {
            if (cause instanceof BaseException baseException) {
                // Encontramos nossa exceção de negócio! Delegamos para o handler correto.
                return handleBaseException(baseException, env);
            }
            // Passa para a próxima causa na cadeia
            cause = cause.getCause();
        }
        // --- FIM DA ALTERAÇÃO ROBUSTA ---

        // Se não for, continua sendo um erro inesperado.
        // Loga a exceção completa no servidor para que a equipe de desenvolvimento possa investigar.
        log.error("Ocorreu um erro inesperado ao processar a requisição GraphQL", ex);

        return GraphqlErrorBuilder.newError()
                .message("Ocorreu um erro interno no servidor. Por favor, tente novamente mais tarde.")
                .errorType(ErrorType.INTERNAL_ERROR)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}