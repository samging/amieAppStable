# Amie backend and website

The desktop app, the Amie website (Kobweb site on port 8081) and the Amie backend (Spring Boot
`packageRepository`) share one identity: the Keycloak realm `master`, client `amie`. Since 2026-10-14 the
backend accepts Keycloak access tokens directly, so the account you sign in with in the app is the same account
the website shows, and what you publish from the app appears on the website's dashboard.

## Configuration

`AMP/backend.json` (written with defaults on first start; no secrets, so it can be committed):

```json
{
  "baseUrl": "http://192.168.1.114:8080",
  "siteUrl": "http://192.168.1.114:8081"
}
```

`AMIE_BACKEND_URL` and `AMIE_SITE_URL` override the file. `BackendConfig.load()` (`backend/BackendConfig.kt`) is
the single place the addresses come from; nothing else in the app hardcodes a host.

On a machine where Keycloak already listens on 8080, start the backend on another port (it reads
`AMIE_BACKEND_PORT`) and point `baseUrl` there, for example `http://localhost:8090`.

## What talks to the backend

| Code | Endpoint | Token | Used by |
|---|---|---|---|
| `render/templates/Client.kt` `rest()` | `/get-device-status`, `/update-device-status`, `/fetch-endpoints`, `/list-github`, `/list-github-metadata`, `/handle-login-error` | bearer when signed in | device dashboard, endpoints, plugins, device terminal |
| `backend/AmieBackend.kt` | `/me`, `/user-packages`, `/upload` | bearer, required | Models page: backend card and **Publish** |
| `util/DownloadService.kt` | `/list-github` | none | package list text |

`Client.rest()` attaches `Authorization: Bearer <Keycloak access token>` through `AuthState.accessToken()`, which
refreshes the token when it is about to expire. Guests send no token. An absolute URL passed as the slug is used
as is; a path is joined to `baseUrl`. Failures land in `lastErrorMessage` and the dialog flags; only failures the
server answered are reported to `/handle-login-error`.

**Devices** are stored on the backend per username. `AppBaseTemplate` calls `DeviceManager.setSession` with the
Keycloak username whenever the session changes, so the backend sees the real user instead of a guest id. When the
backend is unreachable the local `AMP/componentSettings.json` is kept (it used to be overwritten with `{}`).

## Models page

The **AMIE BACKEND** card on the Models page calls `/me` with the session's token and shows:

- the backend address and whether it accepted the token (green), answered but refused it (red, with the reason),
  or is unreachable;
- the backend's profile for the user and a link to the website's profile page;
- the files the backend stores for the user (`/user-packages`, the same list as the website's dashboard);
- **Open website**, which opens `siteUrl/dashboard` in the browser.

**Publish** on an expanded model uploads the module's `model.onnx` and `module.json` through `POST /upload` with
`progLanguage=onnx`, which the backend stores as `uploads/<user>/onnx/<file>` in its GitHub repository. The backend
needs a `GITHUB_TOKEN` for that; without one it answers "Error uploading file" and the app shows that message.

## Backend side (Amie repository)

`packageRepository` validates bearer tokens in `UserService.validateToken`: first as a Keycloak token
(`KeycloakTokenVerifier`: RS256 signature against the realm's JWKS, issuer check, expiry), then as one of its own
HS512 login tokens. A Keycloak user gets a `users` row on first contact. Properties:

```properties
server.port=${AMIE_BACKEND_PORT:8080}
amie.keycloak.issuer=${KEYCLOAK_ISSUER:http://192.168.1.114:8080/realms/master}
amie.keycloak.jwks-uri=${KEYCLOAK_JWKS_URI:http://localhost:8080/realms/master/protocol/openid-connect/certs}
```

The issuer must equal the `iss` claim Keycloak writes, which is the realm's **frontend URL** plus `/realms/master`.
The JWKS URI is where the backend process can reach Keycloak, which may be a different host. If the realm's
frontend URL changes, change `KEYCLOAK_ISSUER` with it.

New endpoint `GET /me` (bearer) returns `{id, username, fullName, email, bio}` for the token's user; `GET /profile`
returns the same shape and no longer includes the password hash.

## Website side

The site reads its addresses from `config/AmieConfig.kt`: backend and Keycloak default to the host the page was
loaded from on port 8080, overridable per browser with `localStorage.setItem("amie.backendUrl", ...)` and
`localStorage.setItem("amie.keycloakUrl", ...)`. The Keycloak token exchange moved to the site's server
(`/api/keycloak/token`), which adds the client secret from `KEYCLOAK_CLIENT_SECRET`, so the secret is no longer in
browser JavaScript and the password login on the site works again. The server also reads `KEYCLOAK_URL` and
`AMIE_SITE_URL` for the redirect URI.
