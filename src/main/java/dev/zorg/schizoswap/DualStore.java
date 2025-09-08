package dev.zorg.schizoswap;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Path base = server.getSavePath(WorldSavePath.ROOT).resolve("schizoswap");
        try { Files.createDirectories(base); } catch (IOException ignored) {}
        return new DualStore(base);
    }

    public void save(UUID player, ProfileType profile, NbtCompound data) {
        // also record last profile switched-to
        try {
            Files.createDirectories(baseDir);
            NbtIo.writeCompressed(data, fileFor(player, profile));
            NbtCompound last = new NbtCompound();
            last.putString("profile", profile.name());
            NbtIo.writeCompressed(last, lastFile(player));
        } catch (IOException e) {
            // swallow to avoid crashing server; could log via LOGGER if desired
        }
    }

    public NbtCompound load(UUID player, ProfileType profile) {
        Path f = fileFor(player, profile);
        if (Files.exists(f)) {
            try { return NbtIo.readCompressed(f.toFile()); } catch (IOException ignored) {}
        }
        return new NbtCompound();
    }

    public ProfileType last(UUID player) {
        Path f = lastFile(player);
        if (Files.exists(f)) {
            try {
                NbtCompound n = NbtIo.readCompressed(f.toFile());
                String name = n.getString("profile");
                if (!name.isEmpty()) return ProfileType.valueOf(name);
            } catch (Exception ignored) {}
        }
        return ProfileType.SURVIVAL;
    }

    private Path fileFor(UUID player, ProfileType profile) {
        return baseDir.resolve(player.toString() + "-" + profile.name().toLowerCase() + ".nbt");
    }

    private Path lastFile(UUID player) {
        return baseDir.resolve(player.toString() + "-last.nbt");
    }
}
