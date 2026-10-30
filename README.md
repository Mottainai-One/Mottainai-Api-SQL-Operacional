# Mottainai Operational API

API REST em Spring Boot para operações de lojas e supermercados, incluindo funcionários, empresas, lojas, produtos, fornecedores, estoque, pedidos, promoções, alertas e fidelidade.

Esta aplicação também mantém algumas rotas de cliente, como `/api/v1/client/**` e `/api/v1/customer-catalog/**`. Elas usam autenticação Firebase e seguem um fluxo separado das rotas operacionais de funcionários.

## Requisitos

- Java 21
- PostgreSQL 15 ou superior
- Maven Wrapper incluído no repositório

## Configuração

Crie um arquivo `.env` na raiz da API. Não versione esse arquivo nem inclua segredos reais no Git.

```properties
DB_URL=jdbc:postgresql://localhost:5432/mottainai
DB_USER=seu_login_da_api
DB_PASSWORD=sua_senha

JWT_SECRET=<segredo-Base64-com-pelo-menos-32-bytes>
FIREBASE_PROJECT_ID=seu-firebase-project-id

STAFF_DATA_ENCRYPTION_KEY=<chave-Base64-de-32-bytes>
STAFF_DATA_KEY_VERSION=1
STAFF_DATA_DECRYPTION_KEYS=
STAFF_DATA_LOOKUP_KEY=<outra-chave-Base64-de-32-bytes>

CUSTOMER_DATA_ENCRYPTION_KEY=<chave-Base64-de-32-bytes>
CUSTOMER_DATA_KEY_VERSION=1
CUSTOMER_DATA_DECRYPTION_KEYS=
```

As chaves de criptografia de funcionários e clientes devem ser distintas. `STAFF_DATA_LOOKUP_KEY` também deve ser uma chave Base64 de 32 bytes, diferente da chave de criptografia de funcionários. Gere chaves aleatórias e guarde-as em um gerenciador de segredos.

O envio de convites de funcionários usa SMTP, configurado por `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` e `MAIL_PASSWORD`. `CORS_ALLOWED_ORIGINS` configura as origens permitidas pelo CORS.

## Banco de dados

A API não instala nem migra o banco. O schema operacional é mantido no repositório `Mottainai-Banco-Operacional`. O instalador vigente fica em `database/operational/install.sql` e requer PostgreSQL 15 ou superior e `psql`.

Para uma instalação nova, siga as instruções do README do repositório do banco. Para um banco já existente, siga o fluxo de atualização indicado lá; não reaplique o instalador completo sobre um banco em uso. O processo de atualização de dados pessoais pode exigir preparação, backfill e finalização antes de remover colunas antigas.

Na inicialização, a API valida o schema com `spring.jpa.hibernate.ddl-auto=validate`. Flyway fica desativado, inclusive no perfil `local`; as migrations deste repositório não criam o schema operacional.

`mottainai_api` é uma role `NOLOGIN`. Configure `DB_USER` com uma role de login dedicada, membro de `mottainai_api`, sem privilégios de superuser e sem `BYPASSRLS`. As credenciais de runtime da API não devem ser usadas para instalar ou atualizar o schema.

A integração de clientes depende de `mottainai.customer.external_auth_uid`. No schema protegido, esse campo armazena um token SHA-256 do UID Firebase, e não o UID em claro. A API espera que a estrutura compatível esteja provisionada no banco.

## Autenticação operacional

1. O aplicativo autentica o funcionário no Firebase.
2. Envia o Firebase ID token e a senha da API para `POST /api/v1/auth/login`.
3. A API valida o ID token e usa o `sub` verificado como Firebase UID. O cliente não envia um UID separado.
4. Após o login, as rotas operacionais protegidas usam o access token emitido pela API:

```http
Authorization: Bearer <api-access-token>
```

Não use um Firebase ID token como bearer nas rotas operacionais. As permissões são derivadas de `employee_role.permission_level`, não do nome do cargo nem de custom claims do Firebase.

| `permission_level` | Authorities da API |
| ---: | --- |
| 100 | `ADMINISTRATOR`, `MANAGER` |
| 80 | `MANAGER` |
| 60 | `SUPERVISOR` |
| 40 | `OPERATOR` |
| 20 | `INTERN` |

Níveis não reconhecidos não recebem authorities. A API usa os níveis numéricos provisionados no banco.

`POST /api/v1/auth/refresh` rotaciona o refresh token. `POST /api/v1/auth/logout` revoga a sessão. Alterações de senha, desativação de conta ou mudança de cargo também revogam sessões. O aplicativo deve solicitar novo login quando receber `401` por sessão expirada ou revogada.

Não há recuperação operacional de senha por CPF ou e-mail. `GET /api/v1/auth/password-reset/validate` e `POST /api/v1/auth/password-reset` são usados para concluir um convite de funcionário e exigem o token de convite e um Firebase ID token válido. Uma conta só é ativada depois da associação com o UID verificado.

No cadastro de funcionário, `createLogin: false` cria o funcionário sem `app_user` ou convite. A rota `POST /api/v1/employees/{id}/invite` permite conceder acesso posteriormente, quando houver um e-mail cadastrado.

## Proteção de dados pessoais

O schema operacional vigente inclui `mottainai.employee_private_data`, que armazena nome, CPF, e-mail e telefone em envelopes AES-256-GCM, com nonce, máscaras e versão da chave. A tabela `employee` mantém HMACs de busca de CPF e e-mail; esses valores não podem ser revertidos para os dados originais. No schema protegido finalizado, as colunas de identidade em claro são removidas de `employee`.

A API descriptografa dados de funcionários somente para respostas autorizadas. O CPF é exibido mascarado. Mantenha as chaves fora do banco de dados e do repositório.

Configure `STAFF_DATA_LOOKUP_KEY` como uma chave Base64 de 32 bytes, distinta de `STAFF_DATA_ENCRYPTION_KEY`. Mantenha a chave de busca estável: alterá-la exige recalcular os HMACs existentes. Ao rotacionar a chave AES, atualize `STAFF_DATA_ENCRYPTION_KEY` e `STAFF_DATA_KEY_VERSION`. Preserve as chaves históricas necessárias em `STAFF_DATA_DECRYPTION_KEYS`, no formato `versao:chaveBase64`, separadas por vírgula. A API usa a versão persistida para selecionar a chave de descriptografia.

Os dados originais de clientes são armazenados na tabela `customer_private_data` em um payload AES-256-GCM, com uma chave independente configurada por `CUSTOMER_DATA_ENCRYPTION_KEY`. O instalador operacional vigente inclui essa tabela e suas políticas de acesso. Apenas Administradores podem ler dados originais de clientes ativos pela rota `/api/v1/customers/{id}/private-data`.

O cadastro de cliente ainda não possui associação a tenant ou empresa no banco; essa rota não fornece isolamento por empresa. Desativar um cliente mantém os dados criptografados e bloqueia a rota. Anonimizar apaga o payload e os tokens correspondentes. Os prazos de retenção e as finalidades de uso devem ser definidos pela empresa.

Em bancos já instalados, siga as instruções de atualização do repositório do banco para preparar e preencher os dados criptografados antes da finalização do schema.

## Rotas e documentação

As rotas operacionais incluem, entre outras:
- `/api/v1/auth`
- `/api/v1/employees`
- `/api/v1/companies`
- `/api/v1/stores`
- `/api/v1/products`
- `/api/v1/suppliers`
- `/api/v1/inventory`
- `/api/v1/purchase-orders`
- `/api/v1/promotions`
- `/api/v1/alerts`

Consulte o OpenAPI para ver métodos, parâmetros, corpos de requisição e permissões de cada operação.

As rotas `/api/v1/client/**` e `/api/v1/customer-catalog/**` usam Firebase ID token. O fluxo de cliente é separado da autenticação operacional de funcionários. Há também rotas de cliente em `/api/v1/customers/**`; elas não substituem o fluxo de autenticação operacional.

Com a API em execução local:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/api-docs>
- Health check: <http://localhost:8080/actuator/health>

## Executar localmente

No Windows:

```powershell
./mvnw.cmd spring-boot:run
```

No macOS ou Linux:

```bash
./mvnw spring-boot:run
```

A inicialização precisa de um schema operacional compatível, variáveis de ambiente configuradas e uma role de runtime que respeite RLS.

## Testes

No Windows:

```powershell
./mvnw.cmd test
```

No macOS ou Linux:

```bash
./mvnw test
```

## Docker e QA

O `Dockerfile` cria uma imagem Java 21 da API. O banco deve ser provisionado separadamente pelo repositório `Mottainai-Banco-Operacional`. A imagem não contém senhas nem dados.

Para criar a imagem:

```bash
docker build -t mottainai-api:qa .
```

No QA, mantenha `spring.flyway.enabled=false`, configure `JWT_SECRET` e as chaves de dados individualmente para o ambiente e use uma role de runtime sem `SUPERUSER` ou `BYPASSRLS`. O Compose opcional conecta a API ao PostgreSQL de QA.
