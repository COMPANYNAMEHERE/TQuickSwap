package dev.zorg.schizoswap;


import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;


public class SchizoSwapMod implements ModInitializer {
    @Override public void onInitialize() {
        // Command: /profileswap [survival|creative]
        CommandRegistrationCallback.EVENT.register((dispatcher, reg, env) -> dispatcher.register(
            Commands.literal("profileswap")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    DualStore store = DualStore.of(ctx.getSource().getServer());
                    ProfileType target = (p.isCreative()) ? ProfileType.SURVIVAL : ProfileType.CREATIVE;
                    ProfileOps.swapTo(p, target, store);
                    ctx.getSource().sendSuccess(() -> Component.literal("Switched to " + target), true);
                    return 1;
                })
                .then(Commands.argument("mode", StringArgumentType.word())
                    .suggests((c,b)->{ b.suggest("survival"); b.suggest("creative"); return b.buildFuture(); })
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        ProfileType target = ProfileType.valueOf(StringArgumentType.getString(ctx, "mode").toUpperCase());
                        DualStore store = DualStore.of(ctx.getSource().getServer());
                        ProfileOps.swapTo(p, target, store);
                        ctx.getSource().sendSuccess(() -> Component.literal("Switched to " + target), true);
                        return 1;
                    })
                )
        ));


        // Optional: auto-save on quit, auto-load on join
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var p = handler.player; var store = DualStore.of(server);
            var current = p.isCreative() ? ProfileType.CREATIVE : ProfileType.SURVIVAL;
            store.save(p.getUUID(), current, ProfileOps.capture(p));
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player; var store = DualStore.of(server);
            var last = store.last(p.getUUID());
            var nbt = store.load(p.getUUID(), last);
            if(!nbt.isEmpty()) ProfileOps.apply(p, nbt);
        });
    }
}
