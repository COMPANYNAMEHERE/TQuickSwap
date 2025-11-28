package dev.tetralights.tquickswap;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class CommonHooks {
    private CommonHooks() {}

    public static void onPlayerJoin(ServerPlayer player) {
        var server = player.server;
        var store = DualStore.of(server);
        var last = store.last(player.getUUID());
        DualStore.LoadedProfile snapshot = store.load(player.getUUID(), last);
        LangHelper.ensureLanguageAvailable(server, null);
        if (!snapshot.isEmpty()) {
            ProfileOps.apply(player, snapshot.data());
            if (snapshot.slot().isBackup() && Config.notifyOnBackupRestore()) {
                player.sendSystemMessage(LangHelper.tr("message.tquickswap.backup_auto", last.displayName(), snapshot.slot().backupIndex()).withStyle(ChatFormatting.GOLD));
            }
        }
        if (Config.switchGamemodeOnSwap()) {
            player.setGameMode(last == ProfileType.SURVIVAL ? GameType.SURVIVAL : GameType.CREATIVE);
        }
    }

    public static void onPlayerQuit(ServerPlayer player) {
        var store = DualStore.of(player.server);
        var current = store.last(player.getUUID());
        store.save(player.getUUID(), current, ProfileOps.capture(player));
    }

    public static void onServerStopping() {
        DualStore.flushAll();
    }
}
