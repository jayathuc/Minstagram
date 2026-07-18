# Play Store listing material

## Name

Working name: Minstagram. Recommendation: rename before submission. The name
contains "stagram" and Meta enforces its trademarks on store listings.
Neutral candidates: Doorway, Threshold, Intently, Mindgate.

## Short description (max 80 chars)

A pause between you and Instagram. Set an intention, keep your time.

## Full description

Instagram is built to keep you scrolling. Minstagram puts one small question
in front of it: why are you here?

Before Instagram opens, you pick a reason and a time budget. A quiet timer
runs while you browse. When time is up, the app steps in. If you open
Instagram on autopilot, Minstagram catches it and asks first.

- Intention gate with a short unlock pause that grows with every session
- Session timer that only counts while Instagram is actually on screen
- Catches direct opens, with an honest snooze if you really want in
- Reel counter and a small question between Reels, so watching stays a choice
- Real screen time on the home screen, today and the last 7 days
- Session history: what you planned, what actually happened
- Loosening a protection takes a five second hold. Tightening is instant.

Everything runs on your phone. No account, no analytics, no data leaves
your device. Uninstalling deletes everything.

## Category

Lifestyle (alternative: Tools). Content rating: Everyone.

## Data safety form answers

- Does your app collect or share any user data? No.
- Data is processed locally only. Nothing is transmitted.
- Data deletion: uninstall removes all data.

## Sensitive permission declarations

### Usage Access (PACKAGE_USAGE_STATS)

Core purpose: digital wellbeing. The app reads foreground app events solely
to detect when Instagram is in use, to run the session timer and to catch
direct opens. Data is processed on device and never transmitted. In-app
prominent disclosure is shown during onboarding before the permission is
requested.

### Accessibility Service (BIND_ACCESSIBILITY_SERVICE)

IsAccessibilityTool: false. Purpose: digital wellbeing. The service is
restricted to the Instagram package, watches only the Reels surface, counts
reel transitions, and shows a full screen question overlay between reels.
It does not capture text, credentials, or content from any app. Prominent
disclosure shown in onboarding; a demo video will be needed for review.

### REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

The app's core function (catching direct Instagram opens) requires a
background monitor that OEM battery managers otherwise kill. Requested
optionally during onboarding.

## Tip jar setup (Play Console)

1. Set up the merchant account in the console (Sri Lanka is supported,
   payouts in USD).
2. Monetize > Products > In-app products: create three products with the
   exact ids `tip_small`, `tip_medium`, `tip_large`. Suggested names and
   prices: "Small tip" 1.99 USD, "Nice tip" 4.99 USD, "Generous tip"
   9.99 USD. Activate all three.
3. The Support screen sorts by price and shows the console names, so
   name them what buyers should see.
4. Tips only work in builds installed through Play. Add your account as a
   license tester and verify the flow on the internal testing track.
5. Donation links (Patreon, Ko-fi, GitHub Sponsors) go on the project
   web page and README, not inside the Play build. For payouts from
   Sri Lanka, Patreon via Payoneer is the reliable rail; verify before
   publishing any link.

## Release checklist

- [ ] Decide final name and applicationId
- [ ] Host PRIVACY.md at a public URL (GitHub Pages works) and link it in
      the console and in onboarding
- [ ] `./gradlew bundleRelease` and upload the AAB
- [ ] Fill data safety + both permission declaration forms
- [ ] Record the accessibility demo video (open Instagram, show gate,
      show reel question)
- [ ] Screenshots: home, session sheet locked, reel question, history,
      settings
- [ ] Back up keystore/upload.keystore and keystore.properties somewhere
      safe; losing them means losing the ability to update the app
