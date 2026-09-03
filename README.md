# Seu Barbeiro

A booking platform for barbershops: clients browse shops, view services and pricing,
and book an appointment with a barber (Pix, card, or cash); barbers and shop owners
manage their side from the same app. Spring Boot API + a single Expo/React Native
codebase that ships to iOS, Android, and web.

## Architecture

```
┌─────────────────────────┐        REST + JWT        ┌──────────────────────────┐
│   mobile/  (Expo app)    │ ───────────────────────► │   Spring Boot API (root)  │
│  iOS · Android · Web     │ ◄─────────────────────── │   :8080                   │
└─────────────────────────┘                            └────────────┬─────────────┘
                                                                      │
                                                                      ▼
                                                              PostgreSQL :5432
```

One backend, one frontend codebase. The frontend talks to the backend over plain HTTP
with a Bearer JWT — no BFF, no GraphQL layer.

## Stack

- **Backend**: Java 21, Spring Boot 3.4 (Web, Security, Data JPA, Validation), Flyway
  migrations, PostgreSQL, JWT auth (`jjwt`), Pix QR-code generation (`zxing`), Gradle.
- **Frontend**: Expo SDK 57 (React 19 / React Native 0.86), Expo Router (file-based
  routing), TypeScript, `expo-secure-store` (native) / `localStorage` (web) for session
  persistence, `react-native-svg`, `@expo-google-fonts/oswald`.

## Repo structure

```
src/main/java/com/two_m/yourbarber/   Spring Boot app — see AGENTS.md for the layering
src/main/resources/db/migration/      Flyway SQL migrations
src/test/java/...                     Backend tests (JUnit + Mockito)
mobile/                                Expo app — see mobile/README.md for details
AGENTS.md                              Conventions, gotchas, and how-to for agents/devs
```

## Getting started

You need Postgres and the backend running before the mobile app can do anything real.

### 1. Postgres

No dev `docker-compose.yml` is checked in (only a prod one that also builds the app
image). The simplest local setup, matching the backend's defaults:

```sh
docker run -d --name seu-barbeiro-db \
  -e POSTGRES_DB=yourbarber \
  -e POSTGRES_USER=myuser \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:16-alpine
```

Reuse it later with `docker start seu-barbeiro-db`.

### 2. Backend

```sh
./gradlew bootRun
```

Flyway runs migrations automatically on boot. Listens on `:8080`. An admin account is
auto-provisioned from `admin.default-email` / `admin.default-password` in
`application.properties` (override via `ADMIN_EMAIL` / `ADMIN_PASSWORD` env vars).

New accounts must confirm a 6-digit code emailed to them before they can log in. Set
`MAIL_USERNAME` / `MAIL_PASSWORD` (and `MAIL_HOST` / `MAIL_PORT` if not using Gmail's
SMTP) so the backend can actually send that email — without them, registration still
succeeds but the confirmation email will fail to send (visible in the backend logs),
and `POST /api/auth/resend-code` can retry once mail is configured.

Copy `.env.example` to `.env` and fill in real values (a Gmail **App password**, not
your normal password, if using Gmail — see the comments in the file). Both `docker
compose` and `./gradlew bootRun` pick up `.env` automatically (see
`spring.config.import` in `application.properties`) — just edit `.env` and (re)start:

```sh
./gradlew bootRun
```

**The backend does not hot-reload.** If you're running it from an IDE, editing a
`.java` file and recompiling does nothing until you stop and restart that run — see
`AGENTS.md` for why.

### 3. Mobile app

```sh
cd mobile
npm install
npm start
```

Then **`w`** for web (fastest way to just look at it), **`i`**/**`a`** for a simulator,
or scan the QR with Expo Go on a physical device. See `mobile/README.md` for the
`EXPO_PUBLIC_API_URL` LAN-IP note (Android emulator and physical devices can't reach
`localhost` on your machine directly).

## What's implemented

**Auth** (all three roles register/login through the same flow):
Login → role picker (Cliente / Barbeiro / Dono Barbearia) → registration form →
confirmation-code screen (UI-only mock, no backend call) → for Dono Barbearia only, a
shop-creation request (needs admin approval before the shop is real).

**Client**: browse all barbershops → shop detail (services with pricing, barbers) →
booking wizard (service → barber → date/time → payment method) → Pix QR / copy-paste
screen when paying by Pix → "Meus Agendamentos" (view/cancel).

**Barber / shop owner**: their own appointments with status actions
(confirm/complete/cancel), profile edit (name/phone/Pix key), and — for owners — a shop
card with an accepting-barbers toggle.

## Known gaps (deliberate, not oversights)

- Email confirmation is a UI-only mock; there's no verify-email endpoint yet.
- No admin UI in the app — approving/rejecting a shop creation request means calling
  `AdminController`'s endpoints directly (curl/Postman) with the admin JWT.
- No services/products CRUD or join-request management screens for shop owners yet.

## Testing

- Backend: `./gradlew test`
- Frontend: `cd mobile && npx tsc --noEmit`

See `AGENTS.md` for backend layering conventions, mobile theming conventions, and a
list of non-obvious gotchas (CORS, `expo-secure-store` on web, peer-dependency
overrides) worth reading before making changes.
