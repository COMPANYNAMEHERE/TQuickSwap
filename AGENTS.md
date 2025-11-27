TQuickSwap — Agent Guide

Overview
- Purpose: Maintain two separate player profiles (Survival/Creative) and swap them via `/swap`.
- Storage: Per-player compressed NBT under `world/tquickswap/` (e.g., `<uuid>-survival.nbt`).
- Entrypoints: `dev.tetralights.tquickswap.TQuickSwapMod` in `fabric/src/main/java` (Fabric) and `neoforge/src/main/java` (NeoForge).
- Command: `/swap [survival|creative]`.

Versions
- Minecraft: 1.21.8
- Fabric Loader: 0.16.13
- Fabric API: 0.133.4+1.21.8
- Fabric Loom: 1.10.1
- Gradle: 8.12
- Java: JDK 21 (via toolchain in build.gradle)

Build & Run
- Build all loaders: `./gradlew clean buildAll` (Fabric + NeoForge)
- Fabric dev server: `./gradlew :fabric:runServer` (accept EULA in `run/eula.txt` on first run)
- Fabric dev client: `./gradlew :fabric:runClient`
- NeoForge dev server/client: `./gradlew :neoforge:runServer` / `./gradlew :neoforge:runClient`
- Join: Connect your Fabric 1.21.8 client to `localhost:25565` and use `/swap`.

Key Files
- Mod entry: `src/main/java/dev/tetralights/tquickswap/TQuickSwapMod.java`
- Storage: `src/main/java/dev/tetralights/tquickswap/DualStore.java`
- Apply/Capture: `src/main/java/dev/tetralights/tquickswap/ProfileOps.java`
- Mod metadata: `src/main/resources/fabric.mod.json`

Data Locations
- Profiles: `world/tquickswap/<uuid>-survival.nbt`, `...-creative.nbt`
- Last profile flag: `world/tquickswap/<uuid>-last.nbt`

Common Tasks
- Swap profile: `/swap` or `/swap survival|creative`
- Clear caches if mappings get weird:
  - `rm -rf .gradle/ ~/.gradle/caches/modules-2/files-2.1/net.fabricmc.yarn`
  - `rm -rf ~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api`
- Verify Java 21: `./gradlew --version`

Conventions
- Mojang mappings (official) for 1.21.8 via Loom.
- No example mod residue (com.example.* removed). If you still see `src/main/resources/assets/modid/icon.png`, it’s unused; safe to delete.

Release (standalone server)
1. Build JAR: `./gradlew build` (output in `build/libs/`)
2. On a Fabric server (1.21.8), place TQuickSwap JAR and Fabric API in `mods/`

Changelogs
- Always add a new changelog entry for releases; do not remove old entries.
- Determine current version from `src/main/resources/fabric.mod.json` (`version` field) before editing; bump it there and keep notes in `CHANGELOG.md`.
3. Start server: `java -Xmx4G -Xms2G -jar fabric-server-launch.jar nogui`

Troubleshooting
- Loom/Gradle mismatch: Project pins Loom 1.10.1 and Gradle 8.12.
- Permissions: You may need to `op` your player to use `/swap`.
