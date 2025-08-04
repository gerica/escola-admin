package com.escola.admin.service.cliente;


import com.escola.admin.model.entity.cliente.Anexo;
import com.escola.admin.model.request.cliente.AnexoRequest;
import com.escola.admin.model.response.cliente.AnexoBase64Response;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AnexoService {

    Mono<Anexo> save(AnexoRequest request);

    Mono<Anexo> findById(Long id);

    Mono<List<Anexo>> findByIdContrato(Long idContrato);

    Mono<Void> deleteById(Long id);

    Mono<AnexoBase64Response> downloadAnexo(Long id);

}
