package com.escola.admin.service;

import com.escola.admin.model.request.AuthenticationRequest;
import com.escola.admin.model.response.AuthenticationResponse;
import reactor.core.publisher.Mono;

public interface AuthenticationService {

    Mono<AuthenticationResponse> authenticate(AuthenticationRequest request);
}
