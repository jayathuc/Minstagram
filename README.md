# Minstagram

A small Android app that puts a question between you and Instagram: why are you opening it?

You pick an intention (check DMs, post a story, watch Reels...) and a time budget. Minstagram then opens the real Instagram app, shows a floating banner with your intention and remaining time, and steps in when time runs out. If you open Instagram directly, Minstagram notices and asks the question first.

## How it works

Minstagram does not wrap or modify Instagram. It launches the normal app and works around it:

- **Intention gate**: pick why you're here and how long you need before Instagram opens
- **Session banner**: a small floating overlay shows your intention and the time left
- **Honest timing**: the timer only counts while Instagram is actually on screen, and pauses when you switch away
- **Time's up**: a full-screen overlay when the budget runs out, or auto-close if you prefer
- **Direct open catch**: a background monitor spots Instagram being opened without a session and shows the gate, with a 30 minute snooze if you really want to skip it
- **Session summary**: how long you actually spent, shown when the session ends

Everything runs on your device. No accounts, no servers, no analytics.

## Permissions it asks for

- **Display over other apps**: for the session banner and the time's up screen
- **Notifications**: for the session timer notification
- **Usage access**: to know when Instagram is on screen

## Tech stack

- Kotlin + Jetpack Compose
- Hilt
- Navigation Compose
- Foreground services + UsageStatsManager for app detection
- Room (planned, for session history)

## Status

Launcher architecture working: intention gate, pause-aware session timer, overlay banner, expiry overlay, direct open interception with snooze, survives reboots. Next up: session history and a usage dashboard.
