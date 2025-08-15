package com.escola.admin.config.init;


import com.escola.admin.controller.help.PageableHelp;
import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.cliente.*;
import com.escola.admin.repository.cliente.ClienteRepository;
import com.escola.admin.service.EmpresaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ClienteInitializer {

    ClienteRepository clienteRepository;
    Random random = new Random();
    PageableHelp pageableHelp;
    EmpresaService empresaService; // Injetado o repositório de empresas

    private final String[] FIRST_NAMES = {
            "Ana", "João", "Maria", "Pedro", "Carla", "Lucas", "Mariana", "Gabriel", "Isabela", "Rafael",
            "Laura", "Daniel", "Sofia", "Mateus", "Lívia", "Bruno", "Beatriz", "Guilherme", "Clara", "Felipe"
    };
    private final String[] LAST_NAMES = {
            "Silva", "Santos", "Oliveira", "Souza", "Lima", "Costa", "Pereira", "Rodrigues", "Almeida", "Nascimento",
            "Ferreira", "Gomes", "Martins", "Ribeiro", "Fernandes", "Carvalho", "Araujo", "Melo", "Barbosa", "Dias"
    };

    @Transactional
    void carga() {
        log.info("Iniciando a verificação de dados (cliente) iniciais...");

        Optional<Empresa> empresaParaVinculo = getAnyExistingEmpresa();
        if (empresaParaVinculo.isEmpty()) {
            log.warn("Nenhuma empresa encontrada para vincular aos usuários. " +
                    "Certifique-se de que o EmpresaInitializer foi executado e criou empresas.");
        }

        criarClientesComDependentesEContatos(empresaParaVinculo); // Método único para clientes, dependentes e contatos

        log.info("Verificação de dados iniciais (cliente) concluída.");
    }

    void criarClientesComDependentesEContatos(Optional<Empresa> empresaParaVinculo) {

        if (clienteRepository.count() == 0) {
            log.info("Nenhum cliente encontrado. Iniciando a criação de 25 clientes de teste...");

            List<Cliente> clientes = new ArrayList<>();

            for (int i = 1; i <= 25; i++) {
                Municipio municipio = getMunicipio(i);
                // Cria o objeto Cliente
                Cliente cliente = Cliente.builder()
                        .nome(generateRandomName())
                        .dataNascimento(LocalDate.of(1980 + (i % 20), (i % 12) + 1, (i % 28) + 1))
                        .cidade(municipio.getDescricao())
                        .uf(municipio.getUf())
                        .codigoCidade(municipio.getCodigo())
                        .docCPF(String.format("%03d.%03d.%03d-%02d", random.nextInt(999), random.nextInt(999), random.nextInt(999), random.nextInt(99)))
                        .docRG(String.format("RG%07d", random.nextInt(10000000)))
                        .endereco("Rua das Flores, " + (100 + i))
                        .email("cliente" + i + "@example.com")
                        .profissao(getProfessionForIndex(i))
                        .localTrabalho("Empresa ABC " + (i % 3 + 1))
                        .statusCliente(i % 3 == 0 ? StatusCliente.INATIVO : StatusCliente.ATIVO) // Exemplo de status
                        .empresa(empresaParaVinculo.orElse(null))
                        .build();

                List<ClienteContato> contatos = criarContatosParaCliente(cliente, i);
                List<ClienteDependente> dependentes = criarDependentesParaCliente(cliente);

                cliente.setContatos(contatos);
                cliente.setDependentes(dependentes);

                clientes.add(cliente);
            }

            clienteRepository.saveAll(clientes);
            log.info(">>> 25 clientes de teste (com contatos e dependentes) criados com sucesso.");
        } else {
            log.info("Clientes já existem no banco de dados. Nenhuma ação necessária para clientes, contatos e dependentes.");
        }
    }

    private List<ClienteContato> criarContatosParaCliente(Cliente cliente, int index) {
        List<ClienteContato> contatos = new ArrayList<>();
        TipoContato[] tiposContato = TipoContato.values();

        // Contato 1: Celular
        ClienteContato celular = ClienteContato.builder()
                .numero(String.format("(%02d) 9%04d-%04d", 11 + (index % 10), random.nextInt(10000), random.nextInt(10000)))
                .tipoContato(TipoContato.CELULAR)
                .observacao("Contato principal")
                .cliente(cliente)
                .build();
        contatos.add(celular);

        // Contato 2: Residencial (opcional)
        if (random.nextBoolean()) {
            ClienteContato residencial = ClienteContato.builder()
                    .numero(String.format("(%02d) %04d-%04d", 11 + (index % 10), random.nextInt(10000), random.nextInt(10000)))
                    .tipoContato(TipoContato.RESIDENCIAL)
                    .observacao("Telefone fixo de casa")
                    .cliente(cliente)
                    .build();
            contatos.add(residencial);
        }

        return contatos;
    }

    private List<ClienteDependente> criarDependentesParaCliente(Cliente cliente) {
        List<ClienteDependente> dependentes = new ArrayList<>();
        TipoParentesco[] todosOsParentescos = TipoParentesco.values();

        // Dependente 1
        String nomeDependente1 = generateRandomName();
        Sexo sexoDependente1 = determinarSexoPeloNome(nomeDependente1);

        ClienteDependente dependente1 = ClienteDependente.builder()
                .nome(nomeDependente1)
                .dataNascimento(cliente.getDataNascimento().plusYears(18 + random.nextInt(15))) // Idade mais realista para dependentes
                .parentesco(todosOsParentescos[random.nextInt(todosOsParentescos.length)])
                .sexo(sexoDependente1)
                .docCPF(String.format("%03d.%03d.%03d-%02d", random.nextInt(999), random.nextInt(999), random.nextInt(999), random.nextInt(99)))
                .cliente(cliente)
                .build();
        dependentes.add(dependente1);

        // Adiciona um segundo dependente (opcional)
        if (random.nextBoolean()) {
            String nomeDependente2 = generateRandomName();
            Sexo sexoDependente2 = determinarSexoPeloNome(nomeDependente2);

            ClienteDependente dependente2 = ClienteDependente.builder()
                    .nome(nomeDependente2)
                    .dataNascimento(cliente.getDataNascimento().plusYears(random.nextInt(18)).minusYears(random.nextInt(5))) // Idade mais realista
                    .parentesco(todosOsParentescos[random.nextInt(todosOsParentescos.length)])
                    .sexo(sexoDependente2)
                    .docCPF(String.format("%03d.%03d.%03d-%02d", random.nextInt(999), random.nextInt(999), random.nextInt(999), random.nextInt(99)))
                    .cliente(cliente)
                    .build();
            dependentes.add(dependente2);
        }

        return dependentes;
    }

    private Sexo determinarSexoPeloNome(String nomeCompleto) {
        String primeiroNome = nomeCompleto.split(" ")[0];
        if (primeiroNome.endsWith("a") || primeiroNome.endsWith("A")) { // Considera maiúsculas e minúsculas
            return Sexo.MULHER;
        }
        return Sexo.HOMEM;
    }

    private Municipio getMunicipio(int index) {
        switch (index % 5) {
            case 0:
                return Municipio.builder()
                        .codigo("9825")
                        .descricao("São Paulo")
                        .uf("SP")
                        .build();
            case 1:
                return Municipio.builder()
                        .codigo("10500")
                        .descricao("Rio de Janeiro")
                        .uf("RJ")
                        .build();
            case 2:
                return Municipio.builder()
                        .codigo("6889")
                        .descricao("Belo Horizonte")
                        .uf("MG")
                        .build();
            case 3:
                return Municipio.builder()
                        .codigo("8359")
                        .descricao("Feira de Santana")
                        .uf("BA")
                        .build();
            case 4:
                return Municipio.builder()
                        .codigo("6281")
                        .descricao("Curitiba")
                        .uf("PR")
                        .build();
            default:
                return Municipio.builder()
                        .codigo("5550")
                        .descricao("Brasília")
                        .uf("DF")
                        .build();
        }
    }

    private String getProfessionForIndex(int index) {
        switch (index % 5) {
            case 0:
                return "Engenheiro";
            case 1:
                return "Médico";
            case 2:
                return "Professor";
            case 3:
                return "Desenvolvedor";
            case 4:
                return "Designer";
            default:
                return "Outros";
        }
    }

    private String generateRandomName() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    @Transactional
    private Optional<Empresa> getAnyExistingEmpresa() {
        Optional<Page<Empresa>> byFiltro = empresaService.findByFiltro("", pageableHelp.getPageable(0, 1, new ArrayList<>()));
        return byFiltro.get().getContent().stream().findAny();

    }
}