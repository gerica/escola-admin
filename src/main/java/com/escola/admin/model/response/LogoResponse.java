package com.escola.admin.model.response;

public record LogoResponse(
        String uuid,
        String mimeType,
        String hash,
        String conteudoBase64
) {
}
