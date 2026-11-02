# Contrato de cliente no schema protegido

As respostas que utilizam `CustomerResponse` contêm somente:

```json
{
  "id": 7,
  "birthYear": 2000,
  "active": true,
  "marketingConsent": false,
  "createdAt": "2026-10-01T10:00:00",
  "updatedAt": "2026-10-01T10:00:00"
}
```

`birthYear` é opcional. `fullName`, `cpf`, `email`, `phone`, `birthDate` e `address` não fazem parte da resposta geral. Os tokens existentes na tabela `customer` continuam para compatibilidade. A API grava os valores originais em um sidecar criptografado AES-256-GCM e os disponibiliza apenas a Administradores em `GET /api/v1/customers/{id}/private-data`.

O instalador operacional recebido ainda não contém esse sidecar. Antes de usar esta API, o banco precisa criar `mottainai.customer_private_data` com: `customer_id INTEGER PRIMARY KEY REFERENCES mottainai.customer(customer_id) ON DELETE CASCADE`, `payload_ciphertext BYTEA NOT NULL CHECK (octet_length(payload_ciphertext) >= 17)`, `payload_nonce BYTEA NOT NULL CHECK (octet_length(payload_nonce) = 12)`, `key_version SMALLINT NOT NULL CHECK (key_version > 0)`, `created_at TIMESTAMP NOT NULL`, e `updated_at TIMESTAMP NOT NULL`. A role da API precisa de `SELECT`, `INSERT`, `UPDATE` e `DELETE` conforme as políticas RLS aplicáveis; as chaves ficam apenas no secret manager/ambiente da API, nunca no banco.

O ciphertext armazena um JSON com nome, CPF, e-mail, telefone, data de nascimento e endereço completo. Cada registro usa nonce aleatório de 12 bytes, tag GCM de 128 bits e AAD UTF-8 `mottainai:operational:customer.personal_data:<customer_id>` para impedir troca de envelopes entre clientes. Configure a chave independente de 32 bytes em Base64 por `CUSTOMER_DATA_ENCRYPTION_KEY` e sua versão positiva em `CUSTOMER_DATA_KEY_VERSION`. Durante rotação, mantenha chaves antigas em `CUSTOMER_DATA_DECRYPTION_KEYS` (`versao:chaveBase64`, separadas por vírgulas); a API usa o `key_version` de cada registro. Na desativação, a API mantém o ciphertext, mas bloqueia sua leitura pela rota. Na anonimização, apaga o sidecar e remove os tokens dos dados de `customer`.

A tabela `customer` do instalador não possui `company_id` nem vínculo direto com empresa/tenant. Por isso, a API restringe essa leitura ao Administrador global, sem afirmar isolamento por empresa. O acesso por Gerentes só deve ser habilitado depois de o modelo fornecer e validar esse vínculo em cada consulta.

## Consumidores

As respostas atuais de `CustomerResponse` seguem sem PII; telas que precisarem dos dados originais devem usar a rota protegida separada. Essa rota depende da criação do sidecar e da configuração da chave descritos acima. Os tokens SHA-256 continuam irreversíveis e não são usados como se fossem os valores originais.

Este repositório contém a API e seus testes. As alterações nos consumidores externos devem ser verificadas nos respectivos repositórios antes da publicação; os testes desta API não comprovam essa adaptação.

## Recuperação de senha do cliente nesta branch

A autenticação SQL de cliente ainda existe nesta branch. A solicitação usa o e-mail informado para localizar o token protegido correspondente e para enviar o link; não precisa obter o e-mail original do banco. O token temporário de recuperação é independente dos tokens de dados pessoais, tem prazo de 15 minutos e é invalidado após o uso. A resposta da solicitação é `204` sem corpo para conta existente, inexistente ou inativa e para falha de envio. Falhas de envio devem ser investigadas internamente, sem revelar ao solicitante quais contas existem.

Essas rotas de cliente serão removidas na separação das APIs. Essa correção não adiciona recuperação de senha ao Firebase operacional.
