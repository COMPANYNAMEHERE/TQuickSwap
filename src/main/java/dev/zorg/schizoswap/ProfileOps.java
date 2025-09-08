package dev.zorg.schizoswap;


import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;


public class ProfileOps {
    public static void swapTo(ServerPlayerEntity p, ProfileType target, DualStore store){
        ProfileType current = (p.interactionManager.getGameMode()==GameMode.CREATIVE) ? ProfileType.CREATIVE : ProfileType.SURVIVAL;
        store.save(p.getUuid(), current, capture(p));
        NbtCompound targetNbt = store.load(p.getUuid(), target);
        if(!targetNbt.isEmpty()) apply(p, targetNbt);
        p.changeGameMode(target==ProfileType.SURVIVAL ? GameMode.SURVIVAL : GameMode.CREATIVE);
        store.save(p.getUuid(), target, capture(p));
    }


    static NbtCompound capture(ServerPlayerEntity p){
        NbtCompound n = new NbtCompound();
        n.putString("world", p.getServerWorld().getRegistryKey().getValue().toString());
        n.putDouble("x", p.getX()); n.putDouble("y", p.getY()); n.putDouble("z", p.getZ());
        n.putFloat("yaw", p.getYaw()); n.putFloat("pitch", p.getPitch());
        NbtList inv = new NbtList(); p.getInventory().writeNbt(inv); n.put("inv", inv);
        n.put("ender", p.getEnderChestInventory().toNbtList());
        n.putInt("level", p.experienceLevel); n.putFloat("xp", p.experienceProgress);
        n.putFloat("health", p.getHealth());
        n.putInt("food", p.getHungerManager().getFoodLevel());
        n.putFloat("sat", p.getHungerManager().getSaturationLevel());
        NbtList effects = new NbtList();
        for (var e : p.getStatusEffects()) effects.add(e.toNbt());
        n.put("effects", effects);
        n.putBoolean("allowFlight", p.getAbilities().allowFlying);
        n.putBoolean("flying", p.getAbilities().flying);
        return n;
    }


    static void apply(ServerPlayerEntity p, NbtCompound n){
        var worldId = Identifier.tryParse(n.getString("world"));
        var worldKey = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, worldId);
        var world = p.getServer().getWorld(worldKey);
        if(world!=null) p.teleport(world, n.getDouble("x"), n.getDouble("y"), n.getDouble("z"), n.getFloat("yaw"), n.getFloat("pitch"));


        p.getInventory().readNbt(n.getList("inv", 10));
        EnderChestInventory e = p.getEnderChestInventory();
        e.readNbtList(n.getList("ender", 10));


        p.experienceLevel = n.getInt("level"); p.experienceProgress = n.getFloat("xp"); p.addExperience(0);
        p.setHealth(Math.min(n.getFloat("health"), p.getMaxHealth()));
        p.getHungerManager().setFoodLevel(n.getInt("food"));
        p.getHungerManager().setSaturationLevel(n.getFloat("sat"));


        p.clearStatusEffects();
        var list = n.getList("effects", 10);
        for (int i=0;i<list.size();i++) p.addStatusEffect(StatusEffectInstance.fromNbt(list.getCompound(i)));


        p.getAbilities().allowFlying = n.getBoolean("allowFlight");
        p.getAbilities().flying = n.getBoolean("flying") && p.getAbilities().allowFlying;
        p.sendAbilitiesUpdate();
    }
}
