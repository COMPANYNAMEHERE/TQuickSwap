package dev.zorg.schizoswap;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;


public class ProfileOps {
    public static void swapTo(ServerPlayer p, ProfileType target, DualStore store){
        ProfileType current = p.isCreative() ? ProfileType.CREATIVE : ProfileType.SURVIVAL;
        store.save(p.getUUID(), current, capture(p));
        CompoundTag targetNbt = store.load(p.getUUID(), target);
        if(!targetNbt.isEmpty()) apply(p, targetNbt);
        p.setGameMode(target==ProfileType.SURVIVAL ? GameType.SURVIVAL : GameType.CREATIVE);
        store.save(p.getUUID(), target, capture(p));
    }


    static CompoundTag capture(ServerPlayer p){
        CompoundTag n = new CompoundTag();
        n.putString("world", p.level().dimension().location().toString());
        n.putDouble("x", p.getX()); n.putDouble("y", p.getY()); n.putDouble("z", p.getZ());
        n.putFloat("yaw", p.getYRot()); n.putFloat("pitch", p.getXRot());
        // Inventory and ender chest serialization adapted post 1.21 data component changes is omitted here
        n.putInt("level", p.experienceLevel); n.putFloat("xp", p.experienceProgress);
        n.putFloat("health", p.getHealth());
        n.putInt("food", p.getFoodData().getFoodLevel());
        n.putFloat("sat", p.getFoodData().getSaturationLevel());
        // Status effect serialization omitted to simplify Mojang mapping migration
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
                double x = n.getDouble("x").orElse(p.getX());
                double y = n.getDouble("y").orElse(p.getY());
                double z = n.getDouble("z").orElse(p.getZ());
                float yaw = n.getFloat("yaw").orElse(p.getYRot());
                float pitch = n.getFloat("pitch").orElse(p.getXRot());
                p.teleportTo(world, x, y, z, java.util.Set.of(), yaw, pitch, false);
            }
        }

        // Inventory and ender chest deserialization omitted in this mapping migration


        p.experienceLevel = n.getInt("level").orElse(p.experienceLevel);
        p.experienceProgress = n.getFloat("xp").orElse(p.experienceProgress);
        p.giveExperiencePoints(0);
        p.setHealth(Math.min(n.getFloat("health").orElse(p.getHealth()), p.getMaxHealth()));
        p.getFoodData().setFoodLevel(n.getInt("food").orElse(p.getFoodData().getFoodLevel()));
        p.getFoodData().setSaturation(n.getFloat("sat").orElse(p.getFoodData().getSaturationLevel()));


        // Effect deserialization omitted


        p.getAbilities().mayfly = n.getBoolean("allowFlight").orElse(p.getAbilities().mayfly);
        p.getAbilities().flying = n.getBoolean("flying").orElse(p.getAbilities().flying) && p.getAbilities().mayfly;
        p.onUpdateAbilities();
    }
}
