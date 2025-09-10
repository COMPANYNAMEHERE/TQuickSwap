
Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/

# TQuickSwap (NeoForge 1.21.8)

Fast, minimal mod to toggle player gamemode. Built for **NeoForge** on **Minecraft 1.21.8**.

## Requirements
- **Java 21**
- **Gradle Wrapper** (use the included `./gradlew`)
- **NeoForge** dev environment
- IDE: IntelliJ IDEA or VS Code with Java

## Getting Started (Dev)

```bash
# 1) Clone
git clone https://github.com/COMPANYNAMEHERE/TQuickSwap.git
cd TQuickSwap

# 2) Generate/refresh IDE files
./gradlew idea        # IntelliJ
# or
./gradlew eclipse     # Eclipse (if you use it)

# 3) Run
./gradlew runClient
./gradlew runServer --args="--nogui"
```

## Build

```bash
# Build release jar
./gradlew build
# Output: build/libs/<modid>-<version>.jar
```

## Install (User/Server)

1. Download the built jar from `build/libs` or your Releases.
2. Drop the jar into the `mods/` folder of a **NeoForge 1.21.8** client or server.
3. Start the game or server.

## Project Layout

- `src/main/java` — Mod source
- `src/main/resources` — Assets and `META-INF`
- `src/main/templates` — `mods.toml` template expanded at build
- `build.gradle` — Uses `net.neoforged.moddev` **2.0.107**, Java **21**, optional Parchment mappings

## Common Tasks

```bash
# Data generation (if used)
./gradlew runData

# Publish to local maven (optional)
./gradlew publish
```

## Configuration

- Java toolchain is pinned to **21** in `build.gradle`.
- NeoForge version, mappings, and mod metadata are read from `gradle.properties`.
- Run configs: `runClient`, `runServer`, `gameTestServer`, `runData`.

## Compatibility

- **Minecraft:** 1.21.8  
- **Loader:** NeoForge

## License

Specify your license here. Example: MIT.

---

_Notes:_  
- Replace organization and links as needed.  
- If you expose commands or keybinds for swapping gamemode, document them here.
