package dev.tetralights.tquickswap;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Config {
    private static final String FILE_NAME = "tquickswap-common.toml";
    private static volatile Boolean cachedSwitchGamemodeOnSwap;

    private Config() {}

    public static boolean switchGamemodeOnSwap() {
        Boolean c = cachedSwitchGamemodeOnSwap;
        if (c != null) return c;
        boolean def = true;
        boolean val = def;
        Path dir = FabricLoader.getInstance().getConfigDir();
        Path file = dir.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                writeFile(file, def);
                val = def;
            } else {
                List<String> lines = Files.readAllLines(file);
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq <= 0) continue;
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim().toLowerCase(java.util.Locale.ROOT);
                    if ("switchGamemodeOnSwap".equals(key)) {
                        if ("true".equals(value) || "false".equals(value)) val = Boolean.parseBoolean(value);
                    }
                }
            }
        } catch (IOException ignored) {}
        cachedSwitchGamemodeOnSwap = val;
        return val;
    }

    public static void reload() {
        cachedSwitchGamemodeOnSwap = null;
    }

    public static synchronized boolean setSwitchGamemodeOnSwap(boolean value) {
        Path dir = FabricLoader.getInstance().getConfigDir();
        Path file = dir.resolve(FILE_NAME);
        try {
            writeFile(file, value);
            cachedSwitchGamemodeOnSwap = value;
        } catch (IOException ignored) {}
        return value;
    }

    public static boolean toggleSwitchGamemodeOnSwap() {
        boolean current = switchGamemodeOnSwap();
        return setSwitchGamemodeOnSwap(!current);
    }

    private static void writeFile(Path file, boolean value) throws IOException {
        Files.createDirectories(file.getParent());
        String content = "# TQuickSwap configuration\n" +
                "# When true, /swap sets the player's gamemode to match the target profile.\n" +
                "# This also affects login: when enabled, the player's gamemode is set\n" +
                "# to their active profile on join.\n" +
                ("switchGamemodeOnSwap = " + Boolean.toString(value) + "\n");
        Files.writeString(file, content);
    }
}

