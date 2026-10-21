# Customer profile updates

`PUT /api/v1/client/auth/profile` updates the authenticated Firebase customer's profile. The UID comes from the validated token. The protected SQL contract does not return plaintext name, email, phone or address in `CustomerResponse`.

The Android app obtains display name and email from its authenticated Firebase user. The SQL API remains the source of the customer ID, active state, birth year and marketing consent.

The `phone` request field has these semantics:

- Omitted or JSON `null`: preserve the stored protected phone token.
- Nonblank plaintext phone: replace the stored token using `ProtectedDataToken.phone`.
- Explicitly empty string: remove the stored phone token.

This allows a name/consent update without requiring clients to retrieve or resend protected phone data. `fullName` and `marketingConsent` retain their existing required-field validation. The endpoint and response shape are unchanged.

`CustomerServiceTest` covers preserving an omitted phone, replacing it with a protected token and explicitly removing it. No plaintext phone is returned or stored by this change.
