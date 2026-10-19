# Mottainai Operational API

API Spring Boot para operações de lojas e supermercados: funcionários, empresas,
lojas, produtos, fornecedores, estoque, pedidos, promoções, alertas e fidelidade.
Também existem rotas de cliente ainda presentes nesta aplicação; elas são
transitórias e podem ser separadas em outro serviço posteriormente.

## Requisitos e configuração

- Java 21
- PostgreSQL 15+
- Maven Wrapper (`./mvnw` no macOS/Linux; `mvnw.cmd` no Windows)

Crie um `.env` na raiz (não versione esse arquivo). As variáveis principais são:

```properties
DB_URL=jdbc:postgresql://localhost:5432/mottainai
DB_USER=seu_login_da_api
DB_PASSWORD=sua_senha
JWT_SECRET=<segredo-Base64-com-pelo-menos-32-bytes>
FIREBASE_PROJECT_ID=seu-firebase-project-id
STAFF_DATA_ENCRYPTION_KEY=<Base64-de-32-bytes-aleatorios>
STAFF_DATA_KEY_VERSION=1
STAFF_DATA_DECRYPTION_KEYS=
```

SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`) é usado para
convites de funcionários. `CORS_ALLOWED_ORIGINS` pode ser definido conforme o
endereço do site. Nunca coloque segredos reais no Git.

## Banco e acesso

A API não instala nem migra o banco. A equipe de dados deve provisionar o
instalador operacional PR #34; depois, a API valida o mapeamento com
`spring.jpa.hibernate.ddl-auto=validate`. Flyway fica desativado também no
perfil `local`; não há dependência de migrations para criar esse schema.

`mottainai_api` é uma role `NOLOGIN`. `DB_USER` deve ser uma role de login
dedicada, não privilegiada, com `INHERIT` e membro de `mottainai_api`. Ela não
deve ser superuser nem ter `BYPASSRLS`, pois isso contornaria o isolamento por
loja/empresa. A role e a senha de login devem ser provisionadas pela equipe de
dados; o instalador não deve ser executado usando as credenciais de runtime da
API.

A integração de autenticação de clientes também depende de `mottainai.customer.external_auth_uid`, anulável para cadastros SQL antigos e único entre UIDs Firebase vinculados. No schema protegido, a coluna armazena um token SHA-256 do UID, não o UID em claro. A API não cria essa coluna; confirme que o instalador operacional contém essa estrutura antes de conectar o ambiente.

A resposta do cliente segue as colunas disponíveis no schema protegido: `id`, `birthYear`, `active`, `marketingConsent`, `createdAt` e `updatedAt`. Ela não devolve nome, CPF, e-mail, telefone ou endereço. Veja o [contrato de cliente e transição](docs/PROTECTED_CUSTOMER_CONTRACT.md).

## Autenticação operacional

1. O app autentica o funcionário no Firebase.
2. Envia o Firebase ID token e a senha da API para `POST /api/v1/auth/login`.
   A API verifica o token e usa somente o `sub` validado como Firebase UID; não
   aceita UID fornecido separadamente.
3. Nas demais rotas operacionais, use o access token emitido pela API:

```http
Authorization: Bearer <api-access-token>
```

O bearer de funcionário deve ser um JWT da API, nunca um Firebase ID token
direto. As permissões são calculadas pelo cargo ativo no banco, não por custom
claims do Firebase:

| Cargo do banco | Authorities da API |
| --- | --- |
| Administrator | `ADMINISTRATOR`, `MANAGER` |
| Manager | `MANAGER` |
| Supervisor | `SUPERVISOR` |
| Operator | `OPERATOR` |
| Intern | `INTERN` |

`POST /api/v1/auth/refresh` rotaciona o refresh token. `POST
/api/v1/auth/logout` revoga a sessão; alterar a senha, desativar a conta ou
mudar seu cargo também revoga sessões. O app deve exigir novo login ao receber
`401` por sessão expirada ou revogada.

Não há recuperação operacional de senha por CPF/e-mail. `GET
/api/v1/auth/password-reset/validate` e `POST /api/v1/auth/password-reset` são
somente para concluir um convite de funcionário: são necessários o token de
convite e um Firebase ID token válido. Convites podem existir antes da criação
da conta Firebase; uma conta só é ativada após associar o UID verificado.

## PII de funcionários

Nome, CPF, e-mail e telefone não ficam em claro nem em HMAC no banco. A API os
armazena em `mottainai.employee_private_data` como ciphertext/nonce `BYTEA`
AES-256-GCM, com máscaras e `key_version`. CPF é exibido mascarado; contatos e
nome podem ser retornados pela API a consumidores autorizados. A chave é
externa ao banco e ao repositório.

Ao rotacionar a chave, atualize `STAFF_DATA_ENCRYPTION_KEY` e
`STAFF_DATA_KEY_VERSION`. Preserve as chaves antigas necessárias à leitura em
`STAFF_DATA_DECRYPTION_KEYS`, no formato `versao:chaveBase64`, separadas por
vírgula (por exemplo, `1:<chave-antiga>`). O `key_version` persistido escolhe a
chave de decrypt; se uma versão antiga não estiver configurada, a API falha
explicitamente em vez de tentar decifrar com a chave ativa.

## Rotas e documentação

As rotas `/api/v1/client/**` e `/api/v1/customer-catalog/**` usam Firebase ID token. O catálogo exige identidade Firebase válida; operações de perfil e fidelidade também associam o UID ao `external_auth_uid` protegido do cliente. O fluxo de cliente continua separado da autenticação operacional de funcionários.

Os endpoints operacionais incluem `/api/v1/auth`, `/api/v1/employees`,
`/api/v1/companies`, `/api/v1/stores`, `/api/v1/products`, `/api/v1/suppliers`,
`/api/v1/inventory`, `/api/v1/purchase-orders`, `/api/v1/promotions`,
`/api/v1/alerts` e outros módulos operacionais. Consulte o OpenAPI para métodos,
parâmetros, corpos e permissões exatos.

Rotas de cliente ainda existentes nesta aplicação incluem `/api/v1/client/**`,
`/api/v1/customer-catalog/**` e partes de `/api/v1/customers/**`; seus contratos
não substituem o fluxo de autenticação operacional.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Health check: `http://localhost:8080/actuator/health`

## Executar e testar

### Imagem Docker para QA

O `Dockerfile` cria uma imagem Java 21 da API. O banco é provisionado pelo repositório `Mottainai-Banco-Operacional`; a API conecta usando `DB_URL`, `DB_USER` e `DB_PASSWORD`. A imagem não contém senhas nem dados. O endpoint de verificação é `/actuator/health`.

No QA, mantenha `spring.flyway.enabled=false`: o instalador operacional cria o esquema e o Hibernate apenas valida a compatibilidade. Configure `JWT_SECRET` individualmente para o ambiente. O Compose opcional liga a API ao PostgreSQL de QA.

```bash
docker build -t mottainai-api:qa .
```

### Execução local

```powershell
./mvnw.cmd spring-boot:run
./mvnw.cmd test
```

No macOS/Linux, use `./mvnw` nos mesmos comandos. A compilação não requer
conexão com o banco; a inicialização completa precisa de um schema PR #34
instalado e de uma role de runtime que respeite RLS.

O contrato detalhado de dados protegidos está em
[`docs/STAFF_AES_GCM_SCHEMA_CONTRACT.md`](docs/STAFF_AES_GCM_SCHEMA_CONTRACT.md).
