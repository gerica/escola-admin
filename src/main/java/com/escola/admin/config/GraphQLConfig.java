package com.escola.admin.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQLConfig {

    /**
     * Cria um bean que ensina ao Spring GraphQL como lidar com tipos escalares não padrão.
     * Cada chamada a .scalar() registra uma implementação para um tipo específico.
     *
     * @return um configurador que adiciona os scalars necessários.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                // Para o tipo java.time.Instant, o mais apropriado é o DateTime.
                .scalar(ExtendedScalars.DateTime)
                // Para o tipo java.time.LocalDate (se você usar em outro lugar).
                .scalar(ExtendedScalars.Date)
                // Para o tipo Long.
                .scalar(ExtendedScalars.GraphQLLong);
    }
}