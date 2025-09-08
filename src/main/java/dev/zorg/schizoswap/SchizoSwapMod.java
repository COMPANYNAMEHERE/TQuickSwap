package dev.zorg.schizoswap;


import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;


import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class SchizoSwapMod implements ModInitializer {
    @Override public void onInitialize() {
        // Command: /profileswap [survival|creative]
        CommandRegistrationCallback.EVENT.register((dispatcher, reg, env) -> dispatcher.register(
            literal("profileswap")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    DualStore store = DualStore.of(ctx.getSource().getServer());
                    ProfileType target = (p.interactionManager.getGameMode()==GameMode.CREATIVE) ? ProfileType.SURVIVAL : ProfileType.CREATIVE;
                    ProfileOps.swapTo(p, target, store);
                    ctx.getSource().sendFeedback(() -> Text.literal("Switched to " + target), true);
                    return 1;
                })
                .then(argument("mode", StringArgumentType.word())
                    .suggests((c,b)->{ b.suggest("survival"); b.suggest("creative"); return b.buildFuture(); })
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        ProfileType target = ProfileType.valueOf(StringArgumentType.getString(ctx, "mode").toUpperCase());
                        DualStore store = DualStore.of(ctx.getSource().getServer());
                        ProfileOps.swapTo(p, target, store);
                        ctx.getSource().sendFeedback(() -> Text.literal("Switched to " + target), true);
                        return 1;
                    })
                )
        ));


        // Optional: auto-save on quit, auto-load on join
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var p = handler.player; var store = DualStore.of(server);
            var current = p.interactionManager.getGameMode()==GameMode.CREATIVE ? ProfileType.CREATIVE : ProfileType.SURVIVAL;
            store.save(p.getUuid(), current, ProfileOps.capture(p));
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player; var store = DualStore.of(server);
            var last = store.last(p.getUuid());
            var nbt = store.load(p.getUuid(), last);
            if(!nbt.isEmpty()) ProfileOps.apply(p, nbt);
        });
    }
}
