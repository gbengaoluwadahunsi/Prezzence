# Visual Parity Audit

Date: 2026-06-09

## Status

The Kotlin rewrite now includes a `Settings > Visual parity` screen that lists the React Native route surface and the matching Kotlin surface for screenshot review. This does not fake screenshot proof; it gives testers an in-app parity map to capture and compare the same flows on Android devices.

## Route Coverage

| React Native route | Kotlin surface | Status |
| --- | --- | --- |
| `/` | Splash and landing | Covered |
| `/auth/language` | Language | Covered |
| `/auth/sign-in` | Sign in | Covered |
| `/auth/sign-up` | Create account | Covered |
| `/auth/forgot-password` | Reset password | Covered |
| `/auth/reset-password` | Reset password email flow | Functional equivalent |
| `/verify` | Auth confirmation path | Functional equivalent through sign-in/onboarding |
| `/(tabs)/home` | Home | Covered |
| `/(tabs)/questions` | Questions | Covered |
| `/onboarding/type` | Role/interview setup selection | Functional equivalent |
| `/onboarding/role` | Questions and role selection | Covered |
| `/onboarding/mic-permission` | Runtime mic/camera permission flow and Device QA | Covered |
| `/interview/entering` | Room setup | Covered |
| `/interview/speaking` | Interview and answer result states | Covered |
| `/(tabs)/progress` | Progress | Covered |
| `/(tabs)/practice` | Progress/history | Functional equivalent |
| `/sessions` | Session history | Covered through Progress/history |
| `/sessions/[sessionId]` | Session summary/result details | Functional equivalent |
| `/(tabs)/profile` | Settings/account/profile surfaces | Functional equivalent |
| `/settings` | Settings | Covered |
| `/settings/account` | Account | Covered |
| `/settings/notifications` | Notifications settings/inbox | Functional equivalent |
| `/profile/notifications` | Notifications inbox | Covered |
| `/profile/subscription` | Google Play subscription management | Covered |
| `/(modals)/paywall` | Subscription screen | Functional equivalent |
| `/profile/privacy` | Privacy & Terms | Covered |
| `/legal/privacy` | Privacy copy | Covered |
| `/legal/terms` | Terms copy | Covered |
| `/feedback` | Feedback | Covered |
| `/profile/delete-account` | Account delete | Covered |
| `/profile/help` | Settings support/legal surfaces | Functional equivalent |
| `/errors/*` | Permission toasts, fallback states, Device QA | Functional equivalent |

## Screenshot Proof Procedure

1. Build/install the Kotlin APK on the target Android device.
2. Run `Settings > Device QA` and save screenshots of the pass/fail results.
3. Open `Settings > Visual parity` and capture the route map.
4. Capture the corresponding React Native route screenshots from the production app or Expo build.
5. Compare each pair for hierarchy, visible controls, copy, spacing, safe-area behavior, and failure states.
6. Record any differences in this file before Play Store replacement.

## Remaining Visual Risk

- Pixel-level equivalence still requires actual screenshots from both apps on the same device sizes.
- Some React Native routes are combined into functional Kotlin equivalents rather than one-to-one native screens.
- Error routes in React Native are represented in Kotlin by permission handling, QA checks, toasts, and fallback UI rather than separate decorative error pages.
