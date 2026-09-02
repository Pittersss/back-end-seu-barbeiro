# AGENTS.md — Seu Barbeiro

Working notes for whoever (human or agent) touches this repo next. This is a monorepo:
Spring Boot API at the root, Expo/React Native app in `mobile/`. If you only read one
section before making changes, read "Gotchas learned the hard way" below — every item
there cost real debugging time once already.

## Repo layout

```
.                       Spring Boot backend (Java 21, Gradle)
  src/main/java/com/two_m/yourbarber/
    controller/         REST controllers — thin, delegate to services
    service/             interface + *Impl per resource
    repository/          Spring Data JPA
    model/                entities; model/enums/ for the enums
    dto/                  request/response DTOs, one subpackage per resource
    mapper/               static entity <-> DTO mappers (referenced from services)
    security/             JwtAuthenticationFilter, JwtTokenProvider
    config/                SecurityConfig
  src/main/resources/db/migration/   Flyway migrations (V1..V7 so far)
  src/test/java/...       mirrors main/java, Mockito-based service tests
mobile/                 Expo Router app (TypeScript), see mobile/README.md
  app/(auth)/            login, register-role, register, confirm-code, register-shop
  app/(app)/             tab flow: home (branches by role), appointments, profile
  app/(app)/shop/[id].tsx, book/[shopId].tsx, appointment/[id]/pix.tsx
  lib/api/               one file per backend controller, typed fetch wrappers
  lib/storage.ts          cross-platform session storage (see gotchas)
  theme/                  colors.ts, typography.ts, spacing.ts, layout.ts
  components/             Logo, Button, Input, Screen, Badge — shared primitives
```

## Backend conventions

- Layering is strict: Controller → Service (interface + Impl) → Repository → Mapper →
  DTO. Controllers never touch entities directly; mappers are static utility classes.
- `User` is the JPA base (`@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`);
  `Barber` and `Client` extend it. A barber's `id` **is** the same row as their `User`
  row — `AuthResponseDTO.userId` from registering as a barber is the same id used by
  `GET/PUT /api/barbers/{id}`. No separate lookup needed, ever.
- `UserRole` enum is only `CLIENT`, `BARBER`, `ADMIN` — **there is no `OWNER` role.**
  "Dono Barbearia" in the UI registers through `/api/auth/register/barber` like any
  barber, then calls `POST /api/barbershops` to submit a `BarberShopRequest`
  (`PENDING`/`APPROVED`/`REJECTED`), approved only via `AdminController`
  (`GET /api/admin/barbershop-requests`, `PATCH /api/admin/barbershop-requests/{id}`).
  Right after registering as an owner, the barber has `barberShopId == null` until an
  admin approves the request — the mobile app's `_barber-home.tsx` renders a distinct
  "pending approval" state for this.
- `SecurityConfig` permits `/api/auth/**` and gates `/api/admin/**` behind `ROLE_ADMIN`;
  everything else requires a valid JWT (`Authorization: Bearer <token>`).
- An admin account is auto-provisioned from `admin.default-email` /
  `admin.default-password` (`application.properties`, env-overridable). No admin UI
  exists in the mobile app — approve/reject shop requests via curl/Postman for now.
- `dto/pix/` and `service/pix/` are Pix QR-code generation for appointment payments —
  present in the working tree as uncommitted changes I didn't author; treat as existing
  code, not something to redo.

## Mobile conventions

- Expo SDK 57 / React 19.2.3 / RN 0.86.3 — newer than most training data. Before
  writing Expo-specific code, check `mobile/AGENTS.md` (Expo-version-specific note) and
  consider fetching current docs at `https://docs.expo.dev/versions/v57.0.0/` rather
  than trusting memory on router/config APIs.
- Design tokens live in `theme/`; **don't hardcode colors or spacing in screens** —
  every screen already pulls from `colors`, `spacing`, `radius`, `typography`,
  `centeredPage` (from `theme/layout.ts`). The action-button blue is `#2F37C9`
  (`colors.blue`) — same token drives Input's accent chip, links, and the CONFIRMED
  status badge, so changing it changes all of them at once.
- The real exported crest is `mobile/assets/your_barber_logo.png`, rendered via
  `components/Logo.tsx` (an `Image`, sized by its real 1373×1146 aspect ratio). Do not
  reintroduce the old hand-built `react-native-svg` recreation — it was replaced.
- The confirm-code screen (`app/(auth)/confirm-code.tsx`) is intentionally a UI-only
  mock — there is no verify-email endpoint on the backend. Don't wire it to an API
  without adding that endpoint first.
- Profile identity (name / phone / avatar) for **all** roles goes through
  `PUT /api/users/me` (`lib/api/users.ts`); `avatarBase64` is a downscaled 256px
  JPEG string (`lib/avatar.ts`). Barber-only fields (pixKey, delayTolerance,
  workStartHour/workEndHour/breakStartHour/breakEndHour) still use
  `PUT /api/barbers/{id}` — that endpoint is a **full replace**, so any screen that
  saves one barber field must send them all (see `profile.tsx` / `availability.tsx`).
  After a profile save call `useAuth().refreshProfile()` so the greeting/`Avatar`
  update app-wide.
- Barber availability = a daily working window + optional daily break on `Barber`,
  plus one-off `TimeBlock`s (`/api/barbers/{id}/time-blocks`) and a client blocklist
  (`/api/barbers/{id}/blocked-clients`). The client booking screen never computes
  free time itself — it calls `GET /api/barbers/{id}/open-slots?serviceId=&from=&to=`
  and renders exactly what comes back (`AvailabilityServiceImpl` subtracts window,
  break, blocks, booked appointments and the past, sized to the service duration).
  Managed on `app/(app)/availability.tsx`; toggle `available` via
  `PATCH /api/barbers/{id}/availability`.
- `POST /api/pix/preview` (backend, uncommitted third-party code) still exists but
  has no mobile UI — the QR sandbox screen was removed after validation.

## Gotchas learned the hard way

1. **The backend does not hot-reload.** If it's running via an IDE run button or a
   plain `java -cp build/classes/...` process, editing a `.java` file and recompiling
   does nothing to that running JVM — it must be killed and restarted. `./gradlew
   bootRun` is the reliable way to (re)start it from a terminal.
2. **CORS is real on web, invisible on native.** `SecurityConfig` has a
   `CorsConfigurationSource` allowing `localhost:*` / `127.0.0.1:*` / `10.*:*` /
   `192.168.*:*` origins — needed because the Expo *web* target is a browser making
   cross-origin requests to `:8080`, unlike native iOS/Android which never enforces
   CORS at all. If you add a new origin pattern (e.g. a deployed web URL), extend that
   bean, not a per-request workaround.
3. **`mobile/.npmrc` sets `legacy-peer-deps=true`.** Expo Router 57 bundles its own web
   tooling (`@radix-ui`, `vaul`) whose peer deps conflict with the pinned `react`
   version. Without that file, plain `npm install` fails with ERESOLVE. Don't remove it
   without re-verifying `npm install` still works clean.
4. **`expo-secure-store` has no web backing** — its web module is a stub (`export
   default {}`), so calling it directly throws on web. Session persistence goes through
   `lib/storage.ts` (SecureStore on native, `localStorage` on web), not
   `expo-secure-store` directly. If you add new persisted state, use that wrapper.
5. **RN Web draws a default browser focus ring on `TextInput`.** Suppressed in
   `components/Input.tsx` via a web-only `outlineStyle: 'none'` style (cast through
   `unknown` since RN's own types don't know the key — react-native-web adds it).
6. **Web content is capped to 480px and centered** (`theme/layout.ts`'s
   `centeredPage`), wired into `Screen.tsx` and every `(app)` screen's outer
   header/content/list/footer style. Without it, RN's flexible-width layouts stretch
   edge-to-edge on a real browser window and look broken. New screens should spread
   `...centeredPage` into their outer container style(s) too.
7. **Local dev Postgres**: no dev `docker-compose.yml` exists, only
   `docker-compose.prod.yaml` (builds the app image, not meant for the edit-reload
   loop). Local Postgres is just a bare `docker run` matching
   `application.properties`' defaults (`myuser`/`secret`/db `yourbarber`); see the root
   README for the exact command. (`compose.yaml` at the repo root is what
   `spring-boot-docker-compose` auto-starts — container `back-end-seu-barbeiro-postgres-1`.)
8. **`@react-native-community/datetimepicker` has no web build** — on the Expo web
   target its pickers render nothing. `components/Calendar.tsx` (month grid) and
   `components/DateTimeSelect.tsx` (calendar + slot grid) are built from RN
   primitives; use them, not the native picker, for scheduling UI.
9. **`open-in-view=false` + lazy associations.** A `@Service` that reads a lazy
   `@ManyToOne` (e.g. `appointment.getBarber().getPixKey()`) **outside** a
   transaction throws `LazyInitializationException` → an unhandled 500.
   `AppointmentServiceImpl` / `BarberShopServiceImpl` / `PixServiceImpl` are all
   class-level `@Transactional`; new services that touch entity graphs must be too.
   `GlobalExceptionHandler` now has an `Exception.class` catch-all that logs the
   stack trace, so genuine 500s show up in the backend console.

## Verifying changes

- Backend: `./gradlew test` (or a single class with `--tests
  "com.two_m.yourbarber...ClassName"`); `./gradlew compileJava -q` for a fast
  compile-only check.
- Mobile: `npx tsc --noEmit` inside `mobile/`; `npx expo export --platform web
  --output-dir <tmp-dir>` (or `--platform ios`) to confirm Metro bundles clean without
  actually running a device — delete the output dir after, it's just a smoke test.
