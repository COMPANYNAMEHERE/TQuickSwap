package dev.tetralights.tquickswap;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerAdvancements;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import dev.tetralights.tquickswap.LangHelper;
public class ProfileOps {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ADVANCEMENTS_KEY = "advancements";

    public static void swapTo(ServerPlayer p, ProfileType target, DualStore store){
        // Determine current profile based on stored active profile, not gamemode
        ProfileType current = store.last(p.getUUID());
        LangHelper.ensureLanguageAvailable(p.getServer(), p.getLanguage());
        // Measure distance as the teleport delta: before vs after apply
        double beforeX = p.getX();
        double beforeY = p.getY();
        double beforeZ = p.getZ();

        // Persist current, perform swap
        store.save(p.getUUID(), current, capture(p));
        DualStore.LoadedProfile targetSnapshot = store.load(p.getUUID(), target);
        CompoundTag targetNbt = targetSnapshot.data();
        if(!targetSnapshot.isEmpty()) apply(p, targetNbt);
        if (targetSnapshot.slot().isBackup() && !targetSnapshot.isEmpty() && Config.notifyOnBackupRestore()) {
            p.sendSystemMessage(LangHelper.tr("message.tquickswap.backup_auto",
                target.displayName(), targetSnapshot.slot().backupIndex())
                .withStyle(ChatFormatting.GOLD));
        }
        if (Config.switchGamemodeOnSwap()) {
            p.setGameMode(target==ProfileType.SURVIVAL ? GameType.SURVIVAL : GameType.CREATIVE);
        }
        store.save(p.getUUID(), target, capture(p));

        // Compute distance traveled due to swap (teleport delta)
        double afterX = p.getX();
        double afterY = p.getY();
        double afterZ = p.getZ();
        double dx = afterX - beforeX;
        double dy = afterY - beforeY;
        double dz = afterZ - beforeZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Single concise log line with distance traveled
        LOGGER.info("[TQuickSwap] {} swapped {} -> {} | distance: {} blocks",
            p.getGameProfile().getName(), current, target,
            String.format(java.util.Locale.ROOT, "%.2f", dist));

        SwapMetrics.recordSwap(current, target, dist, targetSnapshot.fromBackup());

        if (Config.notifyOnSwap()) {
            String distText = String.format(java.util.Locale.ROOT, "%.1f", dist);
            MutableComponent summary = LangHelper.tr("message.tquickswap.swap_summary",
                    target.displayName(), distText)
                .withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/swap menu"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        LangHelper.tr("command.tquickswap.menu.open_hint"))));
            p.sendSystemMessage(summary);
        }
    }

    static CompoundTag capture(ServerPlayer p){
        CompoundTag n = new CompoundTag();
        n.putString("world", p.level().dimension().location().toString());
        n.putDouble("x", p.getX()); n.putDouble("y", p.getY()); n.putDouble("z", p.getZ());
        n.putFloat("yaw", p.getYRot()); n.putFloat("pitch", p.getXRot());
        // Save current gamemode for profile-specific persistence when config is disabled
        try {
            var gm = p.gameMode.getGameModeForPlayer();
            if (gm != null) n.putString("gm", gm.getName());
        } catch (Throwable ignored) {}

        HolderLookup.Provider lookup = p.level().registryAccess();

        // Inventory: entire inventory and ender chest
        n.put("inv", saveContainer(p.getInventory(), lookup));
        n.put("ender", saveContainer(p.getEnderChestInventory(), lookup));

        // Experience/health/food
        n.putInt("level", p.experienceLevel); n.putFloat("xp", p.experienceProgress);
        n.putFloat("health", p.getHealth());
        n.putInt("food", p.getFoodData().getFoodLevel());
        n.putFloat("sat", p.getFoodData().getSaturationLevel());

        // Effects
        ListTag effects = new ListTag();
        for (var e : p.getActiveEffects()) effects.add(saveEffect(e));
        n.put("effects", effects);

        // Abilities
        n.putBoolean("allowFlight", p.getAbilities().mayfly);
        n.putBoolean("flying", p.getAbilities().flying);

        PlayerAdvancements adv = p.getAdvancements();
        PlayerAdvancements.Data advData = adv.asData();
        PlayerAdvancements.Data.CODEC.encodeStart(NbtOps.INSTANCE, advData)
            .resultOrPartial(err -> LOGGER.error("Failed to encode advancements for {}: {}", p.getGameProfile().getName(), err))
            .filter(tag -> {
                if (tag instanceof CompoundTag) {
                    return true;
                }
                LOGGER.error("Unexpected advancements payload type {} for {}", tag.getClass().getName(), p.getGameProfile().getName());
                return false;
            })
            .map(tag -> (CompoundTag) tag)
            .ifPresent(advTag -> n.put(ADVANCEMENTS_KEY, advTag));
        return n;
    }

    static void apply(ServerPlayer p, CompoundTag n){
        var worldId = ResourceLocation.tryParse(n.getString("world"));
        if (worldId != null && !worldId.getPath().isEmpty()) {
            ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
            ServerLevel world = p.getServer().getLevel(worldKey);
            if(world!=null) {
                double x = getDoubleOr(n, "x", p.getX());
                double y = getDoubleOr(n, "y", p.getY());
                double z = getDoubleOr(n, "z", p.getZ());
                float yaw = getFloatOr(n, "yaw", p.getYRot());
                float pitch = getFloatOr(n, "pitch", p.getXRot());
                p.teleportTo(world, x, y, z, yaw, pitch);
            }
        }

        // Inventory and ender chest
        HolderLookup.Provider lookup = p.level().registryAccess();
        if (n.contains("inv", Tag.TAG_LIST)) loadContainer(p.getInventory(), n.getList("inv", Tag.TAG_COMPOUND), lookup);
        if (n.contains("ender", Tag.TAG_LIST)) loadContainer(p.getEnderChestInventory(), n.getList("ender", Tag.TAG_COMPOUND), lookup);

        p.experienceLevel = getIntOr(n, "level", p.experienceLevel);
        p.experienceProgress = getFloatOr(n, "xp", p.experienceProgress);
        p.giveExperiencePoints(0);
        p.setHealth(Math.min(getFloatOr(n, "health", p.getHealth()), p.getMaxHealth()));
        p.getFoodData().setFoodLevel(getIntOr(n, "food", p.getFoodData().getFoodLevel()));
        p.getFoodData().setSaturation(getFloatOr(n, "sat", p.getFoodData().getSaturationLevel()));

        // Effects
        p.removeAllEffects();
        if (n.contains("effects", Tag.TAG_LIST)) {
            ListTag l = n.getList("effects", Tag.TAG_COMPOUND);
            for (int i=0; i<l.size(); i++) {
                CompoundTag ct = l.getCompound(i);
                MobEffectInstance inst = loadEffect(ct);
                if (inst != null) p.addEffect(inst);
            }
        }

        if (n.contains(ADVANCEMENTS_KEY, Tag.TAG_COMPOUND)) {
            var server = p.getServer();
            if (server != null) {
                CompoundTag advTag = n.getCompound(ADVANCEMENTS_KEY);
                PlayerAdvancements adv = p.getAdvancements();
                ServerAdvancementManager manager = server.getAdvancements();
                PlayerAdvancements.Data.CODEC.parse(NbtOps.INSTANCE, advTag)
                    .resultOrPartial(err -> LOGGER.error("Failed to decode advancements for {}: {}", p.getGameProfile().getName(), err))
                    .ifPresent(data -> {
                        adv.applyFrom(manager, data);
                        adv.flushDirty(p, true);
                    });
            }
        }

        // If config is disabled, restore saved gamemode (supports adventure/spectator)
        if (!Config.switchGamemodeOnSwap() && n.contains("gm", Tag.TAG_STRING)) {
            String gmName = n.getString("gm");
            if (!gmName.isEmpty()) {
                try {
                    GameType saved = GameType.byName(gmName, GameType.SURVIVAL);
                    p.setGameMode(saved);
                } catch (Throwable ignored) {}
            }
        }

        p.getAbilities().mayfly = getBooleanOr(n, "allowFlight", p.getAbilities().mayfly);
        p.getAbilities().flying = getBooleanOr(n, "flying", p.getAbilities().flying) && p.getAbilities().mayfly;
        p.onUpdateAbilities();
    }

    private static ListTag saveContainer(Container c, HolderLookup.Provider lookup) {
        ListTag out = new ListTag();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag t = encodeItemStack(s, lookup);
                t.putByte("Slot", (byte) i);
                out.add(t);
            }
        }
        return out;
    }

    private static void loadContainer(Container c, ListTag data, HolderLookup.Provider lookup) {
        for (int i = 0; i < c.getContainerSize(); i++) c.setItem(i, ItemStack.EMPTY);
        for (int i = 0; i < data.size(); i++) {
            CompoundTag t = data.getCompound(i);
            int slot = t.contains("Slot", Tag.TAG_BYTE) ? (t.getByte("Slot") & 255) : 0;
            ItemStack s = decodeItemStack(t, lookup);
            if (slot >= 0 && slot < c.getContainerSize()) c.setItem(slot, s);
        }
    }

    private static CompoundTag saveEffect(MobEffectInstance e) {
        CompoundTag t = new CompoundTag();
        var keyOpt = e.getEffect().unwrapKey();
        keyOpt.ifPresent(k -> t.putString("id", k.location().toString()));
        t.putInt("amplifier", e.getAmplifier());
        t.putInt("duration", e.getDuration());
        t.putBoolean("ambient", e.isAmbient());
        t.putBoolean("visible", e.isVisible());
        t.putBoolean("showIcon", e.showIcon());
        return t;
    }

    private static MobEffectInstance loadEffect(CompoundTag t) {
        String idStr = t.getString("id");
        if (idStr.isEmpty()) return null;
        var rl = ResourceLocation.tryParse(idStr);
        if (rl == null) return null;
        var key = ResourceKey.create(Registries.MOB_EFFECT, rl);
        var holderOpt = BuiltInRegistries.MOB_EFFECT.getHolder(key);
        if (holderOpt.isEmpty()) return null;
        int amp = getIntOr(t, "amplifier", 0);
        int dur = getIntOr(t, "duration", 0);
        boolean amb = getBooleanOr(t, "ambient", false);
        boolean vis = getBooleanOr(t, "visible", true);
        boolean show = getBooleanOr(t, "showIcon", true);
        return new MobEffectInstance(holderOpt.get(), dur, amp, amb, vis, show);
    }

    private static CompoundTag encodeItemStack(ItemStack s, HolderLookup.Provider lookup) {
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
        var res = ItemStack.CODEC.encodeStart(ops, s);
        Tag tag = res.result().orElseGet(CompoundTag::new);
        if (tag instanceof CompoundTag ct) return ct;
        CompoundTag wrap = new CompoundTag();
        wrap.put("v", tag);
        return wrap;
    }

    private static ItemStack decodeItemStack(CompoundTag t, HolderLookup.Provider lookup) {
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
        var res = ItemStack.CODEC.parse(ops, t);
        return res.result().orElse(ItemStack.EMPTY);
    }

    private static int getIntOr(CompoundTag n, String key, int def) {
        return n.contains(key) ? n.getInt(key) : def;
    }

    private static float getFloatOr(CompoundTag n, String key, float def) {
        return n.contains(key) ? n.getFloat(key) : def;
    }

    private static double getDoubleOr(CompoundTag n, String key, double def) {
        return n.contains(key) ? n.getDouble(key) : def;
    }

    private static boolean getBooleanOr(CompoundTag n, String key, boolean def) {
        return n.contains(key) ? n.getBoolean(key) : def;
    }
}
