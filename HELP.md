Teste
### Criar o schema
rodar: ./gradlew generateGraphQLSchema

Aqui estão os tipos mais comuns e seus significados:
* none: Esta é a opção mais segura para ambientes de produção. O Hibernate não fará nenhuma alteração no esquema do banco de dados. Ele espera que o esquema já exista e esteja correto. Se as tabelas não existirem ou estiverem incompatíveis com suas entidades, você terá erros de tempo de execução.

Uso: Ambientes de produção onde a migração de esquema é controlada por ferramentas externas (como Flyway ou Liquibase) ou feita manualmente.

* validate: O Hibernate irá validar o esquema do banco de dados em relação às suas entidades. Se houver alguma diferença, ele lançará uma exceção durante a inicialização da aplicação. Ele não fará nenhuma alteração no banco de dados.

Uso: Ambientes de produção ou homologação para garantir que o esquema do banco de dados esteja sincronizado com o código.

* update: O Hibernate tentará atualizar o esquema do banco de dados para corresponder às suas entidades. Ele adicionará colunas, tabelas e chaves, mas não excluirá nenhuma coluna ou tabela existente. Isso pode levar a esquemas "sujos" com o tempo, com colunas ou tabelas que não são mais usadas pela aplicação.

Uso: Ambientes de desenvolvimento ou teste onde você quer que o Hibernate gerencie pequenas alterações no esquema, mas sem perda de dados. Não recomendado para produção.

* create: O Hibernate criará o esquema do banco de dados do zero a cada vez que a aplicação for iniciada. Ele excluirá todas as tabelas existentes antes de criá-las novamente. Isso significa que todos os dados serão perdidos a cada inicialização.

Uso: Ambientes de desenvolvimento ou teste onde você precisa de um banco de dados limpo a cada vez (por exemplo, para testes de integração).

* create-drop: Semelhante a create, mas o Hibernate também removerá o esquema do banco de dados quando a aplicação for encerrada.

Uso: Principalmente para testes unitários ou de integração onde você precisa de um banco de dados temporário que é limpo após a execução dos testes.