package dev.tetralights.tquickswap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal file-backed storage under the world save folder.
 */
public class DualStore {
    private final Path baseDir;

    private DualStore(Path baseDir) {
        this.baseDir = baseDir;
    }

    public static DualStore of(MinecraftServer server) {
        Path base = server.getWorldPath(LevelResource.ROOT).resolve("tquickswap");
        try { Files.createDirectories(base); } catch (IOException ignored) {}
        return new DualStore(base);
    }

    public void save(UUID player, ProfileType profile, CompoundTag data) {
        try {
            Files.createDirectories(baseDir);
            NbtIo.writeCompressed(data, fileFor(player, profile));
            CompoundTag last = new CompoundTag();
            last.putString("profile", profile.name());
            NbtIo.writeCompressed(last, lastFile(player));
        } catch (IOException e) { /* suppress */ }
    }

    public CompoundTag load(UUID player, ProfileType profile) {
        Path f = fileFor(player, profile);
        if (Files.exists(f)) {
            try (var in = java.nio.file.Files.newInputStream(f)) {
                return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            } catch (IOException e) { /* suppress */ }
        }
        return new CompoundTag();
    }

    public ProfileType last(UUID player) {
        Path f = lastFile(player);
        if (Files.exists(f)) {
            try (var in = java.nio.file.Files.newInputStream(f)) {
                CompoundTag n = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                String name = n.getString("profile");
                if (!name.isEmpty()) return ProfileType.valueOf(name);
            } catch (Exception ignored) {}
        }
        return ProfileType.SURVIVAL;
    }

    public Optional<Instant> lastModified(UUID player, ProfileType profile) {
        try {
            Path f = fileFor(player, profile);
            if (!Files.exists(f)) return Optional.empty();
            FileTime ft = Files.getLastModifiedTime(f);
            return Optional.ofNullable(ft).map(FileTime::toInstant);
        } catch (IOException e) { return Optional.empty(); }
    }

    private Path fileFor(UUID player, ProfileType profile) {
        return baseDir.resolve(player.toString() + "-" + profile.name().toLowerCase() + ".nbt");
    }

    private Path lastFile(UUID player) {
        return baseDir.resolve(player.toString() + "-last.nbt");
    }
}

