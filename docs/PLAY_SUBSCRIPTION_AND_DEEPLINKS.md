# Play Console: subscriptions + App Links

Use this after uploading the production AAB (`com.pollecode.prezzence`).

---

## A. Create `prezzence_pro` subscription (paid users on day one)

1. Play Console → **Monetize → Products → Subscriptions**
2. **Create subscription**
3. **Product ID:** `prezzence_pro` (must match exactly — the app reads this from `BuildConfig`)
4. **Name:** Prezzence Pro (or similar)
5. **Create base plan** (e.g. monthly auto-renewing)
   - Set your price (e.g. $9.99/month)
   - **Activate** the base plan
6. (Optional) Add a **free trial** or intro offer — the app uses the first offer token automatically
7. **Activate** the subscription product

The paywall shows the price from Play (`formattedPrice`). No app update needed when you change price.

### Verify it works (no real charge)

1. Play Console → **Settings → License testing** → add your Gmail
2. Install the app from Play (closed/internal track is fine)
3. Open paywall → confirm price appears
4. Purchase → license testers are charged **$0**
5. Premium should unlock; backend `POST /api/billing/google/sync` grants entitlement

---

## B. Backend billing (Render)

Set on **Render → prezzence-backend → Environment** (see `release/render-production.env.template`):

| Variable | Value |
|---|---|
| `GOOGLE_PLAY_PACKAGE_NAME` | `com.pollecode.prezzence` |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Service account JSON from GCP |
| `BETA_UNLOCK_ALL_FEATURES` | `false` |

**Service account setup**

1. Google Cloud → create service account → enable **Google Play Android Developer API**
2. Play Console → **Users and permissions → API access** → link project → grant service account **Manage orders and subscriptions**
3. Download JSON → paste into `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` on Render → **Redeploy**

Check: `GET https://prezzence-backend.onrender.com/api/billing/google/status`  
Should return `"configured": true` and `"package_matches_production": true`.

### Renewals / cancellations (RTDN)

1. Google Cloud → **Pub/Sub** → create topic (e.g. `play-rtdn`)
2. Create **push subscription** → endpoint:
   `https://prezzence-backend.onrender.com/api/billing/google/rtdn`
3. Play Console → **Monetize → Monetization setup → Real-time developer notifications** → paste topic name → **Send test notification**

---

## C. Email verification deep links (`prezzence://verify`)

When users tap the link in the verification email, Android opens:

`https://prezzence-backend.onrender.com/auth/verified` → **Open Prezzence** → `prezzence://verify`

For verified App Links (no browser chooser), the backend must serve the **Play App Signing** SHA-256.

### Get the fingerprint

1. Play Console → **Test and release → App integrity → App signing**
2. Under **App signing key certificate**, copy **SHA-256 certificate fingerprint**
3. On Render, set:
   ```
   ANDROID_APP_PACKAGE_NAMES=com.pollecode.prezzence
   ANDROID_APP_SHA256_FINGERPRINTS=<paste SHA-256 here>
   ```
4. **Redeploy** the backend

### Verify

```text
GET https://prezzence-backend.onrender.com/.well-known/assetlinks.json
GET https://prezzence-backend.onrender.com/api/health/production
```

`assetlinks.json` should list `com.pollecode.prezzence` with your Play SHA-256.  
`ready_for_paid_users` should be `true` once billing + fingerprints are set.

**Do not use the upload key SHA-256** — only the Play App Signing key works for users who installed from Play Store.

Your **upload key** SHA-256 (for reference only): run `scripts/print-upload-cert-sha256.ps1`

---

## D. Production checklist (quick)

- [ ] `prezzence_pro` subscription + base plan **Activated**
- [ ] Render: `GOOGLE_PLAY_PACKAGE_NAME=com.pollecode.prezzence`
- [ ] Render: `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` set, backend redeployed
- [ ] Render: `ANDROID_APP_SHA256_FINGERPRINTS` = Play App Signing SHA-256
- [ ] Render: `BETA_UNLOCK_ALL_FEATURES=false`
- [ ] RTDN webhook connected
- [ ] License-tester purchase test passed
