package com.escola.admin.service.impl;

import com.escola.admin.model.request.AuthenticationRequest;
import com.escola.admin.model.response.AuthenticationResponse;
import com.escola.admin.repository.UserRepository;
import com.escola.admin.security.JwtService;
import com.escola.admin.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository repository; // Assuming you have a User entity and repository
    JwtService jwtService;
    AuthenticationManager authenticationManager;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        // If we get here, the user is authenticated
        var user = repository.findByUsername(request.getUsername())
                .orElseThrow(); // Or handle exception appropriately
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .firstName(user.getFirstname())
                .lastName(user.getLastname())
                .roles(user.getRoles())
                .build();
    }
}