package com.escola.admin.service.impl;

import com.escola.admin.model.request.AuthenticationRequest;
import com.escola.admin.model.response.AuthenticationResponse;
import com.escola.admin.repository.UserRepository;
import com.escola.admin.security.JwtService;
import com.escola.admin.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository repository; // Assuming you have a User entity and repository
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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