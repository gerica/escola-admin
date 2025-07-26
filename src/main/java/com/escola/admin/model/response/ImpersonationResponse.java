package com.escola.admin.model.response;

public record ImpersonationResponse(String token, AuthenticationResponse user) {
}