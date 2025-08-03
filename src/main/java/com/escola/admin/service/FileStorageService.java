package com.escola.admin.service;

import reactor.core.publisher.Mono;

public interface FileStorageService {

    Mono<String> saveFile(String contentBase64);

    Mono<Boolean> deleteFile(String uuid);
}
