# Protected customer schema verification

`ProtectedCustomerSchemaTest` checks the actual supplied PostgreSQL installer
`mottainai_operational_complete_2026-10-11.txt`, not application migrations or
Hibernate-generated tables. It is opt-in and refuses application database names.
The test uses Hibernate `validate`, persists/reads/updates protected customers,
stores BCrypt customer password/recovery hashes, checks rejection of plaintext
personal data and invalid birth years, and compares Java tokens with PostgreSQL
normalization. Every write transaction is rolled back. This does not exercise
application-role RLS permissions or verify every operational entity.

Use a disposable PostgreSQL 15 container, bound to loopback, with database
`protected_contract`. Copy the supplied installer into the container and run it
with `psql -v ON_ERROR_STOP=1 -f`. Do not apply it to an existing application database.
For a local disposable instance using trust authentication, run in PowerShell:

```powershell
$env:PROTECTED_SCHEMA_TEST_URL='jdbc:postgresql://127.0.0.1:PORT/protected_contract'
$env:PROTECTED_SCHEMA_TEST_USER='postgres'
$env:PROTECTED_SCHEMA_TEST_PASSWORD=''
.\mvnw.cmd '-Dtest=ProtectedDataTokenTest,CustomerServiceTest,ProtectedCustomerSchemaTest' test
```

Replace `PORT` with the port assigned to that disposable container. Keep any real
database credentials outside version control. Remove the disposable container
after verification.

## Installer evidence

- Lines 4487–4505: names/UIDs use `btrim`, e-mail uses `lower(btrim(...))`,
  CPF/phone remove non-digits, and values already matching their token prefix are
  not hashed again. SHA-256 hex output is uppercase.
- Lines 4523–4535: address hashing uses `concat_ws('|', ...)`, preserving spaces
  and empty strings while skipping SQL `NULL`; the address link is cleared.
- Lines 4618–4641: customer columns require typed tokens, `birth_year` is between
  1900 and the current year, `address_id` is null, and authentication password/
  recovery hashes are BCrypt. Personal-data SHA-256 tokens and recovery-token
  BCrypt hashes are different contracts.

## Staff schema contract and remaining SQL work

Staff PII is stored in `employee_private_data` as AES-256-GCM ciphertext and
nonce `BYTEA` pairs with masks and key metadata. Staff lookup uses verified
Firebase UID; the API does not use staff HMAC indexes. The expected final
contract and deployment dependency are documented in
[STAFF_AES_GCM_SCHEMA_CONTRACT.md](STAFF_AES_GCM_SCHEMA_CONTRACT.md). This
customer schema test does not verify staff writes, authentication, or the staff
SQL contract from PR #34.
