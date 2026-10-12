package com.example.dimtowers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Floor Trial Spawner mobs (tagged dt_floor) must always hunt players. Vanilla neutral mobs (zombified piglins,
 * endermen, spiders in light) and brain-driven mobs (piglins, hoglins) never pick a target on their own, and
 * spawner-spawned mobs skip finalizeSpawn, so we hand every floor mob the nearest survival player as its target.
 */
public final class FloorMobs {
    private static final double RANGE = 40.0;

    private FloorMobs() {}

    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob mob) || !mob.getTags().contains("dt_floor")) return;
        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null && follow.getBaseValue() < RANGE) follow.setBaseValue(RANGE);
        mob.setPersistenceRequired();
    }

    public static void onTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide() || !mob.isAlive()) return;
        if (mob.tickCount % 10 != 0 || !mob.getTags().contains("dt_floor")) return;

        LivingEntity cur = mob.getTarget();
        if (cur != null && cur.isAlive() && (!(cur instanceof Player p) || (!p.isCreative() && !p.isSpectator()))) return;

        Player player = mob.level().getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), RANGE,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (player == null) return;

        mob.setTarget(player);
        if (mob instanceof ZombifiedPiglin zp) {
            zp.setPersistentAngerTarget(player.getUUID());
            zp.startPersistentAngerTimer();
        } else if (mob instanceof EnderMan em) {
            em.setTarget(player);
        } else if (mob instanceof AbstractPiglin || mob instanceof Hoglin) {
            // Brain-driven mobs ignore setTarget alone; give the brain the attack target and the anger memory.
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
            mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), 600L);
        }
    }

    /** Floor trial spawners cool down for 1 hour (72000 ticks). */
    public static final int FLOOR_COOLDOWN = 72000;

    /**
     * Towers generated before the cooldown change keep the old value baked into their spawner block entities, so fix
     * floor spawners (recognised by the dt_floor tag in their spawn data) whenever their chunk loads.
     */
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        var chunk = e.getChunk();
        level.getServer().execute(() -> {
            try {
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof TrialSpawnerBlockEntity)) continue;
                    CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
                    if (tag.getInt("target_cooldown_length") == FLOOR_COOLDOWN || !tag.toString().contains("dt_floor")) continue;
                    tag.putInt("target_cooldown_length", FLOOR_COOLDOWN);
                    be.loadWithComponents(tag, level.registryAccess());
                    be.setChanged();
                }
            } catch (Exception ignored) { }
        });
    }
}
