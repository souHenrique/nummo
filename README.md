# Nummo

API REST para gestão financeira pessoal, com controle de contas, categorias,
transações, transferências, cartões de crédito, faturas, boletos, orçamentos, relatórios,
dashboard e exportação de transações em CSV.

O projeto aplica isolamento dos dados por usuário, autenticação JWT, validações
de negócio, controle transacional, concorrência otimista, auditoria de entidades
financeiras e migrações versionadas do banco de dados.

## Estrutura do repositório

```text
nummo/
├── backend/       API Spring Boot
├── frontend/      Aplicação web Angular
├── compose.yaml   Infraestrutura local
└── README.md
```

## Funcionalidades implementadas

- Cadastro, autenticação e atualização do perfil do usuário;
- Contas financeiras com saldo atual e ativação/inativação;
- Categorias de receita e despesa, incluindo relacionamento hierárquico;
- Receitas, despesas, PIX e transferências entre contas;
- Pesquisa paginada de transações com filtros combináveis;
- Cartões de crédito e compras à vista ou parceladas;
- Geração, consulta, fechamento e pagamento de faturas;
- Estorno de compras no cartão e aplicação de créditos em faturas;
- Boletos únicos ou parcelados mensalmente, com vencimento, edição, cancelamento e registro de pagamento;
- Orçamentos mensais por categoria, com cálculo de consumo e alertas;
- Relatórios de caixa diário, semanal, mensal e anual;
- Relatório financeiro por competência;
- Dashboard financeiro com identificação explícita do regime de cada indicador;
- Exportação das transações filtradas em CSV;
- Documentação OpenAPI e Swagger UI;
- Auditoria de contas, transações, cartões e faturas com Hibernate Envers.

## Tecnologias

- Java 25;
- Spring Boot 4.1;
- Spring Web MVC;
- Spring Data JPA e Hibernate;
- Spring Security e JWT;
- PostgreSQL 16;
- Flyway;
- Hibernate Envers;
- MapStruct e Lombok;
- Springdoc OpenAPI;
- JUnit 5, Mockito e Testcontainers;
- Maven Wrapper e Docker Compose.

## Requisitos

- JDK 25;
- Docker com Docker Compose;
- Nenhuma instalação global do Maven é necessária, pois o projeto inclui o
  Maven Wrapper.

## Configuração

Use `.env.example` como referência:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nummo
DB_USERNAME=nummo
DB_PASSWORD=change-me-locally
JWT_SECRET=replace-with-a-base64-secret-of-at-least-32-bytes
JWT_EXPIRATION=3600000
```

`JWT_EXPIRATION` é informado em milissegundos. Não utilize o segredo de exemplo
fora do ambiente local.

O Docker Compose e o perfil `dev` da aplicação leem o arquivo `.env` localizado
na raiz do repositório. Em produção, configure as variáveis diretamente no
ambiente do processo.

### PowerShell

```powershell
Copy-Item .env.example .env
Set-Location backend
.\mvnw.cmd spring-boot:run
```

### Linux ou macOS

```bash
cp .env.example .env
cd backend
./mvnw spring-boot:run
```

No perfil padrão `dev`, a integração do Spring Boot com Docker Compose inicia o
PostgreSQL definido em `compose.yaml`. A API fica disponível em
`http://localhost:8080`.

Para produção, use o perfil `prod` e configure `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `JWT_SECRET` e, opcionalmente, `JWT_EXPIRATION`.

## Autenticação

Somente cadastro, login e documentação da API são públicos. Os demais endpoints
exigem um token JWT no cabeçalho:

```http
Authorization: Bearer <token>
```

Exemplo de cadastro:

```bash
curl --request POST http://localhost:8080/api/v1/auth/register \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Henrique Amorim",
    "email": "henrique@example.com",
    "password": "SenhaSegura123"
  }'
```

Exemplo de login:

```bash
curl --request POST http://localhost:8080/api/v1/auth/login \
  --header "Content-Type: application/json" \
  --data '{
    "email": "henrique@example.com",
    "password": "SenhaSegura123"
  }'
```

## Endpoints

### Usuário, contas e categorias

| Método  | Endpoint                       | Descrição                      |
| ------- | ------------------------------ | ------------------------------ |
| `POST`  | `/api/v1/auth/register`        | Cadastrar usuário              |
| `POST`  | `/api/v1/auth/login`           | Autenticar e obter JWT         |
| `GET`   | `/api/v1/users/me`             | Consultar o perfil autenticado |
| `PATCH` | `/api/v1/users/me`             | Atualizar o perfil autenticado |
| `PATCH` | `/api/v1/users/me/password`    | Alterar a senha                |
| `DELETE`| `/api/v1/users/me`             | Excluir a conta logicamente    |
| `POST`  | `/api/v1/accounts`             | Criar conta                    |
| `GET`   | `/api/v1/accounts`             | Listar contas                  |
| `GET`   | `/api/v1/accounts/{id}`        | Consultar conta                |
| `PATCH` | `/api/v1/accounts/{id}`        | Atualizar conta                |
| `PATCH` | `/api/v1/accounts/{id}/status` | Alterar o status da conta      |
| `POST`  | `/api/v1/categories`           | Criar categoria                |
| `GET`   | `/api/v1/categories`           | Listar categorias              |
| `GET`   | `/api/v1/categories/{id}`      | Consultar categoria            |
| `PATCH` | `/api/v1/categories/{id}`      | Atualizar categoria            |

### Transações e transferências

| Método  | Endpoint                           | Descrição                       |
| ------- | ---------------------------------- | ------------------------------- |
| `POST`  | `/api/v1/transactions`             | Criar receita ou despesa        |
| `GET`   | `/api/v1/transactions`             | Pesquisar transações            |
| `GET`   | `/api/v1/transactions/{id}`        | Consultar transação             |
| `PATCH` | `/api/v1/transactions/{id}`        | Atualizar transação             |
| `POST`  | `/api/v1/transactions/{id}/cancel` | Cancelar transação              |
| `POST`  | `/api/v1/transfers`                | Transferir valores entre contas |

### Cartões e faturas

| Método  | Endpoint                                                              | Descrição                             |
| ------- | --------------------------------------------------------------------- | ------------------------------------- |
| `POST`  | `/api/v1/credit-cards`                                                | Criar cartão de crédito               |
| `GET`   | `/api/v1/credit-cards`                                                | Listar cartões                        |
| `GET`   | `/api/v1/credit-cards/{id}`                                           | Consultar cartão                      |
| `PATCH` | `/api/v1/credit-cards/{id}`                                           | Atualizar cartão                      |
| `POST`  | `/api/v1/credit-cards/{id}/purchases`                                 | Registrar compra à vista ou parcelada |
| `POST`  | `/api/v1/credit-cards/{creditCardId}/purchase/{transactionId}/refund` | Estornar compra no cartão             |
| `GET`   | `/api/v1/invoices`                                                    | Pesquisar faturas                     |
| `GET`   | `/api/v1/invoices/{id}`                                               | Consultar fatura e transações         |
| `GET`   | `/api/v1/credit-cards/{id}/invoices`                                  | Listar faturas de um cartão           |
| `POST`  | `/api/v1/invoices/{id}/close`                                         | Fechar fatura                         |
| `POST`  | `/api/v1/invoices/{id}/pay`                                           | Pagar fatura                          |

### Boletos


| Método | Endpoint | Descrição |
| ------ | -------- | --------- |
| `POST` | `/api/v1/bills` | Cadastrar boleto único ou parcelas mensais |
| `GET` | `/api/v1/bills?year=YYYY&month=M&status=PENDING&page=0&size=20` | Listar por vencimento; status opcional |
| `GET` | `/api/v1/bills/{id}` | Consultar boleto |
| `PATCH` | `/api/v1/bills/{id}` | Editar um boleto pendente |
| `POST` | `/api/v1/bills/{id}/pay` | Registrar pagamento integral |
| `POST` | `/api/v1/bills/{id}/cancel` | Cancelar boleto preservando histórico |


### Orçamentos, relatórios e dashboard

| Método   | Endpoint                                                             | Descrição                            |
| -------- | -------------------------------------------------------------------- | ------------------------------------ |
| `POST`   | `/api/v1/budgets`                                                    | Criar orçamento mensal               |
| `GET`    | `/api/v1/budgets`                                                    | Listar orçamentos                    |
| `GET`    | `/api/v1/budgets/{id}`                                               | Consultar orçamento                  |
| `PATCH`  | `/api/v1/budgets/{id}`                                               | Atualizar orçamento                  |
| `DELETE` | `/api/v1/budgets/{id}`                                               | Excluir definitivamente orçamento    |
| `GET`    | `/api/v1/reports/cash/daily?date=YYYY-MM-DD`                         | Relatório de caixa diário            |
| `GET`    | `/api/v1/reports/cash/weekly?date=YYYY-MM-DD`                        | Relatório e comparação semanal       |
| `GET`    | `/api/v1/reports/cash/monthly?year=YYYY&month=M`                     | Relatório de caixa mensal            |
| `GET`    | `/api/v1/reports/cash/annual?year=YYYY`                              | Evolução anual do caixa              |
| `GET`    | `/api/v1/reports/competence?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` | Relatório por competência            |
| `GET`    | `/api/v1/dashboard`                                                  | Dashboard financeiro consolidado     |
| `GET`    | `/api/v1/exports/transactions.csv`                                   | Exportar transações filtradas em CSV |

## Exportação CSV

O endpoint `GET /api/v1/exports/transactions.csv` reutiliza o mesmo objeto de
filtros, a mesma validação e a mesma especificação da pesquisa de transações.
Ele exporta todos os registros encontrados, sem os parâmetros de paginação.

Exemplo:

```bash
curl --get http://localhost:8080/api/v1/exports/transactions.csv \
  --header "Authorization: Bearer <token>" \
  --data-urlencode "startDate=2026-09-01" \
  --data-urlencode "endDate=2026-09-30" \
  --data-urlencode "status=COMPLETED" \
  --output transactions.csv
```

Contrato do arquivo:

- Codificação UTF-8, sem BOM;
- Mídia `text/csv;charset=UTF-8`;
- Download com o nome `transactions.csv`;
- Separador por vírgula e linhas terminadas por `CRLF`;
- Campos com vírgula, aspas ou quebras de linha são envolvidos por aspas;
- Aspas internas são duplicadas;
- Datas seguem ISO-8601 (`YYYY-MM-DD`);
- Datas ausentes são exportadas como campo vazio;
- Valores monetários usam representação decimal simples, como `1234.56`;
- Ordenação por `competenceDate` decrescente e `id` crescente;
- Categorias e contas são resolvidas somente dentro do usuário autenticado;
- Para transferências, `account` é representado como `Origem -> Destino`.

Cabeçalho estável:

```csv
id,description,type,status,amount,competenceDate,effectiveDate,category,account
```

Quando não há resultados, o arquivo contém somente o cabeçalho. Compras no
cartão ficam com `account` vazio porque não movimentam uma conta no momento da
compra.

## Documentação da API

Em desenvolvimento, com a aplicação em execução:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Os contratos documentam schemas públicos, parâmetros, exemplos, autenticação e
respostas de erro padronizadas. Swagger UI e os documentos OpenAPI são
desativados no perfil `prod`, portanto não ficam expostos no deploy público.

## Banco de dados e migrações

O PostgreSQL é versionado pelo Flyway. As migrações estão em
`backend/src/main/resources/db/migration` e são aplicadas automaticamente na
inicialização.

O Hibernate utiliza `ddl-auto: validate`: a aplicação valida o schema, mas não o
modifica automaticamente. Atualmente existem migrações de `V1` a `V12`,
incluindo usuários, contas, categorias, transações, auditoria, cartões, faturas,
estornos, créditos e orçamentos.

## Testes

Os testes de integração utilizam PostgreSQL real por meio do Testcontainers.
Mantenha o Docker em execução.

## Segurança contínua

O GitHub executa CodeQL para Java e TypeScript em pushes, pull requests e uma
varredura semanal. Pull requests que alteram dependências também passam por uma
revisão de vulnerabilidades conhecidas. A árvore Maven resolvida, incluindo
dependências transitivas, é enviada ao Dependency Graph do GitHub para que os
alertas do Dependabot cubram o backend sem depender de feeds de CVEs externos.
O Dependabot abre atualizações semanais para Maven, pnpm e GitHub Actions.

### PowerShell

```powershell
Set-Location backend
.\mvnw.cmd test
```

### Linux ou macOS

```bash
cd backend
./mvnw test
```

A suíte cobre regras de domínio, autenticação e isolamento por usuário,
persistência, concorrência, relatórios, documentação OpenAPI, dashboard e o
contrato completo da exportação CSV.

## Produção

O frontend é publicado no Vercel, a API Spring Boot no Render e o PostgreSQL é
fornecido pelo Supabase.
