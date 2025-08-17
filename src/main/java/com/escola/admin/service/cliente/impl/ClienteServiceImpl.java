package com.escola.admin.service.cliente.impl;

import com.escola.admin.exception.BaseException;
import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.StatusCliente;
import com.escola.admin.model.mapper.cliente.ClienteMapper;
import com.escola.admin.model.request.cliente.ClienteRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.repository.cliente.ClienteRepository;
import com.escola.admin.service.EmpresaService;
import com.escola.admin.service.cliente.ClienteService;
import com.escola.admin.service.report.RelatorioBaseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    ClienteRepository clienteRepository;
    EmpresaService empresaService;
    ClienteMapper clienteMapper;
    RelatorioBaseService relatorioBaseService;

    @Override
    public Mono<Cliente> save(ClienteRequest request) {
        log.info("Iniciando operação de salvar/atualizar cliente. Request ID: {}, Empresa ID: {}", request.id(), request.idEmpresa());

        // 1. Validar idEmpresa primeiro
        if (request.idEmpresa() == null) {
            log.error("Tentativa de salvar cliente sem idEmpresa fornecido.");
            return Mono.error(new BaseException("O ID da empresa é obrigatório para salvar um cliente."));
        }

        // 2. Buscar a empresa e, se encontrada, continuar o fluxo de salvar o cliente.
        return empresaService.findById(request.idEmpresa())
                .switchIfEmpty(Mono.error(new BaseException("Empresa não encontrada com o ID fornecido: " + request.idEmpresa())))
                .flatMap(empresa -> {
                    log.info("Empresa com ID {} encontrada. Prosseguindo com a operação de cliente.", empresa.getId());

                    // 3. Lógica para encontrar ou criar o Cliente
                    return Mono.justOrEmpty(request.id())
                            .flatMap(id -> Mono.fromCallable(() -> clienteRepository.findById(id)))
                            .flatMap(Mono::justOrEmpty)
                            .map(existingEntity -> {
                                log.info("Atualizando cliente existente com ID: {}", existingEntity.getId());
                                clienteMapper.updateEntity(request, existingEntity);
                                return existingEntity;
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("Criando nova entidade de cliente.");
                                Cliente novoCliente = clienteMapper.toEntity(request);
                                novoCliente.setEmpresa(empresa);
                                return Mono.just(novoCliente);
                            }))
                            // 4. Salvar o Cliente
                            .flatMap(entityToSave ->
                                    Mono.fromCallable(() -> clienteRepository.save(entityToSave))
                                            .subscribeOn(Schedulers.boundedElastic()) // Garante que a operação bloqueante rode em um pool de threads
                            );
                })
                // 5. Tratamento de exceções e log final.
                .doOnSuccess(savedCliente -> log.info("Cliente salvo com sucesso. ID: {}", savedCliente.getId()))
                .doOnError(e -> {
                    if (!(e instanceof BaseException)) {
                        log.error("Ocorreu um erro genérico não esperado ao salvar o cliente: {}", e.getMessage(), e);
                    }
                })
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException);
//                .then(); // Converte o Mono<Cliente> final para Mono<Void>
    }

    @Override
    public Optional<Page<Cliente>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable) {
        Pageable effectivePageable = (pageable != null) ? pageable : Pageable.unpaged();
        return clienteRepository.findByFiltro(filtro, idEmpresa, effectivePageable);
    }

    @Override
    public Optional<Page<Cliente>> findAtivosByFiltro(String filtro, Long idEmpresa, Pageable pageable) {
        return clienteRepository.findByStatusClienteAndFiltro(filtro, idEmpresa, StatusCliente.ATIVO, pageable);
    }

    @Override
    public Optional<Page<Cliente>> findAllClientsByStatusAndFiltroWithDependents(String filtro, Long idEmpresa, Pageable pageable) {
        return clienteRepository.findAllClientsByStatusWithDependents(filtro, idEmpresa, StatusCliente.ATIVO, pageable);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return Mono.fromRunnable(() -> clienteRepository.deleteById(id))
                .doOnSuccess(v -> log.info("Cliente com ID {} excluído com sucesso.", id))
                .onErrorMap(DataIntegrityViolationException.class, this::handleDataIntegrityViolation)
                .onErrorMap(e -> !(e instanceof BaseException), BaseException::handleGenericException)
                .then();
    }

    @Override
    public Mono<Cliente> findById(Long id) {
        log.info("Buscando Cliente por ID: {}", id);
        return Mono.fromCallable(() -> clienteRepository.findById(id))
                .flatMap(optionalCargo -> {
                    if (optionalCargo.isPresent()) {
                        log.info("Cliente encontrado com sucesso para ID: {}", id);
                        return Mono.just(optionalCargo.get());
                    } else {
                        log.warn("Nenhum cargo Cliente para o ID: {}", id);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("Erro ao buscar Cliente por ID {}: {}", id, e.getMessage(), e));
    }

    private BaseException handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados ao salvar cliente: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String errorMessage;

        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            if (message.contains("key (nome)")) {
                errorMessage = "Já existe um cliente com este nome. Por favor, escolha outro.";
            } else if (message.contains("key (doc_cpf)")) {
                errorMessage = "Já existe um cliente com este CPF. Por favor, escolha outro.";
            } else if (message.contains("key (doc_rg)")) {
                errorMessage = "Já existe um cliente com este RG. Por favor, escolha outro.";
            } else if (message.contains("key (email)")) {
                errorMessage = "Já existe um cliente com este email. Por favor, escolha outro.";
            } else {
                errorMessage = "Um registro com valores duplicados já existe. Verifique os campos únicos.";
            }
        } else if (message.contains("delete on table")) {
            if (message.contains("referenced from table \"tb_matricula\"")) {
                errorMessage = "Este cliente possui matricula. Não é possível excluí-lo.";
            } else {
                errorMessage = "Este cliente possui relacionamento com outra tabela. Não é possível excluí-lo.";
            }
        } else {
            errorMessage = "Erro de integridade de dados ao salvar o cliente.";
        }

        log.error("Erro de integridade de dados processado: {}", errorMessage, e); // Mantendo log.error para a exceção original
        return new BaseException(errorMessage, e);
    }

    public Mono<RelatorioBase64Response> emitirRelatorio(FiltroRelatorioRequest request, Usuario usuario) {
        Mono<Page<Cliente>> clientesMono = Mono.justOrEmpty(findByFiltro(request.filtro(), usuario.getEmpresaIdFromToken(), null));
        return relatorioBaseService.emitirRelatorioGenerico(
                clientesMono,
                request,
                usuario,
                "Relatório de clientes", // Subtítulo
                "clientes", // Nome do arquivo
                Cliente.class // Classe da entidade
        );
    }
}
