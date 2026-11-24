# Slice 2a: register and login

## What it adds

| Method | Path | Auth | What it does |
| --- | --- | --- | --- |
| POST | /api/v1/auth/register | public | Creates an account. The password is stored only as a BCrypt hash. |
| POST | /api/v1/auth/login | public | Returns a 15-minute access token (JWT) in the body and sets a 7-day refresh token in an HttpOnly cookie. |
| GET | /api/v1/users/profile | Bearer token | Returns the logged-in user. The user id comes from the token, never from the request. |

Not yet built (slice 2b): `/auth/refresh` (token rotation) and `/auth/logout`.

## Environment variables

| Name | Required | Purpose |
| --- | --- | --- |
| DB_PASSWORD | yes | PostgreSQL password |
| JWT_SECRET | yes | Signing key for access tokens, at least 32 characters. Never commit it. |
| COOKIE_SECURE | no | `true` once the app runs on https. Default `false` for http://localhost. |

Generate a JWT secret in PowerShell:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

## Try it in PowerShell

```powershell
$register = @{ email = "chaitu@example.com"; password = "Passw0rd!123"; fullName = "Chaitu" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register -ContentType "application/json" -Body $register

$login = @{ email = "chaitu@example.com"; password = "Passw0rd!123" } | ConvertTo-Json
$result = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json" -Body $login -SessionVariable session

Invoke-RestMethod -Uri http://localhost:8080/api/v1/users/profile -Headers @{ Authorization = "Bearer $($result.accessToken)" }
```
