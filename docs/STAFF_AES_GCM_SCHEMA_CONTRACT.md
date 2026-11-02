# Staff PII contract — operational database PR #34

This document records the API contract for encrypted staff data. The supplied
`mottainai_operational_complete_2026-10-11.txt` installer does not create
`employee_private_data`, so the API cannot run against that installer unchanged.
The table is a necessary future database change; this document does not modify
the installer.

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
not reversible and is calculated from the normalized 11-digit CPF.

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
operational API does not expose password recovery. Customer password recovery routes have been retired from this API.

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

The API expects a future schema update to add the private-data table, while
retaining the installer's token columns and `employee.cpf_lookup_hmac`. The API
uses that HMAC column for CPF duplicate checks. The installer also makes
`employee.name` and `employee.cpf` required protected tokens, and requires
`app_user.email` and `app_user.cpf`; the API writes those fields. Existing
irreversible hashes cannot be decrypted into AES-GCM ciphertext. Active
personal data must come from an authorized source before it is stored in the
future encrypted table.
