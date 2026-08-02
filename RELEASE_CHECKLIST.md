# Prezzence — Production Release Checklist

Everything needed to ship to Google Play production and run the paid subscription flow.

> ⚠️ **Production package is `com.pollecode.prezzence`** (per the Play Console listing).
> The build defaults to `com.pollecode.prezzencekotlin`, so you **must** override the
> application ID at build time (see below) or the AAB will be rejected for package mismatch.
> `google-services.json` already contains both packages.

App package (production): `com.pollecode.prezzence` · Subscription product ID: `prezzence_pro`

---

## 1. Build a signed production AAB

Set a unique, increasing version for every upload (Play rejects duplicate `versionCode`).

```powershell
# Windows / PowerShell
$env:PREZZENCE_PLAY_APPLICATION_ID="com.pollecode.prezzence"  # MUST match the Play listing
$env:PREZZENCE_VERSION_CODE="1"      # bump (+1) on every upload
$env:PREZZENCE_VERSION_NAME="1.0.0"  # user-facing version

.\gradlew.bat verifyPrezzenceReleaseEnv   # confirms signing + Supabase + backend env
.\gradlew.bat :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Release signing env vars (set on the build machine / CI)
| Variable | Purpose |
|---|---|
| `PREZZENCE_PLAY_APPLICATION_ID` | **`com.pollecode.prezzence`** (production listing) |
| `PREZZENCE_UPLOAD_STORE_FILE` | Path to the upload keystore |
| `PREZZENCE_UPLOAD_STORE_PASSWORD` | Keystore password |
| `PREZZENCE_UPLOAD_KEY_ALIAS` | Key alias |
| `PREZZENCE_UPLOAD_KEY_PASSWORD` | Key password |
| `PREZZENCE_API_URL` | HTTPS backend (`https://prezzence-backend.onrender.com`) |
| `PREZZENCE_SUPABASE_URL` / `PREZZENCE_SUPABASE_ANON_KEY` | Supabase auth |
| `PREZZENCE_SUBSCRIPTION_PRODUCT_ID` | `prezzence_pro` |
| `PREZZENCE_VERSION_CODE` / `PREZZENCE_VERSION_NAME` | Version (bump each upload) |

> These can also live in `release/signing.properties` (gitignored) instead of env vars.

---

## 2. Complete Play Console "App content" (required before rollout)

- [ ] **Privacy policy** — hosted URL added in the store listing.
- [ ] **Data safety** — declare: Audio/voice recordings, Account info, transcripts; sent to backend/AI; used for app functionality. Account deletion is available in-app (Settings → Privacy and deletion).
- [ ] **App access** — provide reviewer test credentials (the app is behind login).
- [ ] **Content rating** questionnaire.
- [ ] **Target audience** (adults, not children) and **Ads** declaration (none).

---

## 3. Create the production release

- [ ] Test and release → **Production** → Create new release.
- [ ] Use **Play App Signing** (recommended — Google manages the signing key).
- [ ] Upload `app-release.aab`, add release notes.
- [ ] Review → **Start rollout to Production**.

### ⚠️ App Links / deep-link verification
Google re-signs the app under Play App Signing, so the `assetlinks.json` fingerprint
must be the **Play App Signing certificate** SHA-256 (Play Console → *App integrity*),
**not** the upload key.

- Endpoint: `GET /.well-known/assetlinks.json` (package + fingerprints come from Render env)
- Health check: `GET /api/health/production` → `ready_for_paid_users` should be `true`
- [ ] Set on Render, then **redeploy backend**:
  - `ANDROID_APP_SHA256_FINGERPRINTS=<Play App Signing SHA-256>`
  - `ANDROID_APP_PACKAGE_NAMES=com.pollecode.prezzence`
- Upload key SHA-256 (wrong for Play installs): run `scripts/print-upload-cert-sha256.ps1`

---

## 4. Subscription / pricing setup (paid users on day one)

**Full Play Console + Render steps:** `docs/PLAY_SUBSCRIPTION_AND_DEEPLINKS.md`  
**Render env template:** `release/render-production.env.template`

The app reads price + offer **dynamically from Play** — nothing is hardcoded.

- [ ] Monetize → Products → **Subscriptions** → Create.
- [ ] Product ID: **`prezzence_pro`** (must match exactly).
- [ ] Add a **base plan** (auto-renewing; monthly and/or annual), set price, **Activate**.
- [ ] (Optional) Add an **offer** (free trial / intro price) — app auto-uses the first offer token.

The paywall price label comes from the active base plan's `formattedPrice`.

---

## 5. Backend billing config (server-side verification)

Verifies purchases via the Google Play Developer API so entitlements can't be faked.

- [ ] Create a Google Cloud **service account**; grant it access in Play Console
      (Users & permissions / API access) with *Manage orders & subscriptions*.
- [ ] Set backend (Render) env:

| Variable | Value |
|---|---|
| `GOOGLE_PLAY_PACKAGE_NAME` | **`com.pollecode.prezzence`** (must match the published app) |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Full service-account JSON (or use `GOOGLE_PLAY_SERVICE_ACCOUNT_FILE`) |
| `ANDROID_APP_SHA256_FINGERPRINTS` | Play App Signing SHA-256 (for assetlinks deep-link verification) |

- App calls `POST /api/billing/google/sync` after purchase → verifies + grants premium.

### Real-Time Developer Notifications (renewals / cancels / refunds)
- [ ] Create a Cloud **Pub/Sub topic** with a **push subscription** to:
      `https://prezzence-backend.onrender.com/api/billing/google/rtdn`
- [ ] Play Console → Monetization setup → **Real-time developer notifications** → paste topic name → Send test notification.
- Webhook already handles grants (purchased/renewed/restarted/recovered) and revokes (canceled/on-hold/revoked/expired).

---

## 6. App attestation — Play Integrity (anti-abuse)

The app sends a **Play Integrity** token after sign-in; the backend decodes it via the
Play Integrity API (reusing the billing service account) and records the verdict.
It is **advisory by default** — users are never blocked — so it's safe to ship as-is and
turn on enforcement later once you've watched the verdicts.

- [ ] Play Console → **App integrity → Play Integrity API** → link the same Google Cloud
      project as the service account, and enable the **Play Integrity API** in that GCP project.
- [ ] Grant the service account the **Play Integrity** scope (it uses
      `https://www.googleapis.com/auth/playintegrity`; the existing Play API access covers this).
- [ ] Confirm the client cloud project number is correct. It defaults to the value in
      `google-services.json` (`730307820828`); override at build time only if it differs:
  - `PREZZENCE_PLAY_CLOUD_PROJECT_NUMBER=<GCP project number>`
- Backend env (Render):

| Variable | Value |
|---|---|
| `GOOGLE_PLAY_PACKAGE_NAME` | **`com.pollecode.prezzence`** (already set for billing) |
| `PLAY_INTEGRITY_ENFORCED` | `false` (default; set `true` later to 403 untrusted app/device) |

- Endpoint: `POST /api/integrity/verify` (requires auth). If the service account isn't
  configured, it returns `verified:false` and stays non-blocking.

---

## 7. Test before going live (no real charges)

- [ ] Play Console → Settings → **License testing** → add tester Gmail accounts.
- [ ] Upload AAB to **Internal testing** first (products only return details once published on a track + licensed tester).
- [ ] Install from the track and verify end-to-end:
  - price shows on paywall → purchase → premium unlocks
  - backend `/api/billing/google/sync` grants entitlement
  - RTDN test notification updates entitlement
- [ ] Promote the **same build** to Production and start rollout.

---

## Quick reference — backend endpoints
- `GET  /api/health/production` — billing + assetlinks config status (no secrets)
- `GET  /api/billing/google/status` — subscription verification configured?
- `POST /api/billing/google/sync` — client purchase verification + grant
- `POST /api/billing/google/rtdn` — Pub/Sub push webhook for subscription lifecycle
- `POST /api/integrity/verify` — Play Integrity token verification (advisory by default)
- `GET  /.well-known/assetlinks.json` — Android App Links verification
- `GET  /auth/verified` — email-verification redirect into `prezzence://verify`
