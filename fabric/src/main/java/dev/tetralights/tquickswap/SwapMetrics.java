package dev.tetralights.tquickswap;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public final class SwapMetrics {
    public enum BackupFallbackReason {
        CORRUPT_PRIMARY,
        MISSING_PRIMARY
    }

    private static final LongAdder TOTAL_SWAPS = new LongAdder();
    private static final LongAdder SWAPS_TO_SURVIVAL = new LongAdder();
    private static final LongAdder SWAPS_TO_CREATIVE = new LongAdder();
    private static final LongAdder BACKUPS_USED = new LongAdder();
    private static final LongAdder BACKUPS_DUE_TO_CORRUPTION = new LongAdder();
    private static final LongAdder BACKUPS_DUE_TO_MISSING = new LongAdder();
    private static final LongAdder PRIMARY_CORRUPTION = new LongAdder();
    private static final LongAdder BACKUP_CORRUPTION = new LongAdder();
    private static final LongAdder MANUAL_RESTORES = new LongAdder();
    private static final AtomicReference<Double> LAST_SWAP_DISTANCE = new AtomicReference<>(0.0);

    private SwapMetrics() {}

    public static void recordSwap(ProfileType from, ProfileType to, double distance, boolean usedBackup) {
        TOTAL_SWAPS.increment();
        if (to == ProfileType.SURVIVAL) {
            SWAPS_TO_SURVIVAL.increment();
        } else {
            SWAPS_TO_CREATIVE.increment();
        }
        LAST_SWAP_DISTANCE.set(distance);
        if (usedBackup) {
            BACKUPS_USED.increment();
        }
    }

    public static void recordBackupFallback(ProfileType profile, DualStore.Slot slot, BackupFallbackReason reason) {
        switch (reason) {
            case CORRUPT_PRIMARY -> BACKUPS_DUE_TO_CORRUPTION.increment();
            case MISSING_PRIMARY -> BACKUPS_DUE_TO_MISSING.increment();
        }
    }

    public static void recordBackupCorrupt(ProfileType profile, DualStore.Slot slot) {
        BACKUP_CORRUPTION.increment();
    }

    public static void recordPrimaryCorrupt(ProfileType profile) {
        PRIMARY_CORRUPTION.increment();
    }

    public static void recordManualRestore(ProfileType profile, DualStore.Slot slot) {
        MANUAL_RESTORES.increment();
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            TOTAL_SWAPS.sum(),
            SWAPS_TO_SURVIVAL.sum(),
            SWAPS_TO_CREATIVE.sum(),
            BACKUPS_USED.sum(),
            BACKUPS_DUE_TO_CORRUPTION.sum(),
            BACKUPS_DUE_TO_MISSING.sum(),
            PRIMARY_CORRUPTION.sum(),
            BACKUP_CORRUPTION.sum(),
            MANUAL_RESTORES.sum(),
            LAST_SWAP_DISTANCE.get()
        );
    }

    public static ComponentData componentData() {
        Snapshot snapshot = snapshot();
        return new ComponentData(
            format(snapshot.totalSwaps()),
            format(snapshot.swapsToSurvival()),
            format(snapshot.swapsToCreative()),
            format(snapshot.backupsUsed()),
            format(snapshot.backupsDueToCorruption()),
            format(snapshot.backupsDueToMissing()),
            format(snapshot.primaryCorruptionDetected()),
            format(snapshot.backupCorruptionDetected()),
            format(snapshot.manualRestores()),
            String.format(Locale.ROOT, "%.2f", snapshot.lastSwapDistance())
        );
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public record Snapshot(long totalSwaps,
                           long swapsToSurvival,
                           long swapsToCreative,
                           long backupsUsed,
                           long backupsDueToCorruption,
                           long backupsDueToMissing,
                           long primaryCorruptionDetected,
                           long backupCorruptionDetected,
                           long manualRestores,
                           double lastSwapDistance) {}

    public record ComponentData(String totalSwaps,
                                String swapsToSurvival,
                                String swapsToCreative,
                                String backupsUsed,
                                String backupsDueToCorruption,
                                String backupsDueToMissing,
                                String primaryCorruptionDetected,
                                String backupCorruptionDetected,
                                String manualRestores,
                                String lastSwapDistance) {}
}
