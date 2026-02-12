<div align="center">

# Music Assistant Companion

**The Multiscreen Music Assistant Client — Phone, Tablet & Android TV**

A fully-functional, community-driven fork of the [Music Assistant Mobile App](https://github.com/music-assistant/mobile-app), extended with **Android TV support** and ongoing improvements. Built with Kotlin Multiplatform (KMP) and Compose Multiplatform — one codebase, every screen.

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_|_Android_TV_|_iOS-green.svg)](#architecture)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#contributing)

---

[Features](#features) · [Architecture](#architecture) · [Getting Started](#getting-started) · [Contributing](#contributing) · [Changelog](CHANGELOG.md)

</div>

## Why This Fork Exists

We built full [Android TV support](https://github.com/music-assistant/mobile-app/pull/88) — D-pad navigation, leanback launcher integration, media remote keys, and more — and submitted it upstream to the original Music Assistant mobile app. The upstream maintainers didn't seem interested in merging these features, so here we are.

This project is **100% functional** and actively maintained. We sync upstream changes when applicable, and our focus is delivering the best Music Assistant experience across all screen sizes — from your pocket to your living room.

> **This started because we simply wanted the functionality.** We're happy to share it with the community and welcome anyone who wants to help make it even better.

---

## Features

### All Platforms
- Manage Music Assistant player queues and playback
- Local playback on device from your MA library
- Comprehensive Settings screen with section-based UI
- Server connection, authentication (built-in / OAuth), and local player configuration

### Android Mobile
- Background playback via media service
- System media notification with quick player controls
- Android Auto support for the built-in player

### Android TV
- Full leanback launcher integration with TV banner
- Native left NavigationRail — Home, Library, Search, Now Playing, Settings
- Complete D-pad focus management throughout the entire app
- Media remote key support — play/pause, next, previous
- Speaker selector dialog replacing swipe gestures
- Volume dialog with +/- buttons replacing sliders
- Larger grid cells and controls optimized for the 10-foot UI
- Visible three-dot menus on playable items (long-press isn't discoverable with a remote)
- WebView-based OAuth for TV (Chrome Custom Tabs unavailable on TV)
- **Zero changes to the phone experience** — all TV code is gated behind `PlatformType.TV`

### Settings & Authentication *(Improved)*
- Refactored section-based layout with Material3 cards
- Fixed OAuth login flow (HomeAssistant, etc.) with retry logic
- Fixed credential error messages and logout behavior
- Instant toggle for local Sendspin player — no restart required
- Proper state management across all connection and auth states

---

## Architecture

Built on **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, sharing a single codebase across every target:

| Platform | Status |
|---|---|
| Android Phone / Tablet | **Fully functional** |
| Android TV | **Fully functional** |
| iOS | In progress (upstream) |

The app interfaces with the [Music Assistant Server](https://github.com/music-assistant/server), an open-source media library manager that integrates with various streaming services and connected speakers. The server runs on devices like a Raspberry Pi, NAS, or Intel NUC and acts as the central hub for all your music.

---

## Getting Started

### Download

Grab the latest debug APK from the [Releases page](https://github.com/anthonymkz/music-assistant-companion/releases).

> *This is a debug build for testing. Production releases are on the roadmap.*

### Android Auto Setup

1. **Enable developer mode** in Android Auto — tap `Version and permission info` repeatedly until the dialog appears
2. Open the overflow menu (**⋮**) → **Developer settings** → Enable **Unknown sources**
3. Customize your launcher to show Music Assistant
4. Set up a VPN so the app can reach your MA server (exclude Android Auto from the VPN)

---

## Contributing

**We're actively looking for contributors.** This project was born out of wanting functionality that didn't exist, and we'd love help from the community.

Whether you're a Kotlin developer, a designer, an iOS specialist, or just someone who uses Music Assistant and wants to help test — pull up a chair.

### How to Contribute

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes
4. Open a Pull Request

We review PRs promptly and will work with you to get them merged. We'll also do our best to keep pulling in relevant changes from the upstream codebase so nothing falls behind.

---

## License

This project is licensed under the **Apache License 2.0** — see the [LICENSE](LICENSE) file for details.

## Acknowledgments

This project is a fork of the [Music Assistant Mobile App](https://github.com/music-assistant/mobile-app) by the [Music Assistant](https://github.com/music-assistant) organization. We acknowledge and thank the original developers and contributors whose work this project is built upon:

- [@formatBCE](https://github.com/formatBCE)
- [@seadowg](https://github.com/seadowg)
- [@DerPicknicker](https://github.com/DerPicknicker)
- [@tavva](https://github.com/tavva)
- [@RemakingEden](https://github.com/RemakingEden)
- [@marcelveldt](https://github.com/marcelveldt)
- [@matheusrodacki](https://github.com/matheusrodacki)
- [@RyanMorash](https://github.com/RyanMorash)

The Music Assistant server project is maintained at [music-assistant.io](https://music-assistant.io/).
