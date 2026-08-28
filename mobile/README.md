# Seu Barbeiro — Mobile

React Native app (Expo + Expo Router) for the Seu Barbeiro barbershop booking API in `../src`.

## Getting started

```sh
npm install
npm start
```

Then press `i` (iOS simulator), `a` (Android emulator), or scan the QR code with Expo Go.

## Pointing at the backend

The API base URL comes from the `EXPO_PUBLIC_API_URL` env var (see `.env`). It defaults to
`http://localhost:8080`, which only works when running the **web** target or an emulator that
proxies `localhost` to your machine (Android Studio emulator does this automatically for
`10.0.2.2`, but plain `localhost` inside Expo Go on a **physical device** will not reach your
computer).

To test on a physical device with Expo Go, find your computer's LAN IP (e.g. `192.168.1.42`) and
set:

```sh
EXPO_PUBLIC_API_URL=http://192.168.1.42:8080
```

in `.env`, then restart `npm start`. Make sure the Spring Boot app (`../gradlew bootRun`) and your
phone are on the same network, and that Postgres is running (`docker compose up` from the repo
root, or your local setup).

## Structure

- `app/` — Expo Router routes. `(auth)/` holds the login/register flow, `(app)/` holds the
  authenticated tab flow (Home, Agendamentos, Perfil) plus modal-style detail/booking screens.
- `lib/api/` — one file per backend controller, thin typed wrappers around `lib/api.ts`'s fetch
  helper.
- `context/AuthContext.tsx` — session state (JWT + user info), persisted via `expo-secure-store`.
- `theme/` — colors, typography (Oswald via `@expo-google-fonts/oswald`), spacing tokens matching
  the Figma print.
- `components/Logo.tsx` — vector recreation of the "Seu Barbeiro" barber-pole crest. If you export
  the real logo asset from Figma, drop it at `assets/images/logo.png` and swap this component's
  implementation for an `<Image>`.

## Known gaps (by design, see repo root plan)

- The registration "confirmation code" screen is a UI-only step — there's no verify-email endpoint
  on the backend yet, so it doesn't call the API.
- "Dono Barbearia" registration submits a barbershop **request**, which needs admin approval
  (`AdminController`) before the shop actually exists. There's no admin UI in this app yet.
- Services/products CRUD and join-request management for shop owners aren't built yet — owners can
  view their shop and toggle "accepting barbers" from the Home tab, nothing deeper.
