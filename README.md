# Minstagram

An intentional Instagram client for Android. Before opening Instagram, you declare *why* you're opening it. The app then holds you to that intention by hiding algorithmic distractions, blocking Explore/Shop navigation, and showing you how long you've been on.

## Why

Instagram is designed to maximize time spent, not value delivered. Minstagram puts a single question between you and the feed: **what brings you here today?** That small friction is enough to make mindless scrolling a conscious choice.

## Features

- **Intention gate** — choose your purpose before the feed opens (Check DMs, Post a story, Browse feed, Watch Reels, or Just browsing)
- **Session banner** — always-visible timer and intention reminder while browsing
- **Distraction blocking** — Explore, Shop, Reels autoplay, and "Suggested for you" posts hidden via JavaScript injection
- **Navigation blocking** — SPA-level `/explore` and `/shop` routes intercepted at both the WebView and `history.pushState` level
- **Session summary** — time spent shown on exit with the option to stay or end the session

## Tech stack

- Kotlin + Jetpack Compose
- Android WebView (Instagram mobile web)
- Hilt (dependency injection)
- Room (local persistence, upcoming)
- Navigation Compose
- MVVM architecture with StateFlow

## Project status

Phase 1 complete — WebView shell, intention gate, distraction hiding, session timing.
