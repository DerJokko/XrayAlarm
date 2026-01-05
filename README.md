# XrayAlarm ✅
A small Fabric mod that helps server admins detect suspicious mining behavior and notify staff via chat or webhooks.

---

## Features
- Detects rapid/suspicious ore mining per tracked block
- Sends alerts to chat and/or Discord webhooks
- Optionally send player coordinates on logout (configurable)
- Customizable logout message with placeholders: `{player}`, `{x}`, `{y}`, `{z}`, `{world}`, `{dimension}`
- Per-mod config with sensible defaults and editable tracked blocks
- Runtime commands to configure behavior

## Installation
1. Build with Gradle: `./gradlew build`
2. Place the generated mod JAR from `build/libs/` into your `mods/` folder

## Configuration
- Default config (shipped): `src/main/resources/default-xray-alarm.json`
- Active config (created on first run): `%minecraft_folder%/config/xray-alarm.json`

Key config fields:
- `enabled` (boolean): master enable/disable
- `useChat` (boolean): send chat notifications
- `webhook.enabled` / `webhook.url` / `webhook.avatarUrl`: enable and target for webhooks, optional avatar URL
- `webhook.usePingRole` / `webhook.pingRole`: optional Discord role pinging for alerts
- `general.coordinatesOnLeave` (boolean): send coordinates on player logout
- `general.logoutMessage` (string): template for logout messages; supports `{player}`, `{x}`, `{y}`, `{z}`, `{world}`, `{dimension}`
- `tracked_blocks`: list of blocks with thresholds and alert messages

## Commands
Use `/xrayAlarm` with these subcommands:
- `setWebhook <url>` — set webhook URL
- `useWebhook <true|false>` — toggle webhook usage
- `useChat <true|false>` — toggle chat notifications
- `setPingRole <id>` — set Discord ping role id
- `usePingRole <true|false>` — enable role pings
- `coordinatesOnLeave <true|false>` — toggle sending coordinates when a player disconnects
- `toggle` — enable/disable the mod
- `info` — show current config summary

## Development & Contribution
- Open a PR for features or fixes and keep changes small and focused.
- Run `./gradlew build` to test compile and run in your dev environment.

## License
GPL-3.0-or-later — see `LICENSE` for details.

---

Made with ❤️ — keep your server fair and fun.