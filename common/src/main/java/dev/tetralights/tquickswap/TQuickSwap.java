package dev.tetralights.tquickswap;

import java.nio.file.Path;

public final class TQuickSwap {
    public static final String MODID = "tquickswap";

    private static volatile Path configDir;

    private TQuickSwap() {}

    public static void setConfigDir(Path dir) {
        configDir = dir;
    }

    public static Path configDir() {
        Path dir = configDir;
        if (dir != null) return dir;
        return Path.of("config");
    }
}
