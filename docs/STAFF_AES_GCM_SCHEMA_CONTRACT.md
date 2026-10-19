# Staff PII contract — operational database PR #34

This API targets the final schema from `Mottainai-Banco-Operacional` PR #34.
The schema owner is the data team; this document records the contract expected
by the API and does not replace or modify the database installer.

## Employee personal data

`mottainai.employee` and `mottainai.app_user` do not contain staff name, CPF,
email, or phone in plaintext. Those values are stored only in
`mottainai.employee_private_data`, one row per employee. `employee.cpf_lookup_hmac`
stores only a keyed HMAC-SHA-256 fingerprint so the API can check CPF duplicates
through the database index without decrypting every employee row.
Each value has a `BYTEA` ciphertext, a separate `BYTEA` nonce, and a masked
display value. The row also stores `key_version`, `algorithm` (`AES-256-GCM`),
`protection_state` (`MIGRATION_PENDING` or `ENCRYPTED`), `encrypted_at`, and
creation/update timestamps. The API writes and reads these fields as binary
AES-256-GCM envelopes; ciphertext includes the authentication tag and nonce is
12 bytes. Field-specific AAD prevents ciphertexts being swapped between
attributes. The AAD is the UTF-8 encoding of the exact value below (no
terminating NUL or newline); data migration/encryption tooling must use the same
bytes:

| Protected value | Exact AAD string |
| --- | --- |
| `employee_private_data.name_ciphertext` | `mottainai:operational:employee.name` |
| `employee_private_data.cpf_ciphertext` | `mottainai:operational:staff.cpf` |
| `employee_private_data.email_ciphertext` | `mottainai:operational:employee.email` |
| `employee_private_data.phone_ciphertext` | `mottainai:operational:employee.phone` |

`cpf_masked` is for display only and is never used as an identity. The HMAC is
not reversible and must be calculated from the normalized 11-digit CPF.

CPF duplicate checks use `employee.cpf_lookup_hmac` and its unique database
index. Email duplicate checks still decrypt stored rows visible to the caller's
RLS context; email lookup is unchanged.

## Staff identity and authentication

The operational app authenticates with Firebase first. The API verifies the
Firebase ID token's signature, issuer, audience, expiry, and subject, then uses
that verified UID to resolve `app_user.firebase_uid`. It does not accept an
arbitrary client-supplied UID as identity and never authenticates staff by
email, CPF, or their masks. The API's separate password is checked against the
schema's retained BCrypt `password_hash`.

`fn_bootstrap_staff_context(TEXT,TEXT)` is called only with the verified
Firebase UID and lookup type `FIREBASE_UID`. Active accounts must have a
Firebase UID. Pending invitations may have no UID. Invitation completion
requires both the invitation token and a verified Firebase ID token; that
verified subject is linked to the pending account before activation. The
operational API does not expose password recovery. Customer password recovery
is a separate customer-app flow.

## Runtime key

Configure `STAFF_DATA_ENCRYPTION_KEY` as Base64 encoding exactly 32 random bytes
and `STAFF_DATA_KEY_VERSION` as a positive `SMALLINT` version matching the
database contract. Configure `STAFF_DATA_LOOKUP_KEY` as a separate Base64
encoding of 32 random bytes. Keep both keys outside source control and the
database. Keep the lookup key stable; changing it requires recalculating the
stored HMAC fingerprints before duplicate checks can work again.
Losing the encryption key makes existing encrypted values unrecoverable;
rotation requires coordinated re-encryption and key-version handling.

Generate a local development key with PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

## Schema rollout dependency

The API expects the PR #34 schema to be installed, including the private-data
table, removed legacy PII/HMAC columns, `FIREBASE_UID` bootstrap, active-user
UID constraint, and nullable UID for pending invitations. Existing irreversible
hashes cannot be decrypted into AES-GCM ciphertext. The data team must complete
any required recovery/backfill using an authorized source before deploying the
API; do not mix this contract with an earlier installer variant.

The release gate is the database installer, not an API-side migration: apply
the installer version that includes `28_reversible_employee_pii.sql` and
`29_finalize_reversible_employee_pii.sql`, then run the database compatibility
tests before deploying this API. The first migration creates the private-data
table and its tenant RLS policies; the finalization migration checks that
protected rows and Firebase links are ready before removing legacy data. Do
not deploy this API if that database PR/version is absent or its tests fail.

Hibernate `ddl-auto=validate` makes the API refuse startup when mapped tables
or columns are missing, including `employee_private_data`. Hibernate does not
check RLS behavior, so this startup check is not a substitute for the database
tests. Those tests must use the actual non-privileged API runtime login (a
member of `mottainai_api`, without superuser or `BYPASSRLS`) and verify that the
table has both RLS enabled and forced, and that reads/writes are restricted to
the company in `fn_get_current_company_id()`. The database contract provides
`employee_private_data_tenant_policy` for `mottainai_api`; the legacy-import
policy is for the migration role only. An RLS test performed as the schema
owner, superuser, or migration role does not prove the API's tenant isolation.
