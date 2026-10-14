# API para o aplicativo Cliente

Todas as rotas abaixo usam `/api/v1`. Envie `Authorization: Bearer <accessToken>` obtido em `POST /customers/auth/login` para consultar o catálogo e os dados do cliente. As rotas de catálogo são exclusivas para tokens com papel `CUSTOMER`.

## Conta

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
    "id": 11,
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
    "id": 11,
    "zipCode": "01001000", "street": "Rua Um", "number": "10",
    "complement": null, "neighborhood": "Centro", "city": "São Paulo", "state": "SP"
  }, "latitude": -23.550520, "longitude": -46.633308 },
  "items": [{ "id": 12, "productId": 7, "name": "Arroz", "originalPrice": 12.50,
    "promotionalPrice": 9.90, "quantityAvailable": 5.000 }]
}
```

## Pontos e erros

Consulte o saldo em `GET /customers/{id}/loyalty` e o histórico em `GET /customers/{id}/loyalty/transactions?from=2026-10-01T00:00:00&to=2026-10-31T23:59:59`. `from` e `to` são obrigatórios; o intervalo não pode ultrapassar seis meses. O histórico retorna uma lista sem paginação, e `points` já traz o sinal do lançamento. A API verifica a titularidade. Nas novas listagens de catálogo, `page` começa em zero e `size` padrão é 20, aceitando 1 a 100. Filtros inválidos retornam `400`; ausência de credenciais, `401`; papel sem acesso, `403`; detalhe indisponível, `404`. As datas de promoções são horários locais do servidor, sem sufixo UTC; o Android não deve assumir fuso no parse. Não há URL de foto no contrato; use placeholder.
