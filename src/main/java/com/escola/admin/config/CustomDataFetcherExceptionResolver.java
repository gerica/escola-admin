package com.escola.admin.config;

import com.escola.admin.exception.BaseException;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@Order(1)
public class CustomDataFetcherExceptionResolver implements DataFetcherExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(CustomDataFetcherExceptionResolver.class);

    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment environment) {
        log.error("Exception caught by CustomDataFetcherExceptionResolver: {}", exception.getClass().getName(), exception);
        if (exception.getCause() != null) {
            log.error("Root cause of the exception: {}", exception.getCause().getClass().getName(), exception.getCause());
        }

        // Tenta encontrar a BaseException, seja na exceção atual ou na causa raiz.
        Optional<BaseException> baseExceptionOptional = Optional.of(exception)
                .filter(BaseException.class::isInstance)
                .map(BaseException.class::cast)
                .or(() -> Optional.ofNullable(exception.getCause())
                        .filter(BaseException.class::isInstance)
                        .map(BaseException.class::cast));

        if (baseExceptionOptional.isPresent()) {
            BaseException baseException = baseExceptionOptional.get();
            log.info("Handling BaseException with message: {}", baseException.getMessage());

            SourceLocation location = Optional.ofNullable(environment.getField())
                    .map(field -> field.getSourceLocation())
                    .orElseGet(() -> Optional.ofNullable(environment.getOperationDefinition())
                            .map(operation -> operation.getSourceLocation())
                            .orElse(null));

            List<Object> path = Optional.ofNullable(environment.getExecutionStepInfo())
                    .map(info -> info.getPath())
                    .map(pathInfo -> pathInfo.toList())
                    .orElse(null);

            GraphQLError error = GraphQLError.newError()
                    .message(baseException.getMessage())
                    .locations(location != null ? List.of(location) : null)
                    .path(path)
                    .errorType(ErrorType.BAD_REQUEST)
                    .build();

            return Mono.just(List.of(error));
        }

        log.warn("Exception is not a BaseException or a BaseException cause. Let's return an empty Mono to allow other resolvers to handle it.");
        // Devolve Mono.empty() para que outros resolvers possam agir ou
        // o Spring GraphQL retorne o erro padrão INTERNAL_ERROR.
        return Mono.empty();
    }
}
