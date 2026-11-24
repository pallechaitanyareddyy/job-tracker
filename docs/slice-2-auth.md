# Slice 2: authentication (register, login, refresh, logout)

| Method | Path | Auth | What it does |
| --- | --- | --- | --- |
| POST | /api/v1/auth/register | public | Creates an account. Only a BCrypt hash of the password is stored. |
| POST | /api/v1/auth/login | public | 15-minute access token (JWT) in the body, 7-day refresh token in an HttpOnly cookie. |
| POST | /api/v1/auth/refresh | refresh cookie | Swaps the cookie token for a new access token and a new cookie token. Each refresh token works once. |
| POST | /api/v1/auth/logout | refresh cookie | Revokes the token and deletes the cookie. Always returns 204. |
| GET | /api/v1/users/profile | Bearer token | The logged-in user. The id comes from the token, never from the request. |

## Reuse detection
If a refresh token that was already used shows up again, it may have been stolen. All sessions of that user are ended
and they must log in again. `RefreshTokenService.rotate` uses `noRollbackFor` so this revocation is saved even though
the call ends in a 401. The "claim" of a token is one atomic UPDATE, so two simultaneous requests cannot both win.

## Environment variables
| Name | Required | Purpose |
| --- | --- | --- |
| DB_PASSWORD | yes | PostgreSQL password |
| JWT_SECRET | yes | Access-token signing key, at least 32 characters. Never commit it. |
| COOKIE_SECURE | no | `true` once the app runs on https. |
