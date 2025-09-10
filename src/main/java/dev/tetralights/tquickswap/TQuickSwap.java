package dev.tetralights.tquickswap;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(TQuickSwap.MODID)
public class TQuickSwap {
    public static final String MODID = "tquickswap";

    public TQuickSwap() {
        // Register for gameplay events (commands, login/logout)
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("swap")
            .executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                DualStore store = DualStore.of(ctx.getSource().getServer());
                ProfileType current = store.last(p.getUUID());
                ProfileType target = (current == ProfileType.CREATIVE) ? ProfileType.SURVIVAL : ProfileType.CREATIVE;
                ProfileOps.swapTo(p, target, store);
                ctx.getSource().sendSuccess(() -> Component.literal("Switched to " + target), true);
                return 1;
            })
            .then(Commands.literal("status").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                var server = ctx.getSource().getServer();
                DualStore store = DualStore.of(server);
                ProfileType current = store.last(p.getUUID());
                var surv = store.lastModified(p.getUUID(), ProfileType.SURVIVAL).map(TQuickSwap::fmtInstant).orElse("never");
                var creat = store.lastModified(p.getUUID(), ProfileType.CREATIVE).map(TQuickSwap::fmtInstant).orElse("never");
                ctx.getSource().sendSuccess(() -> title("TQuickSwap Status"), false);
                ctx.getSource().sendSuccess(() -> line("Current: ", current.name(), "", "", ChatFormatting.AQUA), false);
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
                src.sendSuccess(() -> link("Source ", "https://github.com/COMPANYNAMEHERE/TQuickSwap", "(click)"), false);
                src.sendSuccess(() -> line("Config: ", "/swap config", " ", "(help)", ChatFormatting.GRAY), false);
                src.sendSuccess(() -> line("Toggle gamemode: ", "/swap config gamemode", "", "", ChatFormatting.GRAY), false);
                return 1;
            }))
            .then(Commands.literal("config").requires(s -> s.hasPermission(3))
                .executes(ctx -> {
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
                    boolean now = Config.toggleSwitchGamemodeOnSwap();
                    ctx.getSource().sendSuccess(() -> Component.literal("Switch gamemode on swap: " + (now ? "enabled" : "disabled")), true);
                    return 1;
                }))
            )
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests((c, b) -> { b.suggest("survival"); b.suggest("creative"); return b.buildFuture(); })
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ProfileType target = ProfileType.valueOf(StringArgumentType.getString(ctx, "mode").toUpperCase());
                    DualStore store = DualStore.of(ctx.getSource().getServer());
                    ProfileOps.swapTo(p, target, store);
                    ctx.getSource().sendSuccess(() -> Component.literal("Switched to " + target), true);
                    return 1;
                })
            );

        event.getDispatcher().register(root);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var server = p.getServer();
        var store = DualStore.of(server);
        var current = store.last(p.getUUID());
        store.save(p.getUUID(), current, ProfileOps.capture(p));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var server = p.getServer();
        var store = DualStore.of(server);
        var last = store.last(p.getUUID());
        var nbt = store.load(p.getUUID(), last);
        if (!nbt.isEmpty()) ProfileOps.apply(p, nbt);
        // Respect config on login: align gamemode only when enabled
        if (Config.switchGamemodeOnSwap()) {
            p.setGameMode(last == ProfileType.SURVIVAL ? net.minecraft.world.level.GameType.SURVIVAL : net.minecraft.world.level.GameType.CREATIVE);
        }
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

    private static String fmtInstant(java.time.Instant i) {
        return java.time.ZonedDateTime.ofInstant(i, java.time.ZoneId.systemDefault())
            .toLocalDateTime().toString();
    }
}
