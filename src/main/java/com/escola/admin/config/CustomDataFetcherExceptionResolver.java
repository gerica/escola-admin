package com.escola.admin.config;

import com.escola.admin.security.BaseException;
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

@Component
@Order(1)
public class CustomDataFetcherExceptionResolver implements DataFetcherExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(CustomDataFetcherExceptionResolver.class); // Instanciar Logger

    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment environment) {
        log.error("Exception caught by CustomDataFetcherExceptionResolver: {}", exception.getClass().getName(), exception); // Log the exception type
        if (exception.getCause() != null) {
            log.error("Root cause of the exception: {}", exception.getCause().getClass().getName(), exception.getCause());
        }

        if (exception instanceof BaseException baseException) {
            log.info("Handling BaseException: {}", baseException.getMessage());
            // ... (rest of your existing code for BaseException)
            SourceLocation location = null;
            if (environment.getField() != null && environment.getField().getSourceLocation() != null) {
                location = environment.getField().getSourceLocation();
            } else if (environment.getOperationDefinition() != null && environment.getOperationDefinition().getSourceLocation() != null) {
                location = environment.getOperationDefinition().getSourceLocation();
            }

            List<Object> path = environment.getExecutionStepInfo() != null &&
                    environment.getExecutionStepInfo().getPath() != null ?
                    environment.getExecutionStepInfo().getPath().toList() : null;

            GraphQLError error = GraphQLError.newError()
                    .message(baseException.getMessage())
                    .locations(location != null ? List.of(location) : null)
                    .path(path)
                    .errorType(ErrorType.BAD_REQUEST)
                    .build();
            return Mono.just(List.of(error));
        }
        // If it's not a BaseException, you could convert it to a generic BAD_REQUEST
        // or let it fall through to be an INTERNAL_ERROR.
        // For debugging, let's explicitly convert any other exception to a BAD_REQUEST
        // to see if it catches anything. This is temporary for debugging.
        log.warn("Exception is not a BaseException, letting other resolvers handle or returning default error.");

        // This part is for debugging purposes to see if ANY exception gets here.
        // In production, you might want Mono.empty() for unhandled exceptions.
        GraphQLError genericError = GraphQLError.newError()
//                .message("An unexpected error occurred: " + exception.getMessage())
                .message(exception.getMessage())
                .errorType(ErrorType.INTERNAL_ERROR) // Still an internal error, but with more info
                .build();
        return Mono.just(List.of(genericError));
        // return Mono.empty(); // For production, if you only want to handle BaseException
    }
}