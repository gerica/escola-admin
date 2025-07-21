package com.escola.admin.controller;

import com.escola.admin.model.request.AuthenticationRequest;
import com.escola.admin.model.response.AuthenticationResponse;
import com.escola.admin.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService service;

    @MutationMapping
    public AuthenticationResponse authenticate(@Argument AuthenticationRequest request) {
        log.info("AuthenticationController.authenticate");
        return service.authenticate(request);
    }
}