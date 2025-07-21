package com.escola.admin.service;

import com.escola.admin.model.request.AuthenticationRequest;
import com.escola.admin.model.response.AuthenticationResponse;

public interface AuthenticationService {

    AuthenticationResponse authenticate(AuthenticationRequest request);
}
