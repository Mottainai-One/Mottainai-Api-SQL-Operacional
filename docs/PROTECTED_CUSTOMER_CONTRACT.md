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

`birthYear` é opcional. `fullName`, `cpf`, `email`, `phone`, `birthDate` e `address` não fazem parte da resposta. O banco guarda tokens irreversíveis dos dados pessoais e apenas o ano de nascimento. A API não devolve esses tokens como se fossem os valores originais e não tenta reconstruir os dados.

## Adaptação dos consumidores

Esta é uma alteração incompatível do contrato de resposta em `/api/v1/customers` e nas rotas de perfil de cliente ainda presentes nesta branch. Antes de publicar:

- Atualizar os tipos e validadores do site, PDV e aplicativo para o contrato acima.
- Usar `id` como identificador operacional; não depender de nome, CPF, e-mail ou telefone na resposta para renderizar listas.
- Não preencher formulários de edição com valores ausentes e reenviá-los como se fossem os dados originais. As requisições de criação/edição mantêm seu próprio contrato de entrada.
- Não derivar uma data de nascimento completa de `birthYear`.
- Se uma tela exigir dados pessoais legíveis, definir com produto e dados uma fonte autorizada que os disponibilize. Esse requisito não pode ser atendido revertendo SHA-256.

Este repositório contém a API e seus testes. As alterações nos consumidores externos devem ser verificadas nos respectivos repositórios antes da publicação; os testes desta API não comprovam essa adaptação.

## Recuperação de senha do cliente nesta branch

A autenticação SQL de cliente ainda existe nesta branch. A solicitação usa o e-mail informado para localizar o token protegido correspondente e para enviar o link; não precisa obter o e-mail original do banco. O token temporário de recuperação é independente dos tokens de dados pessoais, tem prazo de 15 minutos e é invalidado após o uso. A resposta da solicitação é `204` sem corpo para conta existente, inexistente ou inativa e para falha de envio. Falhas de envio devem ser investigadas internamente, sem revelar ao solicitante quais contas existem.

Essas rotas de cliente serão removidas na separação das APIs. Essa correção não adiciona recuperação de senha ao Firebase operacional.
