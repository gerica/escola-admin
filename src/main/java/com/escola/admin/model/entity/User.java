package com.escola.admin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
@Table(name = "tb_usuario")
public class User implements UserDetails {

    @Id
    @GeneratedValue
    Integer id;
    String firstname;
    String lastname;

    @Column(unique = true)
    String username; // This will be the user's login identifier

    String password;

    // 1. Mapeamento para uma coleção de Roles
    @ElementCollection(fetch = FetchType.EAGER)
    // EAGER é importante para o Spring Security carregar as roles junto com o usuário
    @CollectionTable(name = "tb_usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role") // Nome da coluna na tabela de junção "tb_usuario_roles"
    @Builder.Default
    Set<Role> roles = new HashSet<>();

//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        // Returns a list containing the user's role (e.g., "ROLE_USER")
//        return List.of(new SimpleGrantedAuthority(role.name()));

    /// /        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
//    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 2. Mapeia cada role do Set para uma autoridade que o Spring Security entende
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // For this example, we'll keep these as true.
    // You can add logic to handle account locking, expiration, etc.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
