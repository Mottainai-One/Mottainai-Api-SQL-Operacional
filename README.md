# Mottainai API SQL Operacional

[![CI](https://github.com/Mottainai-One/Mottainai-Api-SQL-Operacional/actions/workflows/ci.yml/badge.svg)](https://github.com/Mottainai-One/Mottainai-Spring-Api-SQL/actions/workflows/ci.yml)

REST API for the Mottainai ecosystem, built with Spring Boot to manage business rules, authentication, inventory operations, predictive workflows, and integrations with databases, mobile applications, AI services, and BI platforms.

## CI/CD

Este repositório possui um pipeline de CI/CD que executa as seguintes verificações:

O workflow `.github/workflows/ci.yml` executa em pull requests destinados a `main`/`master` e em pushes nessas branches.

| Job | Descrição | Bloqueante |
|-----|-----------|------------|
| Build e Testes | Build Maven + testes em ordem aleatória | ✅ |
| Estilo de Código | Verificar indentação Java (2 espaços) | ✅ |
| Convenção de Commits | Verificar padrão de commits | ✅ |
| Varredura de Segredos | Gitleaks para detectar segredos | ✅ |
| Idioma dos Comentários | Verificar se comentários fazem sentido | ⚠️ |
| Idioma do Código | Verificar código em inglês | ⚠️ |
| Dependências | Verificar dependências desatualizadas | ⚠️ |
| Migrations Flyway | Verificar rollback em migrations | ✅ |
| Verificação Swagger | Verificar se OpenAPI está atualizado | ⚠️ |
| Código Não Utilizado | Verificar imports não utilizados | ⚠️ |
| Proteção .env | Verificar se .env nunca foi commitado | ✅ |

## Padrões de Commit

```
feat: nova funcionalidade
fix: correção de bug
docs: documentação
style: formatação
refactor: refatoração
test: testes
chore: manutenção
ci: configuração de CI
perf: melhoria de performance
build: dependências
revert: reversão de commit
```

## Desenvolvimento

### Pré-requisitos

- Java 21
- Maven 3.8+

### Executar localmente

```bash
./mvnw clean install
```

### Executar testes

```bash
./mvnw test
```

### Build

```bash
./mvnw clean package
```
