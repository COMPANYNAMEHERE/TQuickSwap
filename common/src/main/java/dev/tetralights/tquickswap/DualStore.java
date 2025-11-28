package dev.tetralights.tquickswap;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * File-backed storage with automatic backups and checksums.
 */
public class DualStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tquickswap-io");
        t.setDaemon(true);
        return t;
    });
    private static final ConcurrentMap<Path, CompletableFuture<Void>> PENDING = new ConcurrentHashMap<>();
    private static final ThreadLocal<MessageDigest> DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    });
    private static final HexFormat HEX = HexFormat.of();

    private final Path baseDir;

    private DualStore(Path baseDir) {
        this.baseDir = baseDir;
    }

    public enum Slot {
        PRIMARY,
        BACKUP_1,
        BACKUP_2,
        NONE;

        public boolean isBackup() {
            return this == BACKUP_1 || this == BACKUP_2;
        }

        public int backupIndex() {
            return switch (this) {
                case BACKUP_1 -> 1;
                case BACKUP_2 -> 2;
                default -> -1;
            };
        }
    }

    public record LoadedProfile(CompoundTag data, Slot slot, boolean checksumMismatch) {
        public boolean isEmpty() { return data == null || data.isEmpty(); }
        public boolean fromBackup() { return slot.isBackup(); }
    }

    public static DualStore of(MinecraftServer server) {
        Path base = server.getWorldPath(LevelResource.ROOT).resolve("tquickswap");
        try { Files.createDirectories(base); } catch (IOException ignored) {}
        return new DualStore(base);
    }

    public void save(UUID player, ProfileType profile, CompoundTag data) {
        CompoundTag snapshot = data.copy();
        enqueue(() -> doSave(player, profile, snapshot));
    }

    public LoadedProfile load(UUID player, ProfileType profile) {
        awaitPending();

        Path primary = fileFor(player, profile);
        ReadResult main = readCompressed(primary);
        if (main.data().isPresent()) {
            return new LoadedProfile(main.data().get(), Slot.PRIMARY, false);
        }

        boolean primaryCorrupt = main.checksumMismatch();
        boolean corrupted = primaryCorrupt;
        Slot fallbackSlot = Slot.NONE;

        for (Slot slot : EnumSet.of(Slot.BACKUP_1, Slot.BACKUP_2)) {
            Path backup = backupFile(player, profile, slot.backupIndex());
            ReadResult backupData = readCompressed(backup);
            if (backupData.data().isPresent()) {
                fallbackSlot = slot;
                SwapMetrics.recordBackupFallback(profile, slot,
                    primaryCorrupt ? SwapMetrics.BackupFallbackReason.CORRUPT_PRIMARY : SwapMetrics.BackupFallbackReason.MISSING_PRIMARY);
                return new LoadedProfile(backupData.data().get(), slot, backupData.checksumMismatch());
            }
            if (backupData.checksumMismatch()) {
                corrupted = true;
                SwapMetrics.recordBackupCorrupt(profile, slot);
            }
        }

        if (primaryCorrupt) {
            SwapMetrics.recordPrimaryCorrupt(profile);
        }

        return new LoadedProfile(new CompoundTag(), fallbackSlot, corrupted);
    }

    public ProfileType last(UUID player) {
        awaitPending();
        Path f = lastFile(player);
        if (Files.exists(f)) {
            try (var in = Files.newInputStream(f)) {
                CompoundTag n = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                String name = n.getString("profile");
                if (!name.isEmpty()) return ProfileType.valueOf(name);
            } catch (Exception ignored) {}
        }
        return ProfileType.SURVIVAL;
    }

    public Optional<Instant> lastModified(UUID player, ProfileType profile) {
        awaitPending();
        try {
            Path f = fileFor(player, profile);
            if (!Files.exists(f)) return Optional.empty();
            FileTime ft = Files.getLastModifiedTime(f);
            return Optional.ofNullable(ft).map(FileTime::toInstant);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<Instant> lastBackupModified(UUID player, ProfileType profile, int slot) {
        if (slot < 1 || slot > 2) return Optional.empty();
        awaitPending();
        try {
            Path f = backupFile(player, profile, slot);
            if (!Files.exists(f)) return Optional.empty();
            FileTime ft = Files.getLastModifiedTime(f);
            return Optional.ofNullable(ft).map(FileTime::toInstant);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<CompoundTag> restoreBackup(UUID player, ProfileType profile, int slot) {
        if (slot < 1 || slot > 2) return Optional.empty();
        awaitPending();
        Path source = backupFile(player, profile, slot);
        ReadResult data = readCompressed(source);
        Slot slotEnum = slot == 1 ? Slot.BACKUP_1 : Slot.BACKUP_2;
        if (data.checksumMismatch()) {
            SwapMetrics.recordBackupCorrupt(profile, slotEnum);
            return Optional.empty();
        }
        if (data.data().isEmpty()) return Optional.empty();
        SwapMetrics.recordManualRestore(profile, slotEnum);
        save(player, profile, data.data().get().copy());
        return data.data();
    }

    private Path fileFor(UUID player, ProfileType profile) {
        return baseDir.resolve(player.toString() + "-" + profile.name().toLowerCase() + ".nbt");
    }

    private Path backupFile(UUID player, ProfileType profile, int slot) {
        return baseDir.resolve(player.toString() + "-" + profile.name().toLowerCase() + ".backup" + slot + ".nbt");
    }

    private Path lastFile(UUID player) {
        return baseDir.resolve(player.toString() + "-last.nbt");
    }

    private void enqueue(Runnable action) {
        PENDING.compute(baseDir, (path, future) -> {
            CompletableFuture<Void> tail = future == null ? CompletableFuture.completedFuture(null) : future;
            return tail.thenRunAsync(() -> {
                try {
                    action.run();
                } catch (Exception e) {
                    LOGGER.error("TQuickSwap IO task failed for {}", baseDir, e);
                    throw new RuntimeException(e);
                }
            }, IO_EXECUTOR);
        });
    }

    private void awaitPending() {
        CompletableFuture<Void> pending = PENDING.get(baseDir);
        if (pending != null) {
            try {
                pending.join();
            } catch (Exception e) {
                LOGGER.error("Error waiting for pending IO operations", e);
            }
        }
    }

    private void doSave(UUID player, ProfileType profile, CompoundTag data) {
        try {
            Files.createDirectories(baseDir);
            Path primary = fileFor(player, profile);
            Path backup1 = backupFile(player, profile, 1);
            Path backup2 = backupFile(player, profile, 2);
            rotateBackups(primary, backup1, backup2);
            writeAtomically(data, primary);
            writeLast(player, profile);
        } catch (IOException e) {
            LOGGER.warn("Failed to save profile {} for player {}", profile, player, e);
        }
    }

    private void writeLast(UUID player, ProfileType profile) throws IOException {
        Path lastPath = lastFile(player);
        CompoundTag last = new CompoundTag();
        last.putString("profile", profile.name());
        NbtIo.writeCompressed(last, lastPath);
    }

    private ReadResult readCompressed(Path file) {
        if (!Files.exists(file)) return new ReadResult(Optional.empty(), false);
        try {
            byte[] bytes = Files.readAllBytes(file);
            boolean checksumOk = verifyChecksum(file, bytes);
            if (!checksumOk) {
                return new ReadResult(Optional.empty(), true);
            }
            try (var in = new ByteArrayInputStream(bytes)) {
                CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                return new ReadResult(Optional.ofNullable(tag), false);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read profile data {}", file, e);
            return new ReadResult(Optional.empty(), false);
        }
    }

    private void rotateBackups(Path primary, Path backup1, Path backup2) throws IOException {
        moveWithChecksum(backup1, backup2);
        copyWithChecksum(primary, backup1);
    }

    private void moveWithChecksum(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            moveChecksum(source, target);
        } else {
            Files.deleteIfExists(target);
            Files.deleteIfExists(checksumPath(target));
        }
    }

    private void copyWithChecksum(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            copyChecksum(source, target);
        } else {
            Files.deleteIfExists(target);
            Files.deleteIfExists(checksumPath(target));
        }
    }

    private void moveChecksum(Path source, Path target) throws IOException {
        Path sourceChecksum = checksumPath(source);
        Path targetChecksum = checksumPath(target);
        if (Files.exists(sourceChecksum)) {
            Files.move(sourceChecksum, targetChecksum, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(targetChecksum);
        }
    }

    private void copyChecksum(Path source, Path target) throws IOException {
        Path sourceChecksum = checksumPath(source);
        Path targetChecksum = checksumPath(target);
        if (Files.exists(sourceChecksum)) {
            Files.copy(sourceChecksum, targetChecksum, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(targetChecksum);
        }
    }

    private void writeAtomically(CompoundTag data, Path target) throws IOException {
        byte[] bytes = compress(data);
        Path temp = Files.createTempFile(baseDir, "swap-", ".nbt");
        Files.write(temp, bytes);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        writeChecksum(target, bytes);
    }

    private byte[] compress(CompoundTag data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(data, out);
        return out.toByteArray();
    }

    private boolean verifyChecksum(Path file, byte[] bytes) throws IOException {
        String actual = checksum(bytes);
        Path checksumFile = checksumPath(file);
        if (!Files.exists(checksumFile)) {
            Files.writeString(checksumFile, actual, StandardCharsets.UTF_8);
            return true;
        }
        String expected = Files.readString(checksumFile, StandardCharsets.UTF_8).trim();
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
        LOGGER.warn("Checksum mismatch for {} (expected {}, got {})", file.getFileName(), expected, actual);
        return false;
    }

    private void writeChecksum(Path file, byte[] bytes) throws IOException {
        String checksum = checksum(bytes);
        Files.writeString(checksumPath(file), checksum, StandardCharsets.UTF_8);
    }

    private String checksum(byte[] bytes) {
        MessageDigest md = DIGEST.get();
        md.reset();
        md.update(bytes);
        return HEX.formatHex(md.digest());
    }

    private Path checksumPath(Path file) {
        return file.resolveSibling(file.getFileName().toString() + ".sha1");
    }

    public static void flushAll() {
        PENDING.values().forEach(CompletableFuture::join);
    }

    private record ReadResult(Optional<CompoundTag> data, boolean checksumMismatch) {}
}
