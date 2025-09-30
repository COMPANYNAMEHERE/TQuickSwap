package dev.tetralights.tquickswap;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class Config {
    private static final String FILE_NAME = "tquickswap-common.toml";

    private record ConfigValues(boolean switchGamemodeOnSwap,
                                boolean notifyOnSwap,
                                boolean notifyOnBackupRestore) {}

    private static final ConfigValues DEFAULTS = new ConfigValues(true, true, true);
    private static volatile ConfigValues cachedValues;

    private Config() {}

    public static boolean switchGamemodeOnSwap() {
        return values().switchGamemodeOnSwap();
    }

    public static boolean notifyOnSwap() {
        return values().notifyOnSwap();
    }

    public static boolean notifyOnBackupRestore() {
        return values().notifyOnBackupRestore();
    }

    public static void reload() {
        cachedValues = null;
    }

    public static synchronized boolean setSwitchGamemodeOnSwap(boolean value) {
        ConfigValues current = values();
        ConfigValues updated = new ConfigValues(value, current.notifyOnSwap(), current.notifyOnBackupRestore());
        writeAndCache(updated);
        return value;
    }

    public static synchronized boolean setNotifyOnSwap(boolean value) {
        ConfigValues current = values();
        ConfigValues updated = new ConfigValues(current.switchGamemodeOnSwap(), value, current.notifyOnBackupRestore());
        writeAndCache(updated);
        return value;
    }

    public static synchronized boolean setNotifyOnBackupRestore(boolean value) {
        ConfigValues current = values();
        ConfigValues updated = new ConfigValues(current.switchGamemodeOnSwap(), current.notifyOnSwap(), value);
        writeAndCache(updated);
        return value;
    }

    public static boolean toggleSwitchGamemodeOnSwap() {
        return setSwitchGamemodeOnSwap(!switchGamemodeOnSwap());
    }

    public static boolean toggleNotifyOnSwap() {
        return setNotifyOnSwap(!notifyOnSwap());
    }

    public static boolean toggleNotifyOnBackupRestore() {
        return setNotifyOnBackupRestore(!notifyOnBackupRestore());
    }

    private static ConfigValues values() {
        ConfigValues cached = cachedValues;
        if (cached != null) return cached;
        synchronized (Config.class) {
            cached = cachedValues;
            if (cached != null) return cached;
            ConfigValues loaded = readOrCreate();
            cachedValues = loaded;
            return loaded;
        }
    }

    private static ConfigValues readOrCreate() {
        Path dir = FMLPaths.CONFIGDIR.get();
        Path file = dir.resolve(FILE_NAME);
        ConfigValues defaults = DEFAULTS;
        boolean switchGm = defaults.switchGamemodeOnSwap();
        boolean notifySwap = defaults.notifyOnSwap();
        boolean notifyBackup = defaults.notifyOnBackupRestore();

        try {
            if (!Files.exists(file)) {
                writeFile(file, defaults);
                return defaults;
            }

            List<String> lines = Files.readAllLines(file);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim().toLowerCase(Locale.ROOT);
                switch (key) {
                    case "switchGamemodeOnSwap" -> switchGm = parseBoolean(value, switchGm);
                    case "notifyOnSwap" -> notifySwap = parseBoolean(value, notifySwap);
                    case "notifyOnBackupRestore" -> notifyBackup = parseBoolean(value, notifyBackup);
                }
            }
        } catch (IOException ignored) {}

        ConfigValues loaded = new ConfigValues(switchGm, notifySwap, notifyBackup);
        writeAndCacheSilently(file, loaded);
        return loaded;
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        return fallback;
    }

    private static void writeAndCache(ConfigValues values) {
        Path dir = FMLPaths.CONFIGDIR.get();
        Path file = dir.resolve(FILE_NAME);
        try {
            writeFile(file, values);
            cachedValues = values;
        } catch (IOException ignored) {}
    }

    private static void writeAndCacheSilently(Path file, ConfigValues values) {
        try {
            writeFile(file, values);
        } catch (IOException ignored) {}
    }

    private static void writeFile(Path file, ConfigValues values) throws IOException {
        Files.createDirectories(file.getParent());
        String content = "# TQuickSwap configuration\n" +
            "# switchGamemodeOnSwap: when true, /swap aligns player gamemode with the profile.\n" +
            "# notifyOnSwap: when true, players get a chat summary after swapping.\n" +
            "# notifyOnBackupRestore: when true, players are notified if a backup snapshot is loaded.\n" +
            "switchGamemodeOnSwap = " + values.switchGamemodeOnSwap() + "\n" +
            "notifyOnSwap = " + values.notifyOnSwap() + "\n" +
            "notifyOnBackupRestore = " + values.notifyOnBackupRestore() + "\n";
        Files.writeString(file, content);
    }
}
