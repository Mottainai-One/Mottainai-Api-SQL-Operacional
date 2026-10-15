# Mottainai Backend

API REST operacional do Mottainai para gestão de catálogo, fornecedores, empresas, lojas, estoque, alertas, promoções e fidelidade. É uma aplicação Spring Boot com PostgreSQL, JWT interno e suporte opcional a tokens Firebase de funcionários.

## Requisitos

- Java 21
- PostgreSQL 15+
- Maven Wrapper (`bash ./mvnw`)
- Variáveis em `.env` no diretório raiz (não versionar esse arquivo):

```properties
DB_URL=jdbc:postgresql://localhost:5432/mottainai
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=<segredo-Base64-com-pelo-menos-32-bytes>
CORS_ALLOWED_ORIGINS=http://localhost:3000
FIREBASE_PROJECT_ID=seu-projeto-firebase
```

`FIREBASE_PROJECT_ID` só é necessário quando a equipe operacional também utiliza tokens Firebase.


## Banco de dados

A configuração padrão mantém o Flyway desabilitado e valida o mapeamento JPA (`spring.jpa.hibernate.ddl-auto=validate`). O provisionamento e as alterações estruturais pertencem à equipe de dados, no repositório `Mottainai-Banco-Operacional`, branch `develop`, diretório `database/operational`.

As migrations V1–V9 são históricas e não representam o schema operacional completo. A V10 depende das tabelas operacionais, de colunas usadas nas views e da role `mottainai_api`; não deve ser aplicada sobre um banco vazio provisionado apenas pelas migrations antigas. O perfil `local` aponta para `classpath:db/migration-local`, atualmente sem scripts, e não aplica a V10 de `db/migration`.

Antes de publicar a API, a equipe de dados deve disponibilizar o schema compatível e as funções/permissões acordadas. Só depois devem ser aplicadas as views, índices e funções necessárias aos repositories. A inicialização da API valida o resultado; ativar Flyway não substitui esse provisionamento. Os ajustes de RLS, autenticação e sessões precisam ser validados em PostgreSQL com a role real da API, sem usar proprietário, superusuário ou `BYPASSRLS`.

## Executar

```bash
bash ./mvnw spring-boot:run
```

Documentação interativa:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Health check: `http://localhost:8080/actuator/health`

## Autenticação e autorização

### Equipe

1. Faça `POST /api/v1/auth/login` com `cpf` (11 dígitos) e `password`.
2. A resposta contém `accessToken`, `refreshToken`, `tokenType`, `expiresIn` e `refreshExpiresIn`; os prazos são em segundos.
3. Envie o access token nas rotas de equipe:

```http
Authorization: Bearer <jwt>
```

`POST /api/v1/auth/refresh` recebe `refreshToken` e devolve um novo par de tokens, rotacionando o refresh token. Serialize as renovações e descarte o refresh anterior. `POST /api/v1/auth/logout` recebe `refreshToken` no corpo e exige o access token no cabeçalho.

Troca de senha, desativação e mudança de cargo revogam as sessões do funcionário. Após essas operações, o app deve exigir novo login quando receber `401`.

`PUT /api/v1/auth/password-recovery` recebe `cpf` e `email` para enviar um link. `GET /api/v1/auth/password-reset/validate?token=...` valida o link e `POST /api/v1/auth/password-reset` recebe `token` e `newPassword`. Funcionários convidados usam o link para definir a senha e ativar a conta; o administrador pode reenviar o convite por `POST /api/v1/employees/{id}/invite`.

As rotas operacionais exigem JWT da API. Escritas exigem `ADMINISTRATOR` ou `MANAGER`, com restrições adicionais por rota e escopo da empresa/loja. Tokens Firebase não substituem a autenticação de funcionários.

## Endpoints

| Módulo | Rotas |
| --- | --- |
| Autenticação | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/profile` |
| Senha de funcionário | `PUT /api/v1/auth/password-recovery`, `GET /api/v1/auth/password-reset/validate`, `POST /api/v1/auth/password-reset`, `PUT /api/v1/auth/password` |
| Funcionários | `POST /api/v1/employees`; `GET /api/v1/employees-store`, `GET /api/v1/employees-company`; `GET, PUT, DELETE /api/v1/employees/{id}`; `PUT /api/v1/employees/{id}/status`; `POST /api/v1/employees/{id}/invite` |
| Histórico de funcionário | `GET /api/v1/employees/{id}/audit-logs`, `GET /api/v1/employees/{id}/shifts`, `GET /api/v1/employees/{id}/cancel-request` |
| Estoque | `GET, POST /api/v1/inventory`; `GET, PUT, DELETE /api/v1/inventory/{id}`; `GET /api/v1/inventory/barcode/{barcode}`; `GET /api/v1/inventory/expiring`; `GET, POST /api/v1/inventory/{id}/movements` |
| Contagens | `GET, POST /api/v1/inventory-counts`; `GET /api/v1/inventory-counts/{id}`; `POST /api/v1/inventory-counts/{id}/items`; `PUT, DELETE /api/v1/inventory-counts/{id}/items/{itemId}`; `POST /api/v1/inventory-counts/{id}/finish` |
| Clientes e fidelidade | `GET, POST /api/v1/customers`; `GET, PUT, DELETE /api/v1/customers/{id}`; `GET /api/v1/customers/{id}/loyalty`; `GET /api/v1/customers/{id}/loyalty/transactions` |
| Lojas e promoções | Rotas operacionais de lojas, produtos, estoque e promoções, protegidas por JWT de equipe |

Esta tabela destaca os contratos de integração afetados pela revisão. Consulte `/api-docs` ou o Swagger para a lista completa de rotas, DTOs, parâmetros e códigos HTTP; nem toda listagem possui paginação.

### Substituição das rotas antigas de equipe

| Rota antiga | Contrato atual |
| --- | --- |
| `GET /api/v1/users/me` | `GET /api/v1/auth/profile` |
| `GET /api/v1/stores/me` | Obter `storeId` no perfil e consultar `GET /api/v1/stores/{id}` |
| `GET /api/v1/store-users` | `GET /api/v1/employees-store`; administradores também podem consultar `/employees-company` |
| `GET /api/v1/store-users/{id}` | `GET /api/v1/employees/{id}` |
| `POST /api/v1/store-users/invite` | `POST /api/v1/employees`; reenvio em `POST /api/v1/employees/{id}/invite` |
| `PATCH /api/v1/store-users/{id}` | `PUT /api/v1/employees/{id}`; status em `PUT /api/v1/employees/{id}/status` |

As novas rotas usam o ID de `employee`, não o ID de `app_user`. Os corpos e as permissões também devem seguir o OpenAPI atual; trocar apenas a URL não basta.

### Concorrência, histórico e idempotência

Atualizações de produto e inventário exigem `version` obtido na leitura. Em `409 Conflict`, recarregue os dados antes de permitir uma nova edição. Consultas de movimentação exigem `from` e `to` em ISO date-time, com início menor ou igual ao fim e intervalo máximo de seis meses.

Envie `Idempotency-Key` nas operações críticas que o OpenAPI exigir, incluindo criação de movimentação, finalização de contagem e recebimento de transferência. Gere uma chave por intenção e reutilize-a apenas para repetir a mesma requisição; nunca gere outra chave automaticamente para contornar um conflito.

## Validação

```bash
bash ./mvnw -q -DskipTests compile
bash ./mvnw test
```

A compilação não requer acesso ao banco. A suíte que carrega o contexto integral requer `DB_URL`, `DB_USER` e `DB_PASSWORD` válidos e um schema compatível.
