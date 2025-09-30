package dev.tetralights.tquickswap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LangHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, String> ENGLISH = loadEnglish();
    private static final Set<String> WARNED_LANGS = ConcurrentHashMap.newKeySet();

    private LangHelper() {}

    public static MutableComponent tr(String key, Object... args) {
        String fallback = ENGLISH.getOrDefault(key, key);
        return Component.translatableWithFallback(key, fallback, args);
    }

    public static void ensureLanguageAvailable(MinecraftServer server, String languageCode) {
        if (server == null || languageCode == null || languageCode.isEmpty()) return;
        String normalized = normalize(languageCode);
        if ("en_us".equals(normalized)) return;
        if (!WARNED_LANGS.add(normalized)) return;

        ResourceManager manager = server.getResourceManager();
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(TQuickSwapMod.MODID, "lang/" + normalized + ".json");
        if (manager.getResource(loc).isEmpty()) {
            LOGGER.warn("[TQuickSwap] No translation found for language '{}'; falling back to en_us.", normalized);
        }
    }

    private static Map<String, String> loadEnglish() {
        Map<String, String> map = new ConcurrentHashMap<>();
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(TQuickSwapMod.MODID, "lang/en_us.json");
        try (InputStream stream = LangHelper.class.getResourceAsStream("/assets/" + loc.getNamespace() + "/" + loc.getPath());
             InputStreamReader reader = stream == null ? null : new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            if (reader == null) {
                LOGGER.warn("[TQuickSwap] Missing English translation file; fallback strings will use keys.");
                return Map.of();
            }
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[TQuickSwap] Failed to load English translations", e);
            return Map.of();
        }
        return map;
    }

    private static String normalize(String languageCode) {
        return languageCode.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
