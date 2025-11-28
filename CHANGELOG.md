# Changelog

All notable changes to this project will be documented in this file.

## TQuickSwap 1.5.1 (1.21.10)
- Bump the mod release version to 1.5.1 for the 1.21.10 Fabric and NeoForge builds.
- Keep release notes and metadata aligned across both loaders.

## TQuickSwap 1.4.2 (1.21.10)
- Update Fabric and NeoForge builds to Minecraft 1.21.10 (Fabric Loader 0.16.17, Fabric API 0.110.0+1.21.10).

## TQuickSwap 1.4.1 (1.21.8)
- Port latest NeoForge logic to Fabric 1.21.8.
- Distance metric now measures teleport delta (position before vs after apply).
- `/swap config` and `/swap config gamemode` show clear OP-only messages when lacking permission.
- Keep last-profile logic and optional gamemode alignment on join/swap.
- Keep full profile persistence: position/world, inventory, ender chest, XP, health/food, effects, abilities.

## TQuickSwap 1.1.1 (1.21.8)
- Rebrand to TQuickSwap across UI and docs.
- Add lightweight logging for saves/loads and swaps.
- Add `/swap status` to show current profile and last save times.
- Keep full profile persistence (inventory, ender chest, effects, XP, health/food, abilities, position/world).
- Remove advancement-related features and config.
- Build for Minecraft 1.21.8, Fabric Loader 0.16.13, Fabric API 0.133.4+1.21.8.
