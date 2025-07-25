package com.escola.admin.service.impl;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.mapper.EmpresaMapper;
import com.escola.admin.model.request.EmpresaRequest;
import com.escola.admin.repository.EmpresaRepository;
import com.escola.admin.repository.UsuarioRepository;
import com.escola.admin.service.EmpresaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EmpresaServiceImpl implements EmpresaService {

    EmpresaRepository repository;
    UsuarioRepository usuarioRepository;
    EmpresaMapper mapper;

    @Override
    public Optional<Empresa> save(EmpresaRequest request) {
        Empresa entity;
        Optional<Empresa> optional = Optional.empty();
        if (request.id() != null) {
            optional = repository.findById(request.id());
        }

        if (optional.isPresent()) {
            entity = mapper.updateEntity(request, optional.get());
        } else {
            entity = mapper.toEntity(request);
//            entity.setDataCadastro(LocalDateTime.now());
        }
//        entity.setDataAtualizacao(LocalDateTime.now());
        return Optional.of(repository.save(entity));
    }

    @Override
    public Optional<Empresa> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Page<Empresa>> findByFiltro(String filtro, Pageable pageable) {
        return repository.findByFiltro(filtro, pageable);
    }

    @Override
    public Optional<Void> deleteById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Void> delete(Empresa empresa) {
        return Optional.empty();
    }

    /**
     * Busca a empresa associada a um usuário de forma reativa e segura.
     *
     * @param usuarioId O ID do usuário logado.
     * @return um Mono contendo a Empresa ou um Mono vazio se não houver associação.
     */
    public Mono<Empresa> findEmpresaByUsuarioId(Long usuarioId) {
        // Envolve a chamada bloqueante do repositório em um Mono
        return Mono.fromCallable(() -> usuarioRepository.findEmpresaByUsuarioId(usuarioId))
                // Desempacota o Optional para um Mono<Empresa> ou Mono.empty()
                .flatMap(Mono::justOrEmpty);
    }
}
