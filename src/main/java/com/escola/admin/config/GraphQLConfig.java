package com.escola.admin.config;

import graphql.language.StringValue;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.math.BigDecimal;

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
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(bigDecimalScalar());
    }

    @Bean
    public GraphQLScalarType bigDecimalScalar() {
        return GraphQLScalarType.newScalar()
                .name("BigDecimal") // Nome exato como no seu schema.graphql
                .description("A custom scalar that handles BigDecimal numbers")
                .coercing(new Coercing<BigDecimal, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof BigDecimal) {
                            return ((BigDecimal) dataFetcherResult).toPlainString(); // Converte BigDecimal para String sem notação científica
                        }
                        throw new CoercingSerializeException("Expected a BigDecimal object but got: " + dataFetcherResult.getClass().getName());
                    }

                    @Override
                    public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof String) {
                            try {
                                return new BigDecimal((String) input);
                            } catch (NumberFormatException e) {
                                throw new CoercingParseValueException("Cannot convert value to BigDecimal: " + input, e);
                            }
                        }
                        if (input instanceof Number) { // Também aceitar números diretos (float, int) do input
                            try {
                                return new BigDecimal(input.toString());
                            } catch (NumberFormatException e) {
                                throw new CoercingParseValueException("Cannot convert number to BigDecimal: " + input, e);
                            }
                        }
                        throw new CoercingParseValueException("Expected a String or Number but got: " + input.getClass().getName());
                    }

                    @Override
                    public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return new BigDecimal(((StringValue) input).getValue());
                            } catch (NumberFormatException e) {
                                throw new CoercingParseLiteralException("Cannot convert literal to BigDecimal: " + ((StringValue) input).getValue(), e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue but got: " + input.getClass().getName());
                    }
                })
                .build();
    }

}