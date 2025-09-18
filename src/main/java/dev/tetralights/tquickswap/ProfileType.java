package dev.tetralights.tquickswap;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum ProfileType {
    SURVIVAL,
    CREATIVE;

    public Component displayName() {
        String key = "tquickswap.profile." + name().toLowerCase(Locale.ROOT);
        return LangHelper.tr(key);
    }

    public String commandKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
