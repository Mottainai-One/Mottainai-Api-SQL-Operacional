# Firebase authentication for the operational mobile app

The operational Android app signs in with Firebase Email/Password and sends its Firebase ID token as `Authorization: Bearer <id-token>`. The API validates Firebase tokens for the configured project and continues to accept the API-issued JWT for the website. `POST /api/v1/auth/login` remains the CPF/password login for the website.

## Firebase token contract

The token must contain a verified email and the existing custom claim `role`:

| Firebase `role` | API authority |
| --- | --- |
| `dono` | `ADMINISTRATOR`, `MANAGER` |
| `gerente` | `MANAGER` |
| `estoquista` | `OPERATOR` |

The app's `storeID` claim is not used to grant data access. The API resolves the active employee, store, and company from the relational database using the verified email. This keeps store/company scope owned by SQL while permissions for this Firebase-authenticated mobile request come from the signed Firebase role claim.

## First login and account linking

The employee must already exist as an active `app_user` linked to an active employee, store, and company, with the same email as the verified Firebase account. On the first authorized request, the API stores the verified Firebase UID in `app_user.firebase_uid`. Later requests require both the same UID and email match. One Firebase UID can link to only one staff account.

Before deploying the API, apply `V11__link_firebase_uids_to_staff.sql` to the relational database. Set `FIREBASE_PROJECT_ID` in the API environment to the Firebase project's **Project ID**. Do not put service-account credentials or private keys in the app or repository; the API verifies tokens using Google's published signing keys.

Firebase custom-claim changes become effective for API requests when the client refreshes its ID token; after changing a staff role, force an ID-token refresh in the mobile app before retrying a protected action.

## Request example

```http
GET /api/v1/auth/profile
Authorization: Bearer <firebase-id-token>
```

Use the Firebase ID token for operational mobile API requests. The website continues to use the access token returned by `/api/v1/auth/login`.

The mobile app signs out and changes its password through Firebase. The API's `/api/v1/auth/logout` and `/api/v1/auth/password` endpoints remain for the website's API-issued JWT sessions.
