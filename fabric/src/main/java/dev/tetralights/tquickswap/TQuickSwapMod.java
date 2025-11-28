package dev.tetralights.tquickswap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public class TQuickSwapMod implements ModInitializer {
    @Override
    public void onInitialize() {
        TQuickSwap.setConfigDir(FabricLoader.getInstance().getConfigDir());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SwapCommands.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CommonHooks.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CommonHooks.onPlayerQuit(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> CommonHooks.onServerStopping());
    }
}
