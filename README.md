# TQuickSwap 1.5.0

TQuickSwap is a lightweight **NeoForge** mod for Minecraft **1.21.1** that lets players flip between dedicated Survival and Creative profiles without losing progress. Inventories, ender chests, location, XP, effects, abilities, and gamemode preferences are all stored per-profile in the world save, and two rolling backups are kept for safety.

## What's New in 1.5.0
- Asynchronous, checksummed saves and automatic fallback to the latest healthy snapshot.
- `/swap stats` shows aggregate usage, backups, and corruption counters.
- Localised chat output (English with fallback) plus bundled German, Dutch, Russian, and Simplified Chinese packs.
- Chat menu `/swap menu` with clickable toggles for gamemode sync and notifications.

## Commands
| Command | Access | Description |
|---------|--------|-------------|
| `/swap` | Player | Toggle to the opposite profile (Survival ⇄ Creative).
| `/swap <survival|creative>` | Player | Jump directly to a profile.
| `/swap status` | Player | Show current profile and last-save timestamps (with backups).
| `/swap menu` | Player | Clickable control panel for swaps, configs, and backup restore.
| `/swap stats` | OP | Summaries of swaps, backups, and corruption detections (non-OPs get a friendly denial message).
| `/swap restore <profile> <slot>` | OP | Promote backup slot `1` or `2` for the profile (applies immediately if it is the active profile).
| `/swap config <gamemode|alerts|backupalerts|reload>` | OP | Toggle gamemode syncing, post-swap alerts, backup alerts, or reload config.

## Notifications & Backups
- Every swap can optionally push a chat summary with distance travelled (toggle via `/swap config alerts`).
- If a backup is auto-loaded or manually applied, both the acting player and console receive clear messages.
- Saves are written off-thread with SHA-1 checksums and two rolling backups per profile (`world/tquickswap/<uuid>-*.nbt`).

## Installation
1. Drop the built jar from `build/libs` into your `mods/` folder on both client and server.
2. Launch with **NeoForge 21.1.208** and Java **21**.
3. Customise behaviour in `config/tquickswap-common.toml` or via `/swap config` in-game.

## Developing
```bash
# Generate IntelliJ project files
./gradlew idea

# Run dev server (console accepts commands)
./gradlew Server

# Build release jar
./gradlew build
```
Outputs land in `build/libs/`.

## Localisation Support
Built-in language packs live in `assets/tquickswap/lang/`:
- `en_us` (default fallback)
- `de_de`
- `nl_nl`
- `ru_ru`
- `zh_cn`

If a player selects an unsupported locale, the server logs a warning and they seamlessly fall back to English.

## License
`mod_license` is currently set to **All Rights Reserved**. Update `gradle.properties` and this section if you adopt a different licence.
