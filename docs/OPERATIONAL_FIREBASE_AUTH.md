# Firebase authentication for the operational API

The operational app authenticates with Firebase first, then sends the Firebase
ID token and the API password to `POST /api/v1/auth/login`. The API verifies
the ID token against the configured Firebase project and uses its verified
`sub` (UID) to find the corresponding `app_user`; it never trusts a UID supplied
separately by the client. The API password is checked against the retained
BCrypt `app_user.password_hash`.

Active staff accounts must have a Firebase UID. An invitation may be created
before a Firebase account exists, so its `app_user.firebase_uid` remains null
until the employee completes onboarding. The invitation completion request
must include both the one-time invitation token and a Firebase ID token; the
API validates the token and links that verified UID before activating the
account. A UID already linked to another app user is rejected.

After the login exchange, protected operational API calls must use the
API-issued access token. The API derives all effective authorities from the
employee's active database role; Firebase custom role claims are not used for
authorization. A Firebase ID token is accepted only in the login and invitation
completion payloads, where the API validates its signature, issuer, audience,
and expiry and derives the UID from its verified subject.

Set `FIREBASE_PROJECT_ID` to the Firebase project's Project ID. The API uses
Google's published signing keys; do not put service-account credentials or
private keys in the client or repository. The operational API does not provide
password recovery. Customer account recovery belongs to the separate customer
API.

## Staff invitation and locked-account support

Only an administrator can create an employee invitation or resend one with
`POST /api/v1/employees/{id}/invite`. A store manager who needs to onboard a
new employee, or resend an expired/undelivered invitation, must ask the
company administrator. The endpoint only accepts an employee who is inactive
and has not set an API password.

An active employee who forgot the API password or cannot sign in cannot reset
it through this API. The manager should escalate the case to the administrator
or the support person responsible for staff accounts. Do not deactivate the
employee to force a new invitation: invitations are only for pending accounts,
and deactivation is an access-control action that can interrupt store work.

The staff AES-256-GCM storage and exact SQL expectations are in
[STAFF_AES_GCM_SCHEMA_CONTRACT.md](STAFF_AES_GCM_SCHEMA_CONTRACT.md).
