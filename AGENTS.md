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
  src/main/resources/db/migration/   Flyway migrations (V1..V20 so far)
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
  admin approves the request — `components/BarberHome.tsx` renders a distinct
  "pending approval" state for this, with a second button to browse existing shops
  instead (see the join-request flow below).
- A barber who wants to join an **existing** shop (not create/own one) uses a separate
  flow: `POST/GET/PATCH /api/barbershops/{shopId}/join-requests`
  (`JoinRequestController`/`JoinRequestServiceImpl`), gated on the shop having
  `acceptingBarbers=true` and the barber not already belonging to one. Wired end-to-end
  in mobile: `app/(app)/join-shop.tsx` (browse shops accepting barbers) →
  `app/(app)/shop/[id].tsx`'s "Solicitar entrada" button (barber-only footer, replaces
  "Agendar Horário") → owner reviews/accepts at `app/(app)/join-requests.tsx`, linked
  from `profile.tsx`'s "Minha barbearia" section. Both sides get notified (see the
  notification section below). This used to be backend-only/dead from the mobile
  app's perspective — don't assume a backend endpoint without a mobile screen is
  unused; check for exactly this pattern before adding a duplicate.
- Admin can delete a client (`DELETE /api/admin/clients/{id}`) or a barbershop
  (`DELETE /api/admin/barbershops/{id}`) from `components/AdminHome.tsx`. Deleting a
  barbershop detaches every barber in it (`barberShop = null`) and sets
  `Barber.blockedFromOwning = true` on the owner, so `BarberShopServiceImpl
  .requestCreation()` now rejects (403) any further `POST /api/barbershops` from that
  same barber — they can still join another shop as staff, just never own one again.
  There is no "unblock" endpoint; that field would need a direct DB edit today.
- `SecurityConfig` permits `/api/auth/**` and gates `/api/admin/**` behind `ROLE_ADMIN`;
  everything else requires a valid JWT (`Authorization: Bearer <token>`).
- An admin account is auto-provisioned from `admin.default-email` /
  `admin.default-password` (`application.properties`, env-overridable). It has a real
  mobile UI now: logging in as `ADMIN` routes `app/(app)/home.tsx` to
  `components/AdminHome.tsx` (not `BarberHome` — don't let that fallback regress),
  which approves/rejects barbershop requests and subscription payments, lists/deletes
  clients and barbershops, and lets an admin browse **all** appointments
  (paginated — see the subscription/appointments bullets below). curl/Postman is no
  longer required for any of this.
- `dto/pix/` and `service/pix/` are Pix QR-code generation for appointment payments —
  present in the working tree as uncommitted changes I didn't author; treat as existing
  code, not something to redo.
- Every barber (owner or team member) needs an **active subscription** (R$30/month,
  paid via Pix to the developer's own personal key — `subscription.pix-key`) to use any
  write endpoint on services/products/availability/blocked-clients/shop settings, to
  view or act on their own appointments, and clients can't book a barber whose
  subscription lapsed. `SubscriptionService.assertActive(barberId)` is the one-line
  guard called at the top of each gated method (same pattern as `assertOwner`); it
  throws `SubscriptionRequiredException` → HTTP 402. This now also covers
  `AppointmentServiceImpl.getAppointmentsForUser()` (BARBER role only — CLIENT/ADMIN
  are never gated) and `cancelAppointment()` when the **barber** is the one cancelling
  (a client cancelling their own booking is never blocked by the barber's subscription
  state) — an overdue barber can't see or act on their schedule at all, not just the
  management screens. Status is *computed live* from `SubscriptionPayment` history
  (`ACTIVE` / `PENDING_CONFIRMATION` / `INACTIVE`) — there's no scheduled expiry job,
  and paying again does not stack, it restarts a fresh 30-day window from confirmation.
  `GET/POST /api/subscriptions/me[/pix]` are barber-facing; confirming a payment is an
  admin action (`GET/PATCH /api/admin/subscription-payments[/{id}]`, same as
  barbershop-request approval) exposed in `components/AdminHome.tsx` — no longer
  curl/Postman-only.
- **Notification system**: `NotificationType` (`model/enums/NotificationType.java`)
  covers `APPOINTMENT_REQUESTED/CONFIRMED/CANCELLED`,
  `BARBERSHOP_REQUEST[_DECIDED]`, `SUBSCRIPTION_PAYMENT_PENDING`/`_DECIDED`, and
  `JOIN_REQUEST[_DECIDED]`. Any service that creates one of these situations must
  call `NotificationService.notify(recipientId, type, message, appointmentOrNull)`
  itself — there's no event bus, it's an explicit call at the call site (see
  `BarberShopServiceImpl.requestCreation()`, `AdminServiceImpl.decideRequest()`,
  `SubscriptionServiceImpl.requestPixCharge()`/`decidePayment()`,
  `JoinRequestServiceImpl.requestToJoin()`/`decideRequest()` for the pattern). Adding
  a new "X happened, Y needs to know" flow means adding a `NotificationType` value
  *and* remembering the `notify()` call — it's easy to build the feature and forget
  the notification (this happened at least twice already: barbershop requests and
  join requests both shipped without it originally). On mobile, tapping a
  notification in `components/NotificationPanel.tsx` marks it read and navigates via
  the static `NOTIFICATION_ROUTES` map in that file — a new `NotificationType` needs
  an entry there too, or it's a dead-end tap.

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
- The confirm-code screen (`app/(auth)/confirm-code.tsx`) is **real**, not a mock —
  `POST /api/auth/verify-email` / `/resend-code` exist and it calls them
  (`AuthServiceImpl`). That said, nothing server-side actually *requires*
  verification: `login()` and `SecurityConfig` never check `emailVerified`, so an
  unverified account can already use every authenticated endpoint — the screen is
  just a client-side gate in the registration navigation stack, not a real security
  boundary. For local testing with a fake address (`dev+x@example.com` etc.), set
  `VERIFICATION_LOG_CODE_ON_FAILURE=true` in `.env`
  (`MailServiceImpl.sendVerificationCode`) and the 6-digit code prints to the
  `./gradlew bootRun` console — real SMTP creds won't throw for an undeliverable
  address (it just bounces later), so this logs the code unconditionally rather than
  only on a caught send failure. Never enable it where logs are shared (prod/staging).
- Profile identity (name / phone / avatar) for **all** roles goes through
  `PUT /api/users/me` (`lib/api/users.ts`); `avatarBase64` is a downscaled 192px
  JPEG string (`lib/avatar.ts`) — kept small because it's stored as base64 TEXT
  directly in Postgres, not a separate object store. Barber-only fields (pixKey, delayTolerance,
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
- `app/(app)/appointments.tsx` is shared by all three roles but branches hard on
  `session.role === 'ADMIN'`: admin gets `GET /api/admin/appointments?page=&size=`
  (`lib/api/admin.ts#listAdminAppointments`, 20/page, sorted `scheduledAt desc`) with
  a "Carregar mais" footer + `onEndReached`, no upcoming/history segment (doesn't
  compose with pages), and no cancel/block actions (those never worked for admin
  anyway — `cancelAppointment`'s `isParticipant` check always rejected admin
  requesters, the button just used to render regardless). CLIENT/BARBER still use the
  original unpaginated `listAppointments()` — don't paginate that path too, it's not
  needed and would change the segment-filter UX for no reason.

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
10. **No migration anywhere declares `ON DELETE CASCADE`.** Every FK in
    `db/migration/` is default `NO ACTION`. Deleting a row that anything still points
    to throws a raw `DataIntegrityViolationException` (surfaces as a 500). Hibernate's
    `cascade = CascadeType.ALL, orphanRemoval = true` on `BarberShop.services`/
    `.products` handles those two, but everything else — `Barber.barberShop` (users
    table), `JoinRequest.barberShop`, `Appointment.client`/`.barber`,
    `Notification.recipient`/`.appointment`, `PushSubscription.user`,
    `ClientBlock.client` — has to be nulled/deleted manually, in dependency order,
    before the parent goes. `AdminServiceImpl.deleteClient()`/`deleteBarberShop()` are
    the reference implementations; copy their ordering (detach/delete children first,
    including notifications that reference a doomed row's `appointment_id`) for any
    new delete endpoint rather than rediscovering the FK graph from a 500 stack trace.

## Verifying changes

- Backend: `./gradlew test` (or a single class with `--tests
  "com.two_m.yourbarber...ClassName"`); `./gradlew compileJava -q` for a fast
  compile-only check.
- Mobile: `npx tsc --noEmit` inside `mobile/`; `npx expo export --platform web
  --output-dir <tmp-dir>` (or `--platform ios`) to confirm Metro bundles clean without
  actually running a device — delete the output dir after, it's just a smoke test.
