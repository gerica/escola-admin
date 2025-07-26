package com.escola.admin.security;

import com.escola.admin.model.entity.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Usamos o AntPathMatcher, a mesma ferramenta que o Spring usa para mapear rotas.
    // É a forma mais robusta de lidar com padrões de URL.
    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Este método é invocado pelo Spring para decidir se o filtro deve ser aplicado
     * a uma determinada requisição. Se ele retornar 'true', o filtro é PULADO.
     */
    @Override
    protected boolean shouldNotFilter(@org.springframework.lang.NonNull HttpServletRequest request) throws ServletException {
        // Lista de padrões de URL que devem ser públicos e ignorados pelo filtro JWT.
        final List<String> publicPatterns = List.of(
                "/",           // A raiz exata
                "/graphiql/**" // A UI do GraphiQL e todos os seus assets (CSS, JS)
//                "/graphql/**"  // O endpoint do GraphQL (para permitir introspecção e queries públicas)
        );

        // Verifica se a URL da requisição atual corresponde a algum dos padrões públicos.
        return publicPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // A lógica de validação do JWT só será executada se shouldNotFilter retornar 'false'.
        // O resto da sua lógica, que já é excelente, permanece aqui.

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String userEmail;
        Long empresaId = null;
        String empresaNome = null;

        try {
            userEmail = jwtService.extractUsername(jwt);
            empresaId = jwtService.getEmpresaIdFromToken(jwt);
            empresaNome = jwtService.getEmpresaNomeFromToken(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // --- NOVA LINHA: Definir o ID da empresa no UserDetails customizado ---
                if (userDetails instanceof Usuario) { // Verifica se é a sua classe Usuario
                    ((Usuario) userDetails).setEmpresaIdFromToken(empresaId);
                    ((Usuario) userDetails).setEmpresaNomeFromToken(empresaNome);
                }

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuário '{}' autenticado com sucesso.", userEmail);
                }
            }
        } catch (UsernameNotFoundException e) {
            log.warn("Token JWT válido para um usuário que não existe mais: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token has expired.");
            return;
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("Token JWT inválido (assinatura/formato): {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token signature or format.");
            return;
        } catch (Exception e) {
            log.error("Erro inesperado durante o processamento do JWT: {}", e.getMessage(), e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during authentication.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}