TQuickSwap — Agent Guide

Overview
- Purpose: Maintain two separate player profiles (Survival/Creative) and swap them via `/swap`.
- Storage: Per-player compressed NBT under `world/tquickswap/` (e.g., `<uuid>-survival.nbt`).
- Entrypoints:
  - Fabric: `dev.tetralights.tquickswap.TQuickSwapMod` (`fabric/src/main/java`)
  - NeoForge: `dev.tetralights.tquickswap.TQuickSwapNeoForge` (`neoforge/src/main/java`)
- Command: `/swap [survival|creative]`.

Versions
- Minecraft: 1.21.10
- Fabric Loader: 0.16.17
- Fabric API: 0.110.0+1.21.10
- NeoForge: 21.1.215 (moddev-gradle 2.0.120 + neoform 1.21.10-20241111.144430)
- Fabric Loom: 1.10.1
- Gradle: 8.12 (wrapper)
- Java: JDK 21 (toolchain in each module)

- Build & Run
- Fabric build: `./gradlew :fabric:build`
- NeoForge build: `./gradlew :neoforge:build`
- All builds (clean): `./gradlew clean build`
- Fabric dev server/client: `./gradlew :fabric:runServer`, `./gradlew :fabric:runClient`
- NeoForge dev server/client: `./gradlew :neoforge:runServer`, `./gradlew :neoforge:runClient`
- Join: Connect your Fabric 1.21.10 client to `localhost:25565` and use `/swap`.

Key Files
- Mod entry: `fabric/src/main/java/dev/tetralights/tquickswap/TQuickSwapMod.java` (Fabric)
- Mod entry: `neoforge/src/main/java/dev/tetralights/tquickswap/TQuickSwapNeoForge.java` (NeoForge)
- Storage / ops / helpers: `common/src/main/java/dev/tetralights/tquickswap/DualStore.java`, `.../ProfileOps.java`, `.../SwapCommands.java`, etc.
- Shared strings: `common/src/main/resources/assets/tquickswap/lang`
- Fabric metadata: `fabric/src/main/resources/fabric.mod.json`
- NeoForge metadata: `neoforge/src/main/resources/META-INF/mods.toml`

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
- Mojang mappings (official) for 1.21.10 via Loom.
- No example mod residue (com.example.* removed). If you still see `fabric/src/main/resources/assets/modid/icon.png`, it’s unused; safe to delete.

Release
1. Build the desired loader artifact (`:fabric:build` or `:neoforge:build`, JARs under `<loader>/build/libs/`).
2. Drop that JAR plus the corresponding Fabric API or NeoForge runtime into the server’s `mods/` directory.

Changelogs
- Always add a new changelog entry for releases; do not remove old entries.
- Determine current version from `src/main/resources/fabric.mod.json` (`version` field) before editing; bump it there and keep notes in `CHANGELOG.md`.
3. Start server: `java -Xmx4G -Xms2G -jar fabric-server-launch.jar nogui`

Troubleshooting
- Loom/Gradle mismatch: Project pins Loom 1.10.1 and Gradle 8.12.
- NeoForge tooling: `net.neoforged.moddev` (2.0.120) requires `moddev` repository (already configured in root build script).
- Permissions: You may need to `op` your player to use `/swap`.
