package dev.tetralights.tquickswap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

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
                LangHelper.ensureLanguageAvailable(p.getServer(), p.getLanguage());
                DualStore store = DualStore.of(ctx.getSource().getServer());
                ProfileType current = store.last(p.getUUID());
                ProfileType target = (current == ProfileType.CREATIVE) ? ProfileType.SURVIVAL : ProfileType.CREATIVE;
                ProfileOps.swapTo(p, target, store);
                ctx.getSource().sendSuccess(() -> LangHelper.tr("message.tquickswap.switched", target.displayName()).withStyle(ChatFormatting.AQUA), true);
                return 1;
            })
            .then(Commands.literal("status").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                LangHelper.ensureLanguageAvailable(p.getServer(), p.getLanguage());
                var server = ctx.getSource().getServer();
                DualStore store = DualStore.of(server);
                ProfileType current = store.last(p.getUUID());

                Component survPrimary = timestampComponent(store.lastModified(p.getUUID(), ProfileType.SURVIVAL), ChatFormatting.GRAY);
                Component survB1 = timestampComponent(store.lastBackupModified(p.getUUID(), ProfileType.SURVIVAL, 1), ChatFormatting.DARK_AQUA);
                Component survB2 = timestampComponent(store.lastBackupModified(p.getUUID(), ProfileType.SURVIVAL, 2), ChatFormatting.DARK_AQUA);
                Component creatPrimary = timestampComponent(store.lastModified(p.getUUID(), ProfileType.CREATIVE), ChatFormatting.GRAY);
                Component creatB1 = timestampComponent(store.lastBackupModified(p.getUUID(), ProfileType.CREATIVE, 1), ChatFormatting.DARK_AQUA);
                Component creatB2 = timestampComponent(store.lastBackupModified(p.getUUID(), ProfileType.CREATIVE, 2), ChatFormatting.DARK_AQUA);

                ctx.getSource().sendSuccess(() -> titleKey("command.tquickswap.status.title"), false);
                ctx.getSource().sendSuccess(() -> infoLine("command.tquickswap.status.current", colored(current.displayName(), ChatFormatting.AQUA)), false);
                ctx.getSource().sendSuccess(() -> infoLine("command.tquickswap.status.saved_survival", survPrimary), false);
                ctx.getSource().sendSuccess(() -> infoLine("command.tquickswap.status.backups_survival", survB1, survB2), false);
                ctx.getSource().sendSuccess(() -> infoLine("command.tquickswap.status.saved_creative", creatPrimary), false);
                ctx.getSource().sendSuccess(() -> infoLine("command.tquickswap.status.backups_creative", creatB1, creatB2), false);
                return 1;
            }))
            .then(Commands.literal("help").executes(ctx -> {
                var src = ctx.getSource();
                src.sendSuccess(() -> titleKey("command.tquickswap.help.title"), false);
                src.sendSuccess(() -> commandLink(LangHelper.tr("command.tquickswap.help.swap"), "/swap", ChatFormatting.GOLD), false);
                src.sendSuccess(() -> commandLink(LangHelper.tr("command.tquickswap.help.status"), "/swap status", ChatFormatting.GOLD), false);
                src.sendSuccess(() -> commandLink(LangHelper.tr("command.tquickswap.help.restore"), "/swap restore survival 1", ChatFormatting.GOLD), false);
                src.sendSuccess(() -> commandLink(LangHelper.tr("command.tquickswap.help.menu"), "/swap menu", ChatFormatting.GOLD), false);
                src.sendSuccess(() -> commandLink(LangHelper.tr("command.tquickswap.help.config"), "/swap config", ChatFormatting.GOLD), false);
                src.sendSuccess(() -> infoLine("command.tquickswap.help.backups"), false);
                return 1;
            }))
            .then(Commands.literal("menu").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                LangHelper.ensureLanguageAvailable(p.getServer(), p.getLanguage());
                var server = ctx.getSource().getServer();
                DualStore store = DualStore.of(server);
                ProfileType current = store.last(p.getUUID());
                ProfileType target = current == ProfileType.CREATIVE ? ProfileType.SURVIVAL : ProfileType.CREATIVE;
                boolean canConfigure = ctx.getSource().hasPermission(3);

                ctx.getSource().sendSuccess(() -> titleKey("command.tquickswap.menu.title"), false);
                ctx.getSource().sendSuccess(() -> infoLine("command.tquickswap.menu.current", colored(current.displayName(), ChatFormatting.AQUA)), false);
                ctx.getSource().sendSuccess(() -> commandLink(LangHelper.tr("command.tquickswap.menu.switch", target.displayName()), "/swap " + target.commandKey(), ChatFormatting.GOLD), false);

                ctx.getSource().sendSuccess(() -> toggleLine("command.tquickswap.menu.toggle.gamemode", Config.switchGamemodeOnSwap(), "/swap config gamemode", canConfigure), false);
                ctx.getSource().sendSuccess(() -> toggleLine("command.tquickswap.menu.toggle.alerts", Config.notifyOnSwap(), "/swap config alerts", canConfigure), false);
                ctx.getSource().sendSuccess(() -> toggleLine("command.tquickswap.menu.toggle.backupalerts", Config.notifyOnBackupRestore(), "/swap config backupalerts", canConfigure), false);

                ctx.getSource().sendSuccess(() -> titleKey("command.tquickswap.menu.snapshots"), false);
                ctx.getSource().sendSuccess(() -> backupLine(p, store, ProfileType.SURVIVAL, canConfigure), false);
                ctx.getSource().sendSuccess(() -> backupLine(p, store, ProfileType.CREATIVE, canConfigure), false);
                return 1;
            }))
            .then(Commands.literal("stats")
                .executes(ctx -> {
                    SwapMetrics.ComponentData data = SwapMetrics.componentData();
                    var src = ctx.getSource();
                    boolean isOp = src.hasPermission(3);
                    src.sendSuccess(() -> titleKey("command.tquickswap.stats.title"), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.stats.total", literal(data.totalSwaps())), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.stats.survival", literal(data.swapsToSurvival())), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.stats.creative", literal(data.swapsToCreative())), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.stats.distance", literal(data.lastSwapDistance())), false);

                    if (isOp) {
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.backups", literal(data.backupsUsed())), false);
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.backups.corrupt", literal(data.backupsDueToCorruption())), false);
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.backups.missing", literal(data.backupsDueToMissing())), false);
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.corrupt.primary", literal(data.primaryCorruptionDetected())), false);
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.corrupt.backup", literal(data.backupCorruptionDetected())), false);
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.manual", literal(data.manualRestores())), false);
                    } else {
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.backups", literal(data.backupsUsed())), false);
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.manual", literal(data.manualRestores())), false);
                        src.sendSuccess(() -> infoLine("command.tquickswap.stats.notice.ops"), false);
                    }
                    return 1;
                }))
            .then(Commands.literal("config")
                .executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.only_ops", "/swap config").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    var src = ctx.getSource();
                    src.sendSuccess(() -> titleKey("command.tquickswap.config.title"), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.config.usage"), false);
                    src.sendSuccess(() -> configEntry("command.tquickswap.menu.toggle.gamemode", Config.switchGamemodeOnSwap()), false);
                    src.sendSuccess(() -> configEntry("command.tquickswap.menu.toggle.alerts", Config.notifyOnSwap()), false);
                    src.sendSuccess(() -> configEntry("command.tquickswap.menu.toggle.backupalerts", Config.notifyOnBackupRestore()), false);
                    return 1;
                })
                .then(Commands.literal("help").executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.only_ops", "/swap config help").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    var src = ctx.getSource();
                    src.sendSuccess(() -> titleKey("command.tquickswap.config.title"), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.config.help.gamemode"), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.config.help.alerts"), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.config.help.backup"), false);
                    src.sendSuccess(() -> infoLine("command.tquickswap.config.help.reload"), false);
                    return 1;
                }))
                .then(Commands.literal("gamemode").executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.only_ops", "/swap config gamemode").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    boolean now = Config.toggleSwitchGamemodeOnSwap();
                    ctx.getSource().sendSuccess(() -> LangHelper.tr("command.tquickswap.config.gamemode", boolWord(now).copy().withStyle(ChatFormatting.AQUA)), true);
                    return 1;
                }))
                .then(Commands.literal("alerts").executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.only_ops", "/swap config alerts").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    boolean now = Config.toggleNotifyOnSwap();
                    ctx.getSource().sendSuccess(() -> LangHelper.tr("command.tquickswap.config.alerts", boolWord(now).copy().withStyle(ChatFormatting.AQUA)), true);
                    return 1;
                }))
                .then(Commands.literal("backupalerts").executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.only_ops", "/swap config backupalerts").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    boolean now = Config.toggleNotifyOnBackupRestore();
                    ctx.getSource().sendSuccess(() -> LangHelper.tr("command.tquickswap.config.backupalerts", boolWord(now).copy().withStyle(ChatFormatting.AQUA)), true);
                    return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    if (!ctx.getSource().hasPermission(3)) {
                        ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.only_ops", "/swap config reload").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    Config.reload();
                    ctx.getSource().sendSuccess(() -> LangHelper.tr("command.tquickswap.config.reloaded").withStyle(ChatFormatting.AQUA), true);
                    return 1;
                }))
            )
            .then(Commands.literal("restore")
                .then(Commands.argument("profile", StringArgumentType.word())
                    .suggests((c, b) -> { b.suggest("survival"); b.suggest("creative"); return b.buildFuture(); })
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 2))
                        .executes(ctx -> {
                            if (!ctx.getSource().hasPermission(3)) {
                                ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.only_ops", "/swap restore").withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            LangHelper.ensureLanguageAvailable(p.getServer(), p.getLanguage());
                            String profileRaw = StringArgumentType.getString(ctx, "profile").toUpperCase(Locale.ROOT);
                            ProfileType profile;
                            try {
                                profile = ProfileType.valueOf(profileRaw);
                            } catch (IllegalArgumentException ex) {
                                ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.profile.unknown", profileRaw.toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                            DualStore store = DualStore.of(ctx.getSource().getServer());
                            ProfileType activeBefore = store.last(p.getUUID());
                            var restored = store.restoreBackup(p.getUUID(), profile, slot);
                            if (restored.isEmpty()) {
                                ctx.getSource().sendFailure(LangHelper.tr("command.tquickswap.backup.none", slot, profile.displayName()).withStyle(ChatFormatting.RED));
                                return 0;
                            }

                            if (activeBefore == profile) {
                                ProfileOps.apply(p, restored.get());
                                ctx.getSource().sendSuccess(() -> LangHelper.tr("command.tquickswap.backup.applied", slot, profile.displayName()).withStyle(ChatFormatting.AQUA), true);
                                p.sendSystemMessage(LangHelper.tr("message.tquickswap.backup_manual", slot, profile.displayName()).withStyle(ChatFormatting.GOLD));
                            } else {
                                ctx.getSource().sendSuccess(() -> LangHelper.tr("command.tquickswap.backup.restored", slot, profile.displayName(), profile.commandKey()).withStyle(ChatFormatting.GRAY), false);
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
                    LangHelper.ensureLanguageAvailable(p.getServer(), p.getLanguage());
                    ProfileType target = ProfileType.valueOf(StringArgumentType.getString(ctx, "mode").toUpperCase(Locale.ROOT));
                    DualStore store = DualStore.of(ctx.getSource().getServer());
                    ProfileOps.swapTo(p, target, store);
                    ctx.getSource().sendSuccess(() -> LangHelper.tr("message.tquickswap.switched", target.displayName()).withStyle(ChatFormatting.AQUA), true);
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
        var snapshot = store.load(p.getUUID(), last);
        LangHelper.ensureLanguageAvailable(server, p.getLanguage());
        if (!snapshot.isEmpty()) ProfileOps.apply(p, snapshot.data());
        if (snapshot.slot().isBackup() && !snapshot.isEmpty() && Config.notifyOnBackupRestore()) {
            p.sendSystemMessage(LangHelper.tr("message.tquickswap.backup_auto", last.displayName(), snapshot.slot().backupIndex()).withStyle(ChatFormatting.GOLD));
        }
        // Respect config on login: align gamemode only when enabled
        if (Config.switchGamemodeOnSwap()) {
            p.setGameMode(last == ProfileType.SURVIVAL ? net.minecraft.world.level.GameType.SURVIVAL : net.minecraft.world.level.GameType.CREATIVE);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        DualStore.flushAll();
    }

    private static Component titleKey(String key) {
        return Component.literal("== ").withStyle(ChatFormatting.DARK_GRAY)
            .append(LangHelper.tr(key).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" ==").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent infoLine(String key, Object... args) {
        return LangHelper.tr(key, args).withStyle(ChatFormatting.GRAY);
    }

    private static Component link(String prefix, String url, Component suffix) {
        return Component.literal(prefix).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(url)
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))))
            .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
            .append(suffix.copy().withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component commandLink(Component label, String command, ChatFormatting color) {
        MutableComponent base = label.copy()
            .withStyle(style -> style
                .withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    LangHelper.tr("command.tquickswap.menu.button_hint", command))));
        return base.append(Component.literal(" "))
            .append(LangHelper.tr("menu.tquickswap.button.click").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component toggleLine(String labelKey, boolean value, String command, boolean canToggle) {
        MutableComponent state = stateLabel(value)
            .withStyle(style -> style
                .withColor(value ? ChatFormatting.GREEN : ChatFormatting.RED)
                .withBold(true));
        if (canToggle) {
            state = state.withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    LangHelper.tr("command.tquickswap.menu.button_hint", command))));
        }
        return LangHelper.tr("command.tquickswap.config.entry",
            LangHelper.tr(labelKey), state).withStyle(ChatFormatting.GRAY);
    }

    private static Component configEntry(String labelKey, boolean value) {
        return LangHelper.tr("command.tquickswap.config.entry",
            LangHelper.tr(labelKey), boolWord(value).copy().withStyle(ChatFormatting.AQUA))
            .withStyle(ChatFormatting.GRAY);
    }

    private static Component backupLine(ServerPlayer player, DualStore store, ProfileType profile, boolean canRestore) {
        Component primary = timestampComponent(store.lastModified(player.getUUID(), profile), ChatFormatting.AQUA);
        Component b1 = timestampComponent(store.lastBackupModified(player.getUUID(), profile, 1), ChatFormatting.DARK_AQUA);
        Component b2 = timestampComponent(store.lastBackupModified(player.getUUID(), profile, 2), ChatFormatting.DARK_AQUA);

        MutableComponent base = infoLine("command.tquickswap.menu.snapshot.line",
            colored(profile.displayName(), ChatFormatting.GRAY), primary, b1, b2).copy();

        if (canRestore) {
            base = base
                .append(Component.literal("  "))
                .append(restoreButton(profile, 1))
                .append(Component.literal(" "))
                .append(restoreButton(profile, 2));
        }
        return base;
    }

    private static Component restoreButton(ProfileType profile, int slot) {
        String cmd = "/swap restore " + profile.commandKey() + " " + slot;
        return LangHelper.tr("command.tquickswap.menu.snapshot.button", slot)
            .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    LangHelper.tr("command.tquickswap.menu.button_hint", cmd))));
    }

    private static Component boolWord(boolean value) {
        return LangHelper.tr(value ? "message.tquickswap.enabled" : "message.tquickswap.disabled");
    }

    private static MutableComponent stateLabel(boolean value) {
        return LangHelper.tr(value ? "message.tquickswap.state.on" : "message.tquickswap.state.off");
    }

    private static Component colored(Component component, ChatFormatting color) {
        return component.copy().withStyle(color);
    }

    private static Component literal(String value) {
        return Component.literal(value).withStyle(ChatFormatting.AQUA);
    }

    private static Component timestampComponent(Optional<Instant> instant, ChatFormatting color) {
        MutableComponent base = instant.map(i -> Component.literal(fmtInstant(i))).orElseGet(() -> LangHelper.tr("message.tquickswap.never"));
        return base.withStyle(color);
    }

    private static String fmtInstant(java.time.Instant i) {
        return java.time.ZonedDateTime.ofInstant(i, java.time.ZoneId.systemDefault())
            .toLocalDateTime().toString();
    }
}
