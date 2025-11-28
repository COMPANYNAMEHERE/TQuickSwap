# TQuickSwap

TQuickSwap lets players maintain two separate in-game profiles (Survival and Creative) and swap between them with a command. Each profile stores inventory, ender chest, XP, health/food, potion effects, abilities, and position/world.

Current version: 1.5.1 (Fabric/NeoForge 1.21.10)

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
- Minecraft: 1.21.10
- Fabric Loader: 0.16.17
- Fabric API: 0.110.0+1.21.10
- NeoForge: 21.1.215 (moddev-gradle 2.0.120)

## Build & Run

The recommended way to run TQuickSwap in dev (Fabric or NeoForge) is:

- `./run-loader.sh`

This script:
- Prompts you for the loader (`fabric` or `neoforge`)
- Cleans the corresponding `run/` directory
- Ensures a Java 21 runtime is available (downloads Temurin 21 under `/tmp/jdks` if needed)
- Starts the appropriate `runClient` Gradle task
- Produces loader-specific artifacts under `<loader>/build/libs/` when you build via the script or manually (e.g., `fabric/build/libs/tquickswap-fabric-1.5.1.jar` and `neoforge/build/libs/tquickswap-neoforge-1.5.1.jar`).

<details>
<summary>Manual commands (if <code>run-loader.sh</code> doesn&apos;t work)</summary>

- Fabric build: `./gradlew :fabric:build`
- NeoForge build: `./gradlew :neoforge:build`
- All builds: `./gradlew clean build` (runs both loaders)
- Fabric dev server: `./gradlew :fabric:runServer` (accept EULA in `run/eula.txt` on first run)
- NeoForge dev server: `./gradlew :neoforge:runServer`
- Fabric dev client: `./gradlew :fabric:runClient`
- NeoForge dev client: `./gradlew :neoforge:runClient`

</details>

## Troubleshooting
- Permissions: Make sure your player is OP for `/swap config …`.
- Mappings cache glitches: clear Gradle/Yarn caches if classes can’t be resolved.
- Java 21 required: The helper script `run-loader.sh` ensures a Temurin JDK 21 is available under `/tmp/jdks`. If the tmp directory is cleared, rerun the script or reinstall the JDK and set `JAVA_HOME`.
- Data reset: Delete per-player files in `world/tquickswap/` if you changed data formats during development (backup first).

## Changelog
See `CHANGELOG.md` for release notes.
