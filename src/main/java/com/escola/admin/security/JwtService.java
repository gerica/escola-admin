package com.escola.admin.security;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
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
}