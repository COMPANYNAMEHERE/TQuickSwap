# TQuickSwap

TQuickSwap lets players maintain two separate in-game profiles (Survival and Creative) and swap between them with a command. Each profile stores inventory, ender chest, XP, health/food, potion effects, abilities, and position/world.

Current version: 1.4.1 (Fabric 1.21.8)

## Features
- Dual profiles: separate Survival and Creative states
- Full state swap: inventory, ender chest, XP/level, health, hunger, saturation, status effects, flight flags
- Position/world restore: returns you to the saved world and coordinates
- Gamemode alignment toggle: optionally set gamemode to match the target profile
- Teleport distance logging: logs distance moved by the swap (before vs after apply)
- Auto-persist: save on disconnect and apply on join

## How It Works
- Capture: On swap, TQuickSwap captures your current profile state into an NBT blob (inventory, ender, XP, health/food, effects, abilities, position/world).
- Store: Data is stored as compressed NBT per-player per-profile under the world save folder.
- Apply: When switching (or on join), the saved NBT is applied back to the player. With the config toggle ON, gamemode is set to match the active profile; with it OFF, each profile keeps its own saved gamemode.

## Commands
- `/swap` — toggle between Survival and Creative
- `/swap <survival|creative>` — switch explicitly to a target profile
- `/swap status` — show current profile and last save timestamps
- `/swap help` — show help and repository link
- `/swap config help` — show config options
- `/swap config gamemode` — toggle “switch gamemode on swap”

Permissions:
- `/swap config …` requires OP (permission level 3). Non-OPs get a clear error message.
- `/swap` itself is available to all players by default.

## Data Locations
- Profiles: `world/tquickswap/<uuid>-survival.nbt`, `world/tquickswap/<uuid>-creative.nbt`
- Last profile flag: `world/tquickswap/<uuid>-last.nbt`

## Compatibility
- Minecraft: 1.21.8
- Fabric Loader: 0.16.13
- Fabric API: 0.133.4+1.21.8

## Build & Run
- Build everything: `./gradlew clean buildAll` (builds Fabric and NeoForge)
- Fabric dev server: `./gradlew :fabric:runServer` (accept EULA in `run/eula.txt` on first run)
- Fabric dev client: `./gradlew :fabric:runClient`
- NeoForge dev client/server: `./gradlew :neoforge:runClient` / `./gradlew :neoforge:runServer`

## Troubleshooting
- Permissions: Make sure your player is OP for `/swap config …`.
- Mappings cache glitches: clear Gradle/Yarn caches if classes can’t be resolved.
- Data reset: Delete per-player files in `world/tquickswap/` if you changed data formats during development (backup first).

## Changelog
See `CHANGELOG.md` for release notes.
