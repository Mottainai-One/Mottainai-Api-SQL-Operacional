# Mottainai — Documentação técnica da API

## Escopo e fonte de verdade

Este documento descreve a API exposta pelos controllers em `src/main/java/com/institutojf/mottainai/controller`. Em caso de divergência, o código, as interfaces Swagger em `controller/swagger` e a configuração de segurança prevalecem.

- **Versão da API:** `v1`
- **Controllers REST:** 17
- **Operações HTTP mapeadas:** 82
- **Documentação interativa:** `/swagger-ui.html`
- **OpenAPI:** `/api-docs`
- **Migrations Flyway:** 9 (`V1` a `V9`)

## Stack e configuração

| Componente | Implementação |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.0 |
| Persistência | Spring Data JPA / Hibernate |
| Banco | PostgreSQL, schema `mottainai` |
| Migrations | Flyway (`classpath:db/migration`) |
| Autenticação de equipe | JWT HS256 emitido pela aplicação |
| Autenticação de cliente | JWT Firebase em `/api/v1/client/**` |
| Validação | Jakarta Validation |
| Documentação | springdoc-openapi 3.0.3 |

O Hibernate é configurado com `ddl-auto: validate`; portanto, a aplicação valida o schema existente e não o altera. As variáveis obrigatórias de banco e JWT são lidas do ambiente (`DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`).

## Segurança e autorização

### Convenções usadas nas tabelas

- **Público:** não requer token.
- **JWT equipe:** requer JWT da aplicação. Por configuração global, leituras exigem autenticação; `POST`, `PUT` e `DELETE` exigem `ADMINISTRATOR` ou `MANAGER`, salvo regra de método mais restritiva.
- **Administrador:** exige `ADMINISTRATOR` por `@PreAuthorize`.
- **Administrador ou gerente da própria loja:** valida a loja do usuário pelo `retailStoreAccess`.
- **JWT cliente (Firebase):** requer token Firebase válido; todos os caminhos em `/api/v1/client/**` usam essa cadeia de segurança.

Além das regras acima, inventário, lotes, promoções e itens de promoção recebem o `Authentication` no serviço para aplicar a autorização contextual da operação. O JWT de equipe usa o claim `roles` e é invalidado quando a versão do token não corresponde ao usuário atual.

Os únicos endpoints públicos da API são:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

## Endpoints

Os status abaixo descrevem a resposta de sucesso. Os contratos Swagger associados a cada controller detalham também os erros de validação, recurso inexistente e conflito aplicáveis.

### Autenticação — 3 operações

| Método | Caminho | Sucesso | Acesso |
|---|---|---:|---|
| POST | `/api/v1/auth/login` | 200 `TokenResponse` | Público |
| POST | `/api/v1/auth/forgot-password` | 204 sem corpo | Público |
| POST | `/api/v1/auth/reset-password` | 204 sem corpo | Público |

### Perfil e usuários de loja — 6 operações

| Método | Caminho | Sucesso | Acesso |
|---|---|---:|---|
| GET | `/api/v1/users/me` | 200 `UserResponse` | JWT equipe |
| GET | `/api/v1/stores/me` | 200 `RetailStoreResponse` | JWT equipe |
| GET | `/api/v1/store-users` | 200 `List<UserResponse>` | Administrador |
| GET | `/api/v1/store-users/{id}` | 200 `UserResponse` | Administrador |
| POST | `/api/v1/store-users/invite` | 201 `InviteStoreUserResponse` | Administrador |
| PATCH | `/api/v1/store-users/{id}` | 200 `UserResponse` | Administrador |

### Empresas, lojas, endereços e planos — 19 operações

| Método | Caminho | Sucesso | Acesso |
|---|---|---:|---|
| GET | `/api/v1/companies` | 200 `Page<CompanyResponse>` | Administrador |
| GET | `/api/v1/companies/{id}` | 200 `CompanyResponse` | Administrador |
| POST | `/api/v1/companies` | 201 `CompanyResponse` | Administrador |
| PUT | `/api/v1/companies/{id}` | 200 `CompanyResponse` | Administrador |
| DELETE | `/api/v1/companies/{id}` | 204 sem corpo | Administrador |
| GET | `/api/v1/stores` | 200 `Page<RetailStoreResponse>` | Administrador |
| GET | `/api/v1/stores/{id}` | 200 `RetailStoreResponse` | Administrador ou gerente da própria loja |
| POST | `/api/v1/stores` | 201 `RetailStoreResponse` | Administrador |
| PUT | `/api/v1/stores/{id}` | 200 `RetailStoreResponse` | Administrador ou gerente da própria loja |
| DELETE | `/api/v1/stores/{id}` | 204 sem corpo | Administrador |
| GET | `/api/v1/addresses` | 200 `Page<AddressResponse>` | JWT equipe |
| GET | `/api/v1/addresses/{id}` | 200 `AddressResponse` | JWT equipe |
| POST | `/api/v1/addresses` | 201 `AddressResponse` | JWT equipe; administrador ou gerente |
| PUT | `/api/v1/addresses/{id}` | 200 `AddressResponse` | JWT equipe; administrador ou gerente |
| GET | `/api/v1/subscription-plans` | 200 `Page<SubscriptionPlanResponse>` | Administrador |
| GET | `/api/v1/subscription-plans/{id}` | 200 `SubscriptionPlanResponse` | Administrador |
| POST | `/api/v1/subscription-plans` | 201 `SubscriptionPlanResponse` | Administrador |
| PUT | `/api/v1/subscription-plans/{id}` | 200 `SubscriptionPlanResponse` | Administrador |
| DELETE | `/api/v1/subscription-plans/{id}` | 204 sem corpo | Administrador |

> Não há endpoint `DELETE /api/v1/addresses/{id}`.

### Catálogo — 21 operações

| Método | Caminho | Sucesso | Acesso |
|---|---|---:|---|
| GET | `/api/v1/products` | 200 `Page<ProductResponse>` | JWT equipe |
| GET | `/api/v1/products/{id}` | 200 `ProductResponse` | JWT equipe |
| GET | `/api/v1/products/barcode/{barcode}` | 200 `ProductResponse` | JWT equipe |
| POST | `/api/v1/products` | 201 `ProductResponse` | Administrador ou gerente |
| PUT | `/api/v1/products/{id}` | 200 `ProductResponse` | Administrador ou gerente |
| DELETE | `/api/v1/products/{id}` | 204 sem corpo | Administrador ou gerente |
| GET | `/api/v1/product-categories` | 200 `Page<ProductCategoryResponse>` | JWT equipe |
| GET | `/api/v1/product-categories/{id}` | 200 `ProductCategoryResponse` | JWT equipe |
| POST | `/api/v1/product-categories` | 201 `ProductCategoryResponse` | Administrador ou gerente |
| PUT | `/api/v1/product-categories/{id}` | 200 `ProductCategoryResponse` | Administrador ou gerente |
| DELETE | `/api/v1/product-categories/{id}` | 204 sem corpo | Administrador ou gerente |
| GET | `/api/v1/suppliers` | 200 `Page<SupplierResponse>` | JWT equipe |
| GET | `/api/v1/suppliers/{id}` | 200 `SupplierResponse` | JWT equipe |
| POST | `/api/v1/suppliers` | 201 `SupplierResponse` | Administrador ou gerente |
| PUT | `/api/v1/suppliers/{id}` | 200 `SupplierResponse` | Administrador ou gerente |
| DELETE | `/api/v1/suppliers/{id}` | 204 sem corpo | Administrador ou gerente |
| GET | `/api/v1/supplier-products` | 200 `Page<SupplierProductResponse>` | JWT equipe |
| GET | `/api/v1/supplier-products/{id}` | 200 `SupplierProductResponse` | JWT equipe |
| POST | `/api/v1/supplier-products` | 201 `SupplierProductResponse` | Administrador ou gerente |
| PUT | `/api/v1/supplier-products/{id}` | 200 `SupplierProductResponse` | Administrador ou gerente |
| DELETE | `/api/v1/supplier-products/{id}` | 204 sem corpo | Administrador ou gerente |

### Inventário e lotes — 12 operações

| Método | Caminho | Sucesso | Acesso |
|---|---|---:|---|
| GET | `/api/v1/inventory?storeId={storeId}` | 200 `List<InventoryResponse>` | JWT equipe; escopo validado no serviço |
| GET | `/api/v1/inventory/{id}` | 200 `InventoryResponse` | JWT equipe; escopo validado no serviço |
| POST | `/api/v1/inventory` | 201 `InventoryResponse` | Administrador ou gerente; escopo validado no serviço |
| PUT | `/api/v1/inventory/{id}` | 200 `InventoryResponse` | Administrador ou gerente; escopo validado no serviço |
| DELETE | `/api/v1/inventory/{id}` | 204 sem corpo | Administrador ou gerente; escopo validado no serviço |
| GET | `/api/v1/inventory/barcode/{barcode}?storeId={storeId}` | 200 `List<InventoryResponse>` | JWT equipe; escopo validado no serviço |
| GET | `/api/v1/inventory/expiring?storeId={storeId}&days=30` | 200 `List<InventoryResponse>` | JWT equipe; escopo validado no serviço |
| GET | `/api/v1/inventory/{id}/movements` | 200 `List<InventoryMovementResponse>` | JWT equipe; escopo validado no serviço |
| POST | `/api/v1/inventory/{id}/movements` | 201 `InventoryMovementResponse` | Administrador ou gerente; escopo validado no serviço |
| GET | `/api/v1/batches` | 200 `List<BatchResponse>` | JWT equipe; escopo validado no serviço |
| GET | `/api/v1/batches/{id}` | 200 `BatchResponse` | JWT equipe; escopo validado no serviço |
| POST | `/api/v1/batches` | 201 `BatchResponse` | Administrador ou gerente; escopo validado no serviço |

> Não há `PUT` nem `DELETE` para `/api/v1/batches`.

### Alertas, sugestões e promoções — 18 operações

| Método | Caminho | Sucesso | Acesso |
|---|---|---:|---|
| GET | `/api/v1/alerts?storeId={storeId}` | 200 `List<AlertResponse>` | JWT equipe |
| GET | `/api/v1/alerts/{id}` | 200 `AlertResponse` | JWT equipe |
| POST | `/api/v1/alerts` | 201 `AlertResponse` | Administrador ou gerente |
| POST | `/api/v1/alerts/{id}/resolve` | 200 `AlertResponse` | Administrador ou gerente |
| GET | `/api/v1/suggestions?storeId={storeId}` | 200 `List<SuggestedActionResponse>` | JWT equipe |
| GET | `/api/v1/suggestions/{id}` | 200 `SuggestedActionResponse` | JWT equipe |
| POST | `/api/v1/suggestions` | 201 `SuggestedActionResponse` | Administrador ou gerente |
| POST | `/api/v1/suggestions/{id}/approve` | 200 `SuggestedActionResponse` | Administrador ou gerente |
| POST | `/api/v1/suggestions/{id}/reject` | 200 `SuggestedActionResponse` | Administrador ou gerente |
| GET | `/api/v1/promotions?storeId={storeId}` | 200 `List<PromotionResponse>` | JWT equipe; escopo validado no serviço |
| GET | `/api/v1/promotions/{id}` | 200 `PromotionResponse` | JWT equipe; escopo validado no serviço |
| POST | `/api/v1/promotions` | 201 `PromotionResponse` | Administrador ou gerente; escopo validado no serviço |
| PUT | `/api/v1/promotions/{id}` | 200 `PromotionResponse` | Administrador ou gerente; escopo validado no serviço |
| POST | `/api/v1/promotions/{id}/activate` | 200 `PromotionResponse` | Administrador ou gerente; escopo validado no serviço |
| POST | `/api/v1/promotions/{id}/deactivate` | 200 `PromotionResponse` | Administrador ou gerente; escopo validado no serviço |
| GET | `/api/v1/promotions/{promotionId}/items` | 200 `List<PromotionItemResponse>` | JWT equipe; escopo validado no serviço |
| POST | `/api/v1/promotions/{promotionId}/items` | 201 `PromotionItemResponse` | Administrador ou gerente; escopo validado no serviço |
| DELETE | `/api/v1/promotions/{promotionId}/items/{id}` | 204 sem corpo | Administrador ou gerente; escopo validado no serviço |

### Fidelidade do cliente — 3 operações

| Método | Caminho | Sucesso | Acesso |
|---|---|---:|---|
| GET | `/api/v1/client/loyalty/balance` | 200 `LoyaltyAccountResponse` | JWT cliente (Firebase) |
| GET | `/api/v1/client/loyalty/transactions` | 200 `List<LoyaltyTransactionResponse>` | JWT cliente (Firebase) |
| POST | `/api/v1/client/loyalty/redeem` | 200 sem corpo | JWT cliente (Firebase) |

> Não existem os endpoints `/earn` nem `/rewards` nesta API.

## Convenções HTTP

- Os endpoints de criação que retornam `ResponseEntity.created(...)` respondem `201` e incluem o cabeçalho `Location`.
- Os endpoints de desativação retornam `204 No Content`; não há remoção física documentada nos controllers.
- Endpoints de listagem paginados usam `Page<T>` e aceitam os parâmetros padrão do Spring Data, como `page`, `size` e `sort`.
- As requisições de criação e atualização usam `@Valid`. Erros de validação e de domínio são tratados pelo handler global e expostos como `ApiError` quando aplicável.

## Persistência e componentes

A estrutura atual contém 192 arquivos Java de produção em `com.institutojf.mottainai`, incluindo 19 services, 25 repositories, 32 DTOs de request, 21 DTOs de response e 13 mappers. As migrations preservadas são:

1. `V1__create_extensions.sql`
2. `V2__create_schema_and_types.sql`
3. `V3__create_core_tables.sql`
4. `V4__create_catalog_tables.sql`
5. `V5__create_catalog_indexes.sql`
6. `V6__add_address_uniqueness.sql`
7. `V7__make_product_version_not_null.sql`
8. `V8__enforce_case_insensitive_product_category_name.sql`
9. `V9__add_password_reset_tokens.sql`

## Verificação local

Para compilar, testar e executar a aplicação:

```bash
mvn clean compile
mvn test
mvn spring-boot:run
```

A execução dos testes que carregam contexto exige as variáveis de banco e JWT configuradas. Este documento não declara um total de testes aprovados sem uma execução no ambiente atual.

**Última atualização:** 2026-08-30
