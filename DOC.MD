# Detalhamento das Permissões por Role 

## 1. SUPER_ADMIN
Este é o papel com controle total e irrestrito sobre todo o ERP. É a role que você, como desenvolvedor/proprietário do sistema, usaria.

### Acesso e Gestão de Empresas:
* Criar novas instâncias de empresas (clientes).
* Visualizar, editar e excluir informações de qualquer empresa.
* Acessar os dados de qualquer empresa (para suporte ou auditoria).
* Gerenciar planos de assinatura e faturamento dos clientes.

### Gestão de Usuários Globais:
* Criar e gerenciar outros usuários SUPER_ADMIN.
* Impersonar (acessar como) qualquer usuário ADMIN_EMPRESA para suporte.

### Configurações do Sistema:
* Acessar e modificar configurações globais do ERP.
* Gerenciar logs do sistema.
* Realizar manutenções e atualizações do sistema.
* Definir padrões para novas empresas.

### Relatórios e Análises:
* Acessar relatórios consolidados de uso do sistema entre todas as empresas.
* Visualizar métricas de desempenho do ERP.

## 2. ADMIN_EMPRESA
Este é o administrador da empresa cliente. Ele tem controle total sobre os dados e configurações da sua própria empresa, mas não tem visibilidade ou acesso a outras empresas.

### Gestão de Usuários da Empresa:
* Criar, editar e excluir usuários para a sua empresa (ex: COORDENADOR, PROFESSOR, FINANCEIRO, RECEPCIONISTA).
* Atribuir e modificar roles e permissões específicas para os usuários da sua empresa.
* Redefinir senhas de usuários da sua empresa.

### Configurações da Empresa:
* Configurar os dados cadastrais da empresa (nome, endereço, logo, contato).
* Personalizar campos adicionais nos módulos (ex: adicionar um campo "Tipo Sanguíneo" no cadastro de aluno).
* Gerenciar tabelas auxiliares (ex: tipos de cursos, modalidades de pagamento).
* Configurar modelos de comunicação (e-mails, notificações).

### Acesso Total aos Módulos:
* Acessar todas as funcionalidades e relatórios de todos os módulos dentro da sua empresa: Cadastro de Alunos, Financeiro, Gestão de Turmas, Comunicação, etc.

### Relatórios:
* Acessar todos os relatórios gerenciais e operacionais da sua empresa.

## 3. COORDENADOR
Ideal para gerentes de área ou chefes de equipe que precisam de uma visão mais ampla, mas não total, da operação da empresa.

### Módulo de Gestão de Turmas/Aulas:
* Criar, editar e excluir turmas/aulas.
* Alocar alunos e professores nas turmas.
* Visualizar a frequência de todas as turmas.

### Módulo de Cadastro de Alunos/Clientes:
* Visualizar e editar todos os dados de alunos/clientes.
* Matricular/desmatricular alunos.

### Módulo de Comunicação:
* Enviar comunicados para turmas específicas ou grupos de alunos/responsáveis.
* Visualizar histórico de comunicações.

### Módulo de Gestão de Colaboradores (visão limitada):
* Visualizar informações básicas dos professores/colaboradores sob sua coordenação.
* Visualizar horários de trabalho.

### Relatórios:
* Acessar relatórios operacionais: lista de alunos por turma, frequência, ocupação de turmas, relatórios pedagógicos (se houver módulo).

* Módulo Financeiro (somente visualização):
* Opcional: Visualizar resumos financeiros gerais (ex: mensalidades a receber da sua área), mas sem permissão para alterar dados financeiros.

## 4. PROFESSOR
Para os educadores e instrutores que atuam diretamente com os alunos.

### Módulo de Gestão de Turmas/Aulas:

* Visualizar suas próprias turmas e horários.
* Registrar presença/falta dos alunos nas suas aulas.

### Módulo de Cadastro de Alunos/Clientes (visão limitada):
* Visualizar informações básicas dos alunos das suas turmas (nome, contato, informações de saúde relevantes).
* Sem permissão para editar dados cadastrais gerais ou financeiros dos alunos.

### Módulo de Comunicação:
* Enviar mensagens diretas para os responsáveis dos alunos das suas turmas.
* Receber comunicados da administração.

### Módulo de Avaliação/Desempenho (se implementado):
* Registrar o progresso e o desempenho dos alunos nas suas aulas.
* Adicionar observações e feedback.

### Módulo de Portal do Aluno/Responsável (se implementado):
* Acesso para visualizar o que o responsável vê do aluno para melhor atendimento.

## 5. FINANCEIRO
Responsável por todas as operações monetárias da empresa.

### Módulo Financeiro:
* Gerar e emitir boletos/faturas para mensalidades e outras cobranças.
* Registrar recebimentos e conciliar pagamentos.
* Controlar inadimplência e gerenciar cobranças.
* Registrar despesas e contas a pagar.
* Realizar lançamentos de fluxo de caixa.
* Gerenciar contas bancárias e formas de pagamento.

### Módulo de Cadastro de Alunos/Clientes (visão limitada):
* Visualizar o histórico financeiro dos alunos/clientes.
* Acessar dados de contato para fins de cobrança.
* Sem permissão para alterar dados pedagógicos ou de turmas.

### Relatórios:
* Acessar todos os relatórios financeiros: fluxo de caixa, balancete simples, contas a receber/pagar, inadimplência.

### Módulo de Comunicação:
* Enviar lembretes de pagamento ou comunicados relacionados a finanças.

## 6. RECEPCIONISTA (ou SECRETARIA/ATENDENTE)
Geralmente o primeiro ponto de contato e responsável pela gestão inicial de alunos e agendamentos.

### Módulo de Cadastro de Alunos/Clientes:
* Cadastrar novos alunos/clientes (com campos configurados para o perfil).
* Editar informações básicas de contato e matrícula de alunos existentes.
* Visualizar informações de saúde e contato de emergência.

### Módulo de Gestão de Turmas/Aulas (visão limitada):
* Visualizar horários de turmas e professores.
* Consultar disponibilidade de vagas em turmas.
* Realizar matrículas de alunos em turmas.

### Módulo de Matrículas Online/Inscrições (se implementado):
* Visualizar e gerenciar leads de pré-matrículas.
* Mudar status do processo de inscrição.

### Módulo de Comunicação:
* Enviar comunicados gerais da empresa.
* Visualizar histórico de comunicados enviados/recebidos.
 
### Módulo Financeiro (somente visualização básica):
* Opcional: Visualizar status de pagamento de mensalidades para informar responsáveis, mas sem permissão para realizar operações financeiras.