package dev.tetralights.tquickswap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Locale;

@Mod(TQuickSwapMod.MODID)
public class TQuickSwapMod {
    public static final String MODID = "tquickswap";

    public TQuickSwapMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerQuit);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // placeholder for potential future setup; kept for parity with Fabric.
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("swap")
            .executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                DualStore store = DualStore.of(ctx.getSource().getServer());
                ProfileType current = store.last(p.getUUID());
                ProfileType target = (current == ProfileType.CREATIVE) ? ProfileType.SURVIVAL : ProfileType.CREATIVE;
                ProfileOps.swapTo(p, target, store);
                ctx.getSource().sendSuccess(() -> Component.translatable("message.tquickswap.switched", target.displayName()).withStyle(ChatFormatting.AQUA), true);
                return 1;
            })
            .then(Commands.literal("status").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                var server = ctx.getSource().getServer();
                DualStore store = DualStore.of(server);
                ProfileType current = store.last(p.getUUID());
                var surv = store.lastModified(p.getUUID(), ProfileType.SURVIVAL).map(TQuickSwapMod::fmtInstant).orElse("never");
                var creat = store.lastModified(p.getUUID(), ProfileType.CREATIVE).map(TQuickSwapMod::fmtInstant).orElse("never");
                ctx.getSource().sendSuccess(() -> title("TQuickSwap Status"), false);
                ctx.getSource().sendSuccess(() -> line("Current: ", current.displayName(), "", "", ChatFormatting.AQUA), false);
                ctx.getSource().sendSuccess(() -> line("Saved (Survival): ", surv, "", "", ChatFormatting.GRAY), false);
                ctx.getSource().sendSuccess(() -> line("Saved (Creative): ", creat, "", "", ChatFormatting.GRAY), false);
                return 1;
            }))
            .then(Commands.literal("help").executes(ctx -> {
                var src = ctx.getSource();
                src.sendSuccess(() -> title("TQuickSwap"), false);
                src.sendSuccess(() -> line("Swap between Survival and Creative profiles."), false);
                src.sendSuccess(() -> line("Usage: ", "/swap", " ", "[survival|creative]", ChatFormatting.YELLOW), false);
                src.sendSuccess(() -> line("Examples: ", "/swap", "  or  ", "/swap survival", ChatFormatting.GRAY), false);
                src.sendSuccess(() -> line("Config: ", "/swap config", " ", "(help)", ChatFormatting.GRAY), false);
                src.sendSuccess(() -> line("Toggle gamemode: ", "/swap config gamemode", "", "", ChatFormatting.GRAY), false);
                return 1;
            }))
            .then(Commands.literal("config")
                .executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(Component.literal("Only OPs can use /swap config.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    var src = ctx.getSource();
                    src.sendSuccess(() -> title("TQuickSwap Config"), false);
                    src.sendSuccess(() -> line("Usage: ", "/swap config help", "  or  ", "/swap config gamemode", ChatFormatting.YELLOW), false);
                    src.sendSuccess(() -> line("switchGamemodeOnSwap: ", Boolean.toString(Config.switchGamemodeOnSwap()), "", "", ChatFormatting.AQUA), false);
                    return 1;
                })
                .then(Commands.literal("help").executes(ctx -> {
                    var src = ctx.getSource();
                    src.sendSuccess(() -> title("TQuickSwap Config"), false);
                    src.sendSuccess(() -> line("- gamemode: toggle switching gamemode on swap"), false);
                    src.sendSuccess(() -> line("Example: ", "/swap config gamemode", "", "", ChatFormatting.GRAY), false);
                    return 1;
                }))
                .then(Commands.literal("gamemode").executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(Component.literal("Only OPs can use /swap config gamemode.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    boolean now = Config.toggleSwitchGamemodeOnSwap();
                    ctx.getSource().sendSuccess(() -> Component.literal("Switch gamemode on swap: " + (now ? "enabled" : "disabled")), true);
                    return 1;
                }))
            )
            .then(Commands.literal("restore")
                .then(Commands.argument("profile", StringArgumentType.word())
                    .suggests((c, b) -> { b.suggest("survival"); b.suggest("creative"); return b.buildFuture(); })
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 2))
                        .executes(ctx -> {
                            if (!ctx.getSource().hasPermission(3)) {
                                ctx.getSource().sendFailure(Component.literal("Only OPs can use /swap restore.").withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String profileRaw = StringArgumentType.getString(ctx, "profile").toUpperCase(Locale.ROOT);
                            ProfileType profile;
                            try {
                                profile = ProfileType.valueOf(profileRaw);
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFailure(Component.literal("Unknown profile: " + profileRaw));
                                return 0;
                            }
                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                            DualStore store = DualStore.of(ctx.getSource().getServer());
                            boolean restored = store.restoreBackup(p.getUUID(), profile, slot);
                            if (restored) {
                                ProfileOps.apply(p, store.load(p.getUUID(), profile));
                                ctx.getSource().sendSuccess(() -> Component.literal("Restored " + profile.displayName() + " slot " + slot), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("No backup found for " + profile.displayName() + " slot " + slot));
                            }
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests((c, b) -> { b.suggest("survival"); b.suggest("creative"); return b.buildFuture(); })
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ProfileType target = ProfileType.valueOf(StringArgumentType.getString(ctx, "mode").toUpperCase(Locale.ROOT));
                    DualStore store = DualStore.of(ctx.getSource().getServer());
                    ProfileOps.swapTo(p, target, store);
                    ctx.getSource().sendSuccess(() -> Component.literal("Switched to " + target.displayName()), true);
                    return 1;
                })
            );

        event.getDispatcher().register(root);
    }

    private void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var store = DualStore.of(player.server);
        var current = store.last(player.getUUID());
        store.save(player.getUUID(), current, ProfileOps.capture(player));
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var store = DualStore.of(player.server);
        var last = store.last(player.getUUID());
        var nbt = store.load(player.getUUID(), last);
        if (!nbt.isEmpty()) ProfileOps.apply(player, nbt);
        if (Config.switchGamemodeOnSwap()) {
            player.setGameMode(last == ProfileType.SURVIVAL ? GameType.SURVIVAL : GameType.CREATIVE);
        }
    }

    private static Component title(String name) {
        return Component.literal("== ")
            .withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal(name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" ==").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component line(String prefix, String main, String sep, String suffix, ChatFormatting color) {
        return Component.literal(prefix)
            .append(Component.literal(main).withStyle(color))
            .append(Component.literal(sep))
            .append(Component.literal(suffix));
    }

    private static String fmtInstant(java.time.Instant instant) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
        return fmt.format(instant);
    }
}
