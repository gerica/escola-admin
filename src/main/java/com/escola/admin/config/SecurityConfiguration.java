package com.escola.admin.config;

import com.escola.admin.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Desabilita CSRF para APIs stateless
                .cors(Customizer.withDefaults()) // Habilita a configuração de CORS definida no bean 'corsConfigurationSource'
                .authorizeHttpRequests(auth -> auth
                        // Libera os endpoints de autenticação, o schema do GraphQL e a UI do GraphiQL
                        .requestMatchers("/graphql", "/graphiql", "/", "/error")
                        .permitAll()
                        // Exige autenticação para todas as outras requisições
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean que define a configuração central de CORS para a aplicação.
     * Esta é a única fonte de configuração de CORS necessária.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Define as origens permitidas (seu frontend Angular, gateway, etc.)
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));

        // Define os métodos HTTP permitidos para uma API GraphQL
        // POST: Para queries e mutations.
        // GET: Para introspecção do schema por ferramentas como GraphiQL.
        // OPTIONS: Essencial para as requisições de pre-flight do CORS.
        configuration.setAllowedMethods(List.of("POST", "GET", "OPTIONS"));

        // Permite todos os cabeçalhos na requisição
        configuration.setAllowedHeaders(List.of("*"));

        // Permite o envio de credenciais (essencial para autenticação baseada em tokens/cookies)
        configuration.setAllowCredentials(true);

        // Otimização: Define o tempo (em segundos) que o navegador pode cachear a resposta de pre-flight (OPTIONS)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta configuração a todos os endpoints da aplicação ("/**")
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}