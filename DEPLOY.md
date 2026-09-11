# DEPLOY.md — Seu Barbeiro (deploy gratuito, escalável depois)

Passo a passo para publicar a aplicação de graça para os primeiros clientes e
escalar depois sem retrabalho. Feito para ser retomado a frio, em outra máquina
e em outra conversa.

> Leia junto com `AGENTS.md` (convenções e gotchas do código).

---

## 1. Arquitetura do deploy

```
┌──────────────────────┐        HTTPS + JWT        ┌───────────────────────────┐
│  App web (Expo web)  │ ───────────────────────►  │  Spring Boot API (Docker) │
│  Cloudflare Pages    │ ◄───────────────────────  │  Render  (free web svc)    │
│  *.pages.dev         │                            └─────────────┬─────────────┘
└──────────────────────┘                                          │ JDBC + SSL
                                                                  ▼
                                                         PostgreSQL — Neon
                                                         (free, persistente)
```

| Camada        | Serviço              | Plano  | Observação                                             |
|---------------|----------------------|--------|-------------------------------------------------------|
| Banco         | **Neon** (neon.tech) | Free   | Persistente. Dorme após ~5 min, acorda em ~1s.        |
| Backend       | **Render** (Docker)  | Free   | 512 MB. Dorme após 15 min → cold start ~40–60s.       |
| App clientes  | **Cloudflare Pages** | Free   | Upload da pasta `mobile/dist/`. Link `*.pages.dev`.   |
| App nativo    | **EAS Build**        | Free\* | Android APK grátis. iOS exige Apple Developer ($99/a).|

Escala depois: Render Starter ($7/mês, sem cold start) ou mesma imagem Docker num
VPS; Neon Launch ($19/mês); EAS Submit para as lojas.

---

## 2. Contas necessárias

- GitHub (repo já está lá: `back-end-seu-barbeiro`)
- Neon — https://neon.tech
- Render — https://render.com
- Cloudflare — https://dash.cloudflare.com
- Gmail dedicado para envio dos códigos de verificação:
  `donotreplyseubarbeiro@gmail.com` (com **App Password** de 16 caracteres —
  https://myaccount.google.com/apppasswords, precisa de verificação em 2 etapas)
- (Depois) Expo — https://expo.dev, para `eas build`

---

## 3. Variáveis de ambiente

O backend lê tudo de env vars (com fallback local em `application.properties`).
Localmente elas vêm do arquivo **`.env` na raiz** (gitignored — nunca commitar).
Em produção, são configuradas no **painel do Render** (aba *Environment*).

**Fonte da verdade dos valores reais: o painel do Render.** Se `.env` for perdido,
recrie a partir de `.env.example` copiando os valores da aba Environment do Render.

| Variável                     | Secret? | Valor / origem                                                              |
|------------------------------|---------|----------------------------------------------------------------------------|
| `DATABASE_URL`               | não     | `jdbc:postgresql://<host-direto-neon>/neondb?sslmode=require` (ver §4)     |
| `DB_USER`                    | sim     | usuário do Neon (ex. `neondb_owner`)                                       |
| `DB_PASSWORD`                | sim     | senha do Neon                                                             |
| `JWT_SECRET`                 | sim     | `openssl rand -hex 32`                                                    |
| `ADMIN_EMAIL`                | não     | e-mail da conta admin (auto-criada no boot)                               |
| `ADMIN_PASSWORD`             | sim     | senha forte da conta admin                                                |
| `MAIL_HOST`                  | não     | `smtp.gmail.com`                                                          |
| `MAIL_PORT`                  | não     | `587`                                                                     |
| `MAIL_USERNAME`              | não     | `donotreplyseubarbeiro@gmail.com`                                         |
| `MAIL_PASSWORD`              | sim     | App Password do Gmail (16 chars, sem espaços)                             |
| `MAIL_FROM`                  | não     | `donotreplyseubarbeiro@gmail.com`                                         |
| `SUBSCRIPTION_PIX_KEY`       | não     | sua chave Pix (recebe as assinaturas de R$30 dos barbeiros)               |
| `SUBSCRIPTION_MERCHANT_NAME` | não     | `Seu Barbeiro`                                                            |
| `WEBPUSH_VAPID_PUBLIC_KEY`   | não     | `npx web-push generate-vapid-keys`                                        |
| `WEBPUSH_VAPID_PRIVATE_KEY`  | sim     | idem (par gerado junto)                                                   |
| `WEBPUSH_VAPID_SUBJECT`      | não     | `mailto:donotreplyseubarbeiro@gmail.com`                                  |
| `CORS_ALLOWED_ORIGINS`       | não     | **URL do app web** (Cloudflare Pages), ex. `https://seu-barbeiro.pages.dev` |
| `PORT`                       | não     | injetado pelo Render automaticamente — **não** definir manualmente        |

> ⚠️ `CORS_ALLOWED_ORIGINS` é a URL do **frontend** (Pages), não a do backend.
> Origens de dev local (`localhost:*`, `192.168.*`) já são liberadas pelo
> `SecurityConfig` — não precisa listar.

---

## 4. Passo a passo

### Passo 0 — Ajustes no repo (JÁ FEITOS, aqui só para referência)

- `application.properties`: `server.port=${PORT:8080}` (Render injeta `$PORT`).
- `mobile/public/_redirects`: SPA fallback do export web.
- `mobile/eas.json`: perfis `development` / `preview` (APK) / `production`.
- `.env` na raiz: criado (gitignored) com secrets e valores de referência.

Verificar: `./gradlew compileJava -q`

### Passo 1 — Banco no Neon

1. neon.tech → **New Project** → região **AWS `sa-east-1` (São Paulo)**.
2. Em **Connection Details**, **desligue** "Connection pooling" e copie os dados.
   Precisamos do **endpoint direto** (host **sem** o sufixo `-pooler`), porque o
   Hikari mantém conexões longas — o pooler (PgBouncer) do Neon é para serverless.
3. Monte a `DATABASE_URL` no formato JDBC, sem credenciais na URL, sem
   `channel_binding`:
   ```
   DATABASE_URL=jdbc:postgresql://ep-xxxx-xxxx.sa-east-1.aws.neon.tech/neondb?sslmode=require
   DB_USER=<usuario>
   DB_PASSWORD=<senha>
   ```
4. Teste local: preencha o `.env`, rode `./gradlew bootRun`. Nos logs deve
   aparecer o Flyway aplicando `V1..V7` e `Started ... on port 8080`.

### Passo 2 — Backend no Render

1. render.com → **New → Web Service** → conecta o GitHub → repo
   `back-end-seu-barbeiro`, branch `main`.
2. Render detecta o `Dockerfile` → **Runtime: Docker**, Root Directory vazio.
3. **Instance Type: Free**. Health Check Path: deixe **vazio** (não há Actuator;
   o Render checa a porta TCP).
4. **Environment Variables**: adicione todas as da tabela §3 (menos `PORT`).
5. **Create Web Service**. Primeiro build ~5–8 min (Gradle dentro do Docker).
6. Anote a URL do serviço, ex.: `https://back-end-seu-barbeiro.onrender.com`.

**Manter quente (opcional, contorna o cold start):** cron-job.org (free) fazendo
`GET https://<backend>/` a cada 10 min. O free do Render dá 750 h/mês ≈ 1 serviço
sempre ligado.

**Se der OutOfMemory no boot:** no `Dockerfile`, troque `-Xmx256m` por `-Xmx200m`.

### Passo 3 — App web para os clientes

> Faça **depois** do Passo 2 — a URL da API fica embutida no build.

1. `mobile/.env.production` (crie; é só uma URL pública, pode commitar):
   ```
   EXPO_PUBLIC_API_URL=https://back-end-seu-barbeiro.onrender.com
   ```
2. Compilar:
   ```sh
   cd mobile
   npm install
   npx expo export --platform web --output-dir dist
   ```
   Confira `mobile/dist/` (tem `index.html`, `_expo/`, `_redirects`).
3. Publicar (Cloudflare Pages, sem CLI):
   - dash.cloudflare.com → **Workers & Pages** → **Create** → aba **Pages** →
     **Upload assets**
   - Nome do projeto: `seu-barbeiro` → **Create project**
   - Arraste a pasta `mobile/dist` inteira → **Deploy site**
   - Anote a URL: `https://seu-barbeiro.pages.dev`
   - *(Alternativa 2 min: https://app.netlify.com/drop — arrasta o `dist`.)*
4. **CORS**: Render → Environment → `CORS_ALLOWED_ORIGINS=https://seu-barbeiro.pages.dev`
   → salvar (redeploy automático).
5. **Atualizar o app depois**: repita 3.2 e, no projeto do Cloudflare Pages,
   **Create new deployment** → arraste o novo `dist`.

### Passo 4 — Configuração pós-deploy (admin via curl)

Não há tela de admin no app. Com o backend no ar:

```sh
API=https://back-end-seu-barbeiro.onrender.com

# 1. login como admin (ADMIN_EMAIL / ADMIN_PASSWORD do Render)
curl -s -X POST $API/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"<ADMIN_EMAIL>","password":"<ADMIN_PASSWORD>"}'
# copie o token do JSON de resposta
TOKEN=<token>

# 2. aprovar pedidos de barbearia
curl -s $API/api/admin/barbershop-requests -H "Authorization: Bearer $TOKEN"
curl -s -X PATCH $API/api/admin/barbershop-requests/<id> \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"approved": true}'

# 3. confirmar pagamentos de assinatura (quando o Pix cair na sua conta)
curl -s $API/api/admin/subscription-payments -H "Authorization: Bearer $TOKEN"
curl -s -X PATCH $API/api/admin/subscription-payments/<id> \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"approved": true}'
```

### Passo 5 — Teste ponta a ponta

1. Abra `https://seu-barbeiro.pages.dev` no PC (F12 → Network para ver erros).
2. Registre um cliente com um e-mail **real seu**.
3. O código de 6 dígitos deve chegar (remetente `donotreplyseubarbeiro@gmail.com`).
   Login só funciona **depois** de verificar o código (`User.isEnabled()` ==
   `emailVerified`).
4. Verifique → deve logar e cair na Home.
5. Repita no celular; no iPhone use "Adicionar à Tela de Início" (vira PWA).

### Passo 6 (depois) — App nativo Android

`mobile/eas.json` já está pronto.

```sh
npm i -g eas-cli
cd mobile
eas login
eas init                 # cria o projectId em app.json
eas build --platform android --profile preview   # gera APK com link de download
```

Ajuste o `EXPO_PUBLIC_API_URL` dentro de `eas.json` se a URL do backend mudar.
iOS: precisa de conta Apple paga — até lá, PWA no Safari.

---

## 5. Troubleshooting

| Sintoma | Causa / correção |
|---|---|
| `URL must start with 'jdbc'` no boot do Render | `DATABASE_URL` está no formato cru do Neon. Use `jdbc:postgresql://host/neondb?sslmode=require` e ponha usuário/senha em `DB_USER`/`DB_PASSWORD`. |
| `FATAL: password authentication failed` | `DB_USER`/`DB_PASSWORD` errados, ou credenciais deixadas dentro da URL brigando com as separadas. |
| Flyway trava / erros estranhos de prepared statement | Você está usando o endpoint `-pooler` (PgBouncer). Use o endpoint **direto** do Neon. |
| App web carrega mas toda chamada dá erro de CORS | `CORS_ALLOWED_ORIGINS` no Render não bate **exatamente** com a URL do Pages (sem barra no fim, com `https://`). |
| Primeira requisição do dia demora ~1 min | Cold start do free tier do Render. Normal. Use o ping do cron-job.org. |
| Cliente registra mas não consegue logar | E-mail não verificado. Confira se o SMTP está mandando (logs do Render) e se o código chegou. `POST /api/auth/resend-code` reenvia. |
| `OutOfMemoryError` no boot | Baixe `-Xmx` no `Dockerfile` (256m → 200m). |
| Rota web tipo `/shop/123` dá 404 ao dar F5 | `_redirects` não foi para o `dist/`. Confirme que `mobile/public/_redirects` existe antes do `expo export`. |

---

## 6. Estado atual (atualize ao avançar)

- [x] Passo 0 — ajustes no repo
- [x] Passo 1 — projeto Neon criado (`sa-east-1`), `DATABASE_URL` no formato JDBC direto
- [~] Passo 2 — serviço Render `back-end-seu-barbeiro` criado; corrigindo `DATABASE_URL`
- [ ] Passo 3 — build web + Cloudflare Pages + CORS
- [ ] Passo 4 — aprovar barbearias / assinaturas via curl
- [ ] Passo 5 — teste ponta a ponta
- [ ] Passo 6 — APK Android via EAS
