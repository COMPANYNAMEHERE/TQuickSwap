package dev.zorg.schizoswap;


import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;


public class SchizoSwapMod implements ModInitializer {
    @Override public void onInitialize() {
        // Command: /swap [survival|creative]
        CommandRegistrationCallback.EVENT.register((dispatcher, reg, env) -> dispatcher.register(
            Commands.literal("swap")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    DualStore store = DualStore.of(ctx.getSource().getServer());
                    ProfileType target = (p.isCreative()) ? ProfileType.SURVIVAL : ProfileType.CREATIVE;
                    ProfileOps.swapTo(p, target, store);
                    ctx.getSource().sendSuccess(() -> Component.literal("Switched to " + target), true);
                    return 1;
                })
                .then(Commands.literal("help").executes(ctx -> {
                    var src = ctx.getSource();
                    src.sendSuccess(() -> title("SchizoSwap"), false);
                    src.sendSuccess(() -> line("Swap between Survival and Creative profiles."), false);
                    src.sendSuccess(() -> line("Usage: ", "/swap", " ", "[survival|creative]", ChatFormatting.YELLOW), false);
                    src.sendSuccess(() -> line("Examples: ", "/swap", "  or  ", "/swap survival", ChatFormatting.GRAY), false);
                    src.sendSuccess(() -> link("Source ", "https://github.com/COMPANYNAMEHERE/ShizoSwitch", "(click)") , false);
                    return 1;
                }))
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

    private static Component title(String name) {
        return Component.literal("== ").withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal(name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" ==").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component line(String prefix, String a, String b, String c, ChatFormatting color) {
        return Component.literal(prefix).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(a).withStyle(color))
            .append(Component.literal(b).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(c).withStyle(color));
    }

    private static Component line(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static Component link(String prefix, String url, String suffix) {
        return Component.literal(prefix).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(url)
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(url)))))
            .append(Component.literal(" " + suffix).withStyle(ChatFormatting.DARK_GRAY));
    }
}
