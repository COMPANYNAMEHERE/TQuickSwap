package dev.tetralights.tquickswap;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class ProfileOps {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void swapTo(ServerPlayer p, ProfileType target, DualStore store){
        // Determine current profile based on stored active profile, not gamemode
        ProfileType current = store.last(p.getUUID());
        // Load previous snapshot for current profile to compute distance
        CompoundTag prev = store.load(p.getUUID(), current);
        double px = getDoubleOr(prev, "x", p.getX());
        double py = getDoubleOr(prev, "y", p.getY());
        double pz = getDoubleOr(prev, "z", p.getZ());
        double dx = p.getX() - px;
        double dy = p.getY() - py;
        double dz = p.getZ() - pz;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Persist current, perform swap
        store.save(p.getUUID(), current, capture(p));
        CompoundTag targetNbt = store.load(p.getUUID(), target);
        if(!targetNbt.isEmpty()) apply(p, targetNbt);
        p.setGameMode(target==ProfileType.SURVIVAL ? GameType.SURVIVAL : GameType.CREATIVE);
        // Save the applied target profile snapshot; this also records target as active
        store.save(p.getUUID(), target, capture(p));

        // Single concise log line with distance traveled
        LOGGER.info("[TQuickSwap] {} swapped {} -> {} | distance: {} blocks",
            p.getGameProfile().getName(), current, target,
            String.format(java.util.Locale.ROOT, "%.2f", dist));
    }

    static CompoundTag capture(ServerPlayer p){
        CompoundTag n = new CompoundTag();
        n.putString("world", p.level().dimension().location().toString());
        n.putDouble("x", p.getX()); n.putDouble("y", p.getY()); n.putDouble("z", p.getZ());
        n.putFloat("yaw", p.getYRot()); n.putFloat("pitch", p.getXRot());

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
        return n;
    }

    static void apply(ServerPlayer p, CompoundTag n){
        var worldId = ResourceLocation.tryParse(n.getString("world").orElse(""));
        if (worldId != null && !worldId.getPath().isEmpty()) {
            ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
            ServerLevel world = p.getServer().getLevel(worldKey);
            if(world!=null) {
                double x = getDoubleOr(n, "x", p.getX());
                double y = getDoubleOr(n, "y", p.getY());
                double z = getDoubleOr(n, "z", p.getZ());
                float yaw = getFloatOr(n, "yaw", p.getYRot());
                float pitch = getFloatOr(n, "pitch", p.getXRot());
                p.teleportTo(world, x, y, z, java.util.Set.of(), yaw, pitch, true);
            }
        }

        // Inventory and ender chest
        HolderLookup.Provider lookup = p.level().registryAccess();
        if (n.contains("inv")) loadContainer(p.getInventory(), n.getList("inv").orElse(new ListTag()), lookup);
        if (n.contains("ender")) loadContainer(p.getEnderChestInventory(), n.getList("ender").orElse(new ListTag()), lookup);

        p.experienceLevel = getIntOr(n, "level", p.experienceLevel);
        p.experienceProgress = getFloatOr(n, "xp", p.experienceProgress);
        p.giveExperiencePoints(0);
        p.setHealth(Math.min(getFloatOr(n, "health", p.getHealth()), p.getMaxHealth()));
        p.getFoodData().setFoodLevel(getIntOr(n, "food", p.getFoodData().getFoodLevel()));
        p.getFoodData().setSaturation(getFloatOr(n, "sat", p.getFoodData().getSaturationLevel()));

        // Effects
        p.removeAllEffects();
        if (n.contains("effects")) {
            ListTag l = n.getList("effects").orElse(new ListTag());
            HolderLookup.Provider lookup2 = p.level().registryAccess();
            for (int i=0; i<l.size(); i++) {
                CompoundTag ct = l.getCompound(i).orElse(new CompoundTag());
                MobEffectInstance inst = loadEffect(ct, lookup2);
                if (inst != null) p.addEffect(inst);
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
            CompoundTag t = data.getCompound(i).orElse(new CompoundTag());
            int slot = t.contains("Slot") ? ((t.getByte("Slot").orElse((byte)0)) & 255) : 0;
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

    private static MobEffectInstance loadEffect(CompoundTag t, HolderLookup.Provider lookup) {
        String idStr = t.getString("id").orElse("");
        if (idStr.isEmpty()) return null;
        var rl = ResourceLocation.tryParse(idStr);
        if (rl == null) return null;
        var key = ResourceKey.create(Registries.MOB_EFFECT, rl);
        var registry = lookup.lookupOrThrow(Registries.MOB_EFFECT);
        var holderOpt = registry.get(key);
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
        return n.contains(key) ? n.getInt(key).orElse(def) : def;
    }

    private static float getFloatOr(CompoundTag n, String key, float def) {
        return n.contains(key) ? n.getFloat(key).orElse(def) : def;
    }

    private static double getDoubleOr(CompoundTag n, String key, double def) {
        return n.contains(key) ? n.getDouble(key).orElse(def) : def;
    }

    private static boolean getBooleanOr(CompoundTag n, String key, boolean def) {
        return n.contains(key) ? n.getBoolean(key).orElse(def) : def;
    }
}
