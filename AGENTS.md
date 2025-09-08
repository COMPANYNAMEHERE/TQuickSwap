SchizoSwap — Agent Guide

Overview
- Purpose: Maintain two separate player profiles (Survival/Creative) and swap them via `/profileswap`.
- Storage: Per-player compressed NBT under `world/schizoswap/` (e.g., `<uuid>-survival.nbt`).
- Entrypoint: `dev.zorg.schizoswap.SchizoSwapMod` in `src/main/java`.
- Command: `/profileswap [survival|creative]`.

Versions
- Minecraft: 1.21.1
- Fabric Loader: 0.16.7
- Fabric API: 0.110.0+1.21.1
- Fabric Loom: 1.7.4
- Gradle: 8.10.2
- Java: JDK 21 (via toolchain in build.gradle)

Build & Run
- Build: `./gradlew clean build`
- Dev server: `./gradlew runServer` (accept EULA in `run/eula.txt` on first run)
- Dev client: `./gradlew runClient`
- Join: Connect your Fabric 1.21.1 client to `localhost:25565` and use `/profileswap`.

Testing
- Game tests: `./gradlew runGameTestServer`
  - Validates command registration and default store behavior.
  - Tests live in `src/testmod/java` and are enabled via Fabric GameTest API.

Key Files
- Mod entry: `src/main/java/dev/zorg/schizoswap/SchizoSwapMod.java`
- Storage: `src/main/java/dev/zorg/schizoswap/DualStore.java`
- Apply/Capture: `src/main/java/dev/zorg/schizoswap/ProfileOps.java`
- Mod metadata: `src/main/resources/fabric.mod.json`
- Testmod metadata: `src/testmod/resources/fabric.mod.json`

Data Locations
- Profiles: `world/schizoswap/<uuid>-survival.nbt`, `...-creative.nbt`
- Last profile flag: `world/schizoswap/<uuid>-last.nbt`

Common Tasks
- Swap profile: `/profileswap` or `/profileswap survival|creative`
- Clear caches if mappings get weird:
  - `rm -rf .gradle/ ~/.gradle/caches/modules-2/files-2.1/net.fabricmc.yarn`
  - `rm -rf ~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api`
- Verify Java 21: `./gradlew --version`

Conventions
- Yarn mappings for 1.21.1 (`1.21.1+build.10:v2`).
- No example mod residue (com.example.* removed). If you still see `src/main/resources/assets/modid/icon.png`, it’s unused; safe to delete.

Release (standalone server)
1. Build JAR: `./gradlew build` (output in `build/libs/`)
2. On a Fabric server (1.21.1), place SchizoSwap JAR and Fabric API in `mods/`
3. Start server: `java -Xmx4G -Xms2G -jar fabric-server-launch.jar nogui`

Troubleshooting
- Loom/Gradle mismatch: Project pins Loom 1.7.4 and Gradle 8.10.2.
- Missing symbols: Ensure Yarn mappings are used (already configured in build.gradle).
- Permissions: You may need to `op` your player to use `/profileswap`.

