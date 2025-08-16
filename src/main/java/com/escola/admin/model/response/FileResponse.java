package com.escola.admin.model.response;

public record FileResponse(
        String uniqueFileName,
        String fileHash
) {
}