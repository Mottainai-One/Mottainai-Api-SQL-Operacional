# API para o aplicativo Cliente

Todas as rotas abaixo usam `/api/v1`. Envie `Authorization: Bearer <firebaseIdToken>` nas rotas `/client/**` e `/customer-catalog/**`. A API valida o token Firebase com o mesmo projeto configurado no backend; o UID é obtido do token e não deve ser enviado pelo app como identidade. Não é necessário incluir um papel `CUSTOMER` no token. As rotas legadas `/customers/**` continuam usando a autenticação própria da API descrita na seção Conta.

## Conta

### Firebase (aplicativo Cliente)

Depois de criar a conta/autenticar com o Firebase Authentication, envie o ID token no header `Authorization: Bearer <firebaseIdToken>` e crie o perfil operacional com `POST /client/auth/profile`. O e-mail e o UID são extraídos do token Firebase validado; não envie nem armazene senha na API.

Pré-requisito de banco: `mottainai.customer.external_auth_uid` deve ser anulável e único quando preenchido, para que um UID Firebase não seja associado a dois clientes. Confira/aplique essa alteração no repositório do banco antes de publicar a API.

```json
{
  "fullName": "Ana Silva",
  "cpf": "12345678909",
  "phone": "11999999999",
  "birthDate": "2000-01-20",
  "marketingConsent": false,
  "address": {
    "zipCode": "01001000", "street": "Rua Um", "number": "10",
    "complement": null, "neighborhood": "Centro", "city": "São Paulo", "state": "SP"
  }
}
```

A API retorna `201` com o perfil e seu `id`; também cria a conta de pontos com saldo inicial zero. Uma repetição para o mesmo UID retorna o perfil já vinculado, sem duplicar cliente ou conta de pontos. CPF ou e-mail já cadastrado para outra identidade retorna `409`. `GET /client/auth/profile` retorna o perfil e o ID. Para operações do app Cliente, não envie esse ID: os endpoints resolvem o cliente pelo UID verificado no token.

`PUT /client/auth/profile` atualiza somente os campos editáveis. CPF e e-mail vêm da identidade Firebase e não são alterados por essa rota.

```json
{ "fullName": "Ana Silva", "phone": "11999999999", "marketingConsent": true }
```

O retorno é `200` com o perfil atualizado. Se ainda não houver perfil SQL vinculado ao UID, a API retorna `404`.

### Autenticação SQL legada

`POST /customers` cria conta e retorna `201`, sem autenticar. `fullName`, `cpf` (11 dígitos), `email` e `password` (mínimo 8 caracteres) são obrigatórios; `phone`, `birthDate`, `marketingConsent` e `address` são opcionais no contrato atual. Quando `address` for enviado, seus campos obrigatórios devem estar completos, inclusive CEP de 8 dígitos sem hífen. `POST /customers/auth/login` recebe `{ "email": "cliente@exemplo.com", "password": "Senha@123" }` e retorna `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600 }`. Depois, `GET /customers/auth/profile` retorna o perfil com `id` para consultas de pontos. Não há refresh token de cliente: ao receber `401`, encerre a sessão e solicite novo login.

## Lojas

`GET /customer-catalog/stores?page=0&size=20&query=mercado` retorna uma página Spring (`content`, `number`, `size`, `totalElements`, `totalPages`, `last`). `query` procura no nome da loja, sem diferenciar maiúsculas e minúsculas. Para proximidade, acrescente **juntos** `latitude=-23.55&longitude=-46.63&radiusKm=10`. Os limites são latitude ±90, longitude ±180 e raio maior que zero até 500 km. Sem esses parâmetros, lojas sem coordenadas continuam na listagem. Ordenação: nome e ID crescentes.

`GET /customer-catalog/stores/{id}` retorna uma loja ativa ou `404`.

Exemplo de loja:

```json
{
  "id": 3,
  "name": "Mercado Centro",
  "address": {
    "zipCode": "01001000", "street": "Rua Um", "number": "10",
    "complement": null, "neighborhood": "Centro", "city": "São Paulo", "state": "SP"
  },
  "latitude": -23.550520,
  "longitude": -46.633308
}
```

## Promoções

`GET /customer-catalog/promotions?page=0&size=20&storeId=3&query=arroz` retorna uma página; `storeId` e `query` são opcionais. `query` procura no título. Ordenação: início e ID decrescentes. `GET /customer-catalog/promotions/{id}` retorna o detalhe ou `404`. Apenas promoções aprovadas, ativas, vigentes, de lojas ativas e com ao menos um item disponível aparecem. Itens sem estoque ou produtos inativos não são enviados.

Exemplo de promoção:

```json
{
  "id": 9, "name": "Oferta de hoje", "description": "Arroz em promoção",
  "promotionType": "SPECIAL_PRICE", "startsAt": "2026-10-09T09:00:00",
  "endsAt": "2026-10-10T18:00:00",
  "store": { "id": 3, "name": "Mercado Centro", "address": {
    "zipCode": "01001000", "street": "Rua Um", "number": "10",
    "complement": null, "neighborhood": "Centro", "city": "São Paulo", "state": "SP"
  }, "latitude": -23.550520, "longitude": -46.633308 },
  "items": [{ "id": 12, "productId": 7, "name": "Arroz", "originalPrice": 12.50,
    "promotionalPrice": 9.90, "quantityAvailable": 5.000 }]
}
```

## Pontos e erros

No app Cliente, consulte o saldo em `GET /client/loyalty/balance` e o histórico em `GET /client/loyalty/transactions?from=2026-10-01T00:00:00&to=2026-10-31T23:59:59`. Não envie `customerId`: a API resolve o cliente pelo UID do token. `from` e `to` são obrigatórios; o intervalo não pode ultrapassar seis meses. O histórico retorna uma lista sem paginação, e `points` já traz o sinal do lançamento.

As rotas legadas `GET /customers/{id}/loyalty` e `/transactions` continuam disponíveis para clientes autenticados pelo JWT próprio da API. Nas listagens de catálogo, `page` começa em zero e `size` padrão é 20, aceitando 1 a 100. Filtros inválidos retornam `400`; ausência de credenciais, `401`; método de escrita no catálogo, `403`; detalhe indisponível, `404`. As datas de promoções são horários locais do servidor, sem sufixo UTC; o Android não deve assumir fuso no parse. Não há URL de foto no contrato; use placeholder.
