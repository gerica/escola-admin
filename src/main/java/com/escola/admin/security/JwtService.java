package com.escola.admin.security;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(Usuario userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority) // Simplificado o map
                .collect(Collectors.toList());
        extraClaims.put("authorities", authorities);

        // --- ADICIONANDO OS DADOS DA EMPRESA COMO CLAIMS ---
        Empresa empresa = userDetails.getEmpresa();
        if (empresa != null && empresa.getId() != null) { // Verifica se há uma empresa válida

            extraClaims.put("empresaId", empresa.getId());
            extraClaims.put("empresaNomeFantasia", empresa.getNomeFantasia());
//            extraClaims.put("empresaLogoUrl", empresa.getLogoUrl());
            // Adicione outros campos da empresa se desejar
            // extraClaims.put("empresaCnpj", empresaResponse.getCnpj());
        } else {
            // Opcional: Adicionar um claim para indicar que não há empresa associada
            extraClaims.put("empresaId", null); // Ou outro valor que indique ausência
            extraClaims.put("empresaNome", "Nenhuma");
        }

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public Long getEmpresaIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("empresaId", Long.class));
    }

    public String getEmpresaNomeFromToken(String token) {
        return extractClaim(token, claims -> claims.get("empresaNomeFantasia", String.class));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gera um token de impersonação.
     * O token é para o 'targetUser', mas contém claims sobre o 'impersonator'.
     */
    public String generateImpersonationToken(UserDetails targetUser, Authentication impersonator) {
        Map<String, Object> extraClaims = new HashMap<>();
        List<String> authorities = targetUser.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority) // Simplificado o map
                .collect(Collectors.toList());
        extraClaims.put("authorities", authorities);

        // Claims padrão para o usuário-alvo
        if (targetUser instanceof Usuario) {
            Empresa empresa = ((Usuario) targetUser).getEmpresa();
            if (empresa != null) {
                extraClaims.put("empresaId", empresa.getId());
                extraClaims.put("empresaNome", empresa.getNomeFantasia());
            }
        }

        // Claims ESPECIAIS de impersonação
        extraClaims.put("is_impersonated", true);
        extraClaims.put("impersonator_username", impersonator.getName()); // Nome do SUPER_ADMIN

        return Jwts.builder()
                .claims(extraClaims)
                .subject(targetUser.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Verifica se o token representa uma sessão de impersonação.
     *
     * @param token O JWT.
     * @return true se a claim 'is_impersonated' for verdadeira, caso contrário false.
     */
    public Boolean isImpersonated(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            Boolean isImpersonated = claims.get("is_impersonated", Boolean.class);
            return Boolean.TRUE.equals(isImpersonated);
        } catch (Exception e) {
            return false;
        }
    }

    // --- MÉTODO NOVO ---

    /**
     * Extrai o nome de usuário do SUPER_ADMIN que está impersonando.
     *
     * @param token O JWT de impersonação.
     * @return O nome de usuário do impersonador, ou null se a claim não existir.
     */
    public String getImpersonatorUsername(String token) {
        return extractClaim(token, claims -> claims.get("impersonator_username", String.class));
    }
}