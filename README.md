# SchizoSwap

SchizoSwap lets players maintain two separate in-game profiles (Survival and Creative) and swap between them with a command. Each profile stores inventory, ender chest, XP, health/food, potion effects, abilities, and position/world.

## Features
- Dual profiles: Survival and Creative kept separate
- Full state swap: inventory, ender chest, XP/level, health, hunger, saturation, status effects, flight flags
- Position/world restore: returns you to the saved world and coordinates
- Simple command: switch with a single `/profileswap` call
- Auto-persist: optional auto-save on disconnect and auto-load on join

## How It Works
- Capture: When you swap, SchizoSwap captures your current profile state into an NBT blob (inventory, ender, XP, health/food, effects, abilities, position/world).
- Store: The data is stored as compressed NBT files per player and profile under the world save folder (`world/schizoswap/`).
- Apply: When switching profiles (or on join), it reads the saved NBT and applies it back to the player, then switches the game mode appropriately.

## Commands
- `/profileswap` — toggles between Survival and Creative
- `/profileswap <survival|creative>` — switches explicitly to a target profile

Note: Command requires permission to run commands normally allowed for players with access to command usage (server config dependent).

## Compatibility
- Minecraft: 1.21.1
- Loader: Fabric Loader 0.16.x
- Fabric API: 0.110.0+1.21.1


<details>
  <summary><strong>Setup</strong> (click to expand)</summary>

  <h4>For Players/Servers</h4>
  <ol>
    <li>Install Fabric Loader (matching your Minecraft version, 1.21.1).</li>
    <li>Install Fabric API (version compatible with 1.21.1).</li>
    <li>Drop the SchizoSwap mod JAR into the `mods/` folder.</li>
    <li>Start the game or server. Ensure command permissions allow use of `/profileswap`.</li>
  </ol>

  <h4>For Developers</h4>
  <ol>
    <li>Java 21 toolchain installed.</li>
    <li>Gradle project includes:
      <ul>
        <li>Minecraft: 1.21.1</li>
        <li>Fabric Loader: 0.16.7</li>
        <li>Fabric API: 0.110.0+1.21.1</li>
      </ul>
    </li>
    <li>Run `gradlew runClient` / `gradlew runServer` for local testing.</li>
  </ol>

  <h4>Troubleshooting</h4>
  <ul>
    <li>If swapping does nothing, check server logs for permission or mapping conflicts.</li>
    <li>Ensure Fabric API is present on both client and server when required.</li>
    <li>Delete per-player NBT files in `world/schizoswap/` if your data format changed during development.</li>
  </ul>

</details>
