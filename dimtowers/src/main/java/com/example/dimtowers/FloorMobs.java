package com.example.dimtowers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.trialspawner.TrialSpawnerState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.LevelChunk;
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
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
            mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), 600L);
        }
    }

    public static final int FLOOR_COOLDOWN = 72000;

    public static final int FLOOR_RANGE = 22;

    private static final double FLOOR_VERTICAL = 5.0;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> FLOOR_SPAWNERS = new ConcurrentHashMap<>();

    public static void onLevelTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now % 5 != 0) return;
        Set<BlockPos> spawners = FLOOR_SPAWNERS.get(level.dimension());
        if (spawners == null || spawners.isEmpty()) return;
        java.util.List<ServerPlayer> players = new java.util.ArrayList<>();
        for (ServerPlayer p : level.players()) if (p.isAlive() && !p.isCreative() && !p.isSpectator()) players.add(p);
        if (players.isEmpty()) return;
        for (BlockPos pos : spawners) {
            if (!level.isLoaded(pos)) continue;
            java.util.List<ServerPlayer> near = new java.util.ArrayList<>();
            for (ServerPlayer p : players) {
                double dx = p.getX() - (pos.getX() + 0.5), dz = p.getZ() - (pos.getZ() + 0.5);
                if (dx * dx + dz * dz <= (double) FLOOR_RANGE * FLOOR_RANGE && Math.abs(p.getY() - pos.getY()) <= FLOOR_VERTICAL) near.add(p);
            }
            if (near.isEmpty()) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.TRIAL_SPAWNER)) {
                spawners.remove(pos);
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof TrialSpawnerBlockEntity)) continue;
            TrialSpawnerState st = state.getValue(BlockStateProperties.TRIAL_SPAWNER_STATE);
            if (st != TrialSpawnerState.WAITING_FOR_PLAYERS && st != TrialSpawnerState.ACTIVE) continue;
            try {
                CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
                boolean changed = false;
                if (st == TrialSpawnerState.WAITING_FOR_PLAYERS) {
                    ListTag reg = new ListTag();
                    for (ServerPlayer p : near) reg.add(NbtUtils.createUUID(p.getUUID()));
                    tag.put("registered_players", reg);
                    changed = true;
                } else if (tag.getInt("total_mobs_spawned") == 0 && tag.getLong("next_mob_spawns_at") > now) {
                    tag.putLong("next_mob_spawns_at", now);
                    changed = true;
                }
                if (!changed) continue;
                be.loadWithComponents(tag, level.registryAccess());
                be.setChanged();
            } catch (Exception ignored) { }
        }
    }

    public static final int FLOOR_SPAWN_TICKS = 2;

    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getChunk() instanceof LevelChunk chunk)) return;
        level.getServer().execute(() -> {
            try {
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof TrialSpawnerBlockEntity)) continue;
                    CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
                    if (!tag.toString().contains("dt_floor")) continue;
                    FLOOR_SPAWNERS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(be.getBlockPos().immutable());
                    boolean changed = false;
                    if (tag.getInt("target_cooldown_length") != FLOOR_COOLDOWN) {
                        tag.putInt("target_cooldown_length", FLOOR_COOLDOWN);
                        changed = true;
                    }
                    if (tag.getInt("required_player_range") != FLOOR_RANGE) {
                        tag.putInt("required_player_range", FLOOR_RANGE);
                        changed = true;
                    }
                    for (String key : new String[]{"normal_config", "ominous_config"}) {
                        if (!tag.contains(key, Tag.TAG_COMPOUND)) continue;
                        CompoundTag cfg = tag.getCompound(key);
                        ListTag pots = cfg.getList("spawn_potentials", Tag.TAG_COMPOUND);
                        for (int j = 0; j < pots.size(); j++) {
                            CompoundTag entity = pots.getCompound(j).getCompound("data").getCompound("entity");
                            if (entity.getString("id").equals("minecraft:creeper")) {
                                entity.putString("id", "minecraft:zombie");
                                changed = true;
                            }
                        }
                        if (cfg.getInt("ticks_between_spawn") != FLOOR_SPAWN_TICKS) {
                            cfg.putInt("ticks_between_spawn", FLOOR_SPAWN_TICKS);
                            changed = true;
                        }
                    }
                    if (!changed) continue;
                    be.loadWithComponents(tag, level.registryAccess());
                    be.setChanged();
                }
            } catch (Exception ignored) { }
        });
    }
}
