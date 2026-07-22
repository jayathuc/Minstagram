# Minstagram

A small Android app that puts a question between you and Instagram: why are you opening it, and are you still sure you want to keep scrolling?

You pick an intention (check DMs, post a story, watch Reels...) and a time budget. Minstagram then opens the real Instagram app, shows a floating banner with your intention and remaining time, and steps in when time runs out. While you watch Reels it counts them and slips a quick question in every so often, so watching stays a conscious act. If you open Instagram directly, Minstagram notices and asks the question first.

## How it works

Minstagram does not wrap or modify Instagram. It launches the normal app and works around it.

### The intention gate

- **Intention gate**: pick why you're here and how long you need before Instagram opens
- **Session banner**: a small floating overlay shows your intention and the time left
- **Honest timing**: the timer only counts while Instagram is actually on screen, and pauses when you switch away
- **Time's up**: a full-screen overlay when the budget runs out, or auto-close if you prefer
- **Direct open catch**: a background monitor spots Instagram being opened without a session and shows the gate, with a snooze if you really want to skip it
- **Easy exit**: one tap from the session sheet leaves for the home screen, or an app you choose, instead of dropping you back into the feed

### The Reels quiz gate

- **Conscious counting**: counts only the Reels you actually move forward through, anchored to Instagram's Reels pager, so the feed, stories, comments and scrolling back over the same Reels never count. Sponsored Reels are skipped too.
- **A question every few Reels**: after a configurable number of Reels, the current Reel is paused and a short multiple-choice question comes up. Answer it to carry on, or take the "I've got better things to do" exit.
- **A large, varied pool**: questions span twelve topics (math, geography, science, space, animals, the human body, numbers and shapes, time and calendar, everyday, history, words, sports). Math is generated so it never repeats, and correct answers show a small fact or a quiet well done.
- **No brute forcing**: a wrong answer locks the buttons for a moment that grows with each miss, then swaps in a fresh question, so blindly tapping one option gets you nowhere.

### After the session

- **Session summary**: how long you actually spent and how many Reels went by, shown when the session ends
- **History**: past sessions grouped by date, with a week or month usage chart

Everything runs on your device. No accounts, no servers, no analytics.

## Settings

- Choose how many Reels pass between questions
- Turn individual quiz topics on or off
- Pick where the exit button sends you (home screen or another app)
- Support the app through the tip jar

## Permissions it asks for

- **Display over other apps**: for the session banner, the time's up screen and the quiz
- **Notifications**: for the session timer notification
- **Usage access**: to know when Instagram is on screen
- **Accessibility service**: to count Reels and show the quiz over Instagram

## Tech stack

- Kotlin + Jetpack Compose
- Hilt
- Navigation Compose
- Room for session history
- Foreground services + UsageStatsManager for app detection
- An accessibility service for Reel counting and the quiz overlay
- Play Billing for the tip jar

## Status

Working end to end: intention gate, pause-aware session timer, overlay banner, expiry overlay, direct open interception with snooze, the Reels quiz gate with a large question bank, session history with a usage chart, an easy exit, and a tip jar. Survives reboots.
