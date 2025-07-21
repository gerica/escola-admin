package com.escola.admin.controller;

import com.escola.admin.model.request.AuthenticationRequest;
import com.escola.admin.model.response.AuthenticationResponse;
import com.escola.admin.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationController {

    AuthenticationService service;

    @MutationMapping
    public AuthenticationResponse authenticate(@Argument AuthenticationRequest request) {
        log.info("AuthenticationController.authenticate");
        return service.authenticate(request);
    }
}