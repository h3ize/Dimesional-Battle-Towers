package com.example.dimtowers;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Shared boss logic for the four tower boss mobs. */
public final class BossKit {
    private BossKit() {}

    public enum Weapon { SWORD, BOW, AXE }

    public static final double DMG_ZOMBIE = 18.0, DMG_SKELETON = 10.0, DMG_WITHER = 18.0, DMG_PIGLIN = 20.0;
    public static final int XP = 1000; // ~ level 26 in one go

    public static AttributeSupplier.Builder boost(AttributeSupplier.Builder b, double damage) {
        return b.add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0)
                .add(Attributes.SCALE, 2.0);
    }

    /** Called from finalizeSpawn, so it works for the Trial Spawner, eggs and commands alike. */
    public static void setup(Mob mob, Weapon weaponType, double damage) {
        mob.setCustomName(mob.getType().getDescription());
        mob.setCustomNameVisible(true);
        var enchants = mob.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack weapon;
        switch (weaponType) {
            case BOW -> {
                weapon = new ItemStack(Items.BOW);
                weapon.enchant(enchants.getOrThrow(Enchantments.POWER), 8);
                weapon.enchant(enchants.getOrThrow(Enchantments.PUNCH), 2);
            }
            case AXE -> {
                weapon = new ItemStack(Items.NETHERITE_AXE);
                weapon.enchant(enchants.getOrThrow(Enchantments.SHARPNESS), 5);
            }
            default -> {
                weapon = new ItemStack(Items.NETHERITE_SWORD);
                weapon.enchant(enchants.getOrThrow(Enchantments.SHARPNESS), 5);
                weapon.enchant(enchants.getOrThrow(Enchantments.KNOCKBACK), 3);
            }
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0f);
        // vanilla finalizeSpawn (e.g. wither skeleton) resets damage, so set it again here
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        if (mob instanceof AbstractPiglin p) p.setImmuneToZombification(true);
        mob.setPersistenceRequired();
        mob.setHealth(mob.getMaxHealth());
    }

    @Nullable
    public static LivingEntity target(Mob mob) {
        LivingEntity t = mob.getTarget();
        // Only Brain-based mobs (piglins) register ATTACK_TARGET. Goal-based mobs such as
        // zombies and skeletons throw "Unregistered memory fetched" if asked for it.
        if (t == null && mob instanceof AbstractPiglin) {
            t = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        }
        return t;
    }

    /** Per-tick boss behaviour: gear upkeep, enrage at 50%, minion waves, lightning while enraged. */
    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        if (mob.tickCount % 20 == 0) {
            ItemStack h = mob.getItemBySlot(EquipmentSlot.HEAD);
            if (h.isEmpty() || h.getDamageValue() > 0) mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        }
        float frac = mob.getHealth() / mob.getMaxHealth();
        if (frac <= 0.75f) wave(mob, sl, "dt_wave1", 2);
        if (frac <= 0.40f) wave(mob, sl, "dt_wave2", 3);
        if (frac <= 0.15f) wave(mob, sl, "dt_wave3", 4);
        if (frac <= 0.5f && !mob.getTags().contains("dt_enraged")) {
            mob.addTag("dt_enraged");
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 1, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 1, false, true));
            sl.playSound(null, mob.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0f, 1.0f);
        }
        if (mob.getTags().contains("dt_enraged") && mob.tickCount % 140 == 0) {
            LivingEntity t = target(mob);
            if (t != null && t.isAlive()) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(t.blockPosition()));
                    sl.addFreshEntity(bolt);
                }
            }
        }
    }

    private static void wave(Mob boss, ServerLevel sl, String tag, int count) {
        if (boss.getTags().contains(tag)) return;
        boss.addTag(tag);
        for (int i = 0; i < count; i++) {
            EntityType<? extends Mob> type;
            if (boss instanceof AbstractPiglin) type = EntityType.PIGLIN_BRUTE;
            else switch (boss.getRandom().nextInt(3)) {
                case 0 -> type = EntityType.ZOMBIE;
                case 1 -> type = EntityType.SKELETON;
                default -> type = EntityType.WITHER_SKELETON;
            }
            Mob m = type.create(sl);
            if (m == null) continue;
            m.moveTo(boss.getX() + (boss.getRandom().nextDouble() - 0.5) * 6, boss.getY(),
                    boss.getZ() + (boss.getRandom().nextDouble() - 0.5) * 6, boss.getRandom().nextFloat() * 360f, 0f);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            for (EquipmentSlot slot : EquipmentSlot.values()) m.setDropChance(slot, 0f);
            m.setPersistenceRequired();
            m.addTag("dt_minion");
            m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            LivingEntity t = target(boss);
            if (t != null) m.setTarget(t);
            sl.addFreshEntity(m);
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0f, 0.8f);
    }

    public static class FireballGoal extends Goal {
        private final Mob boss;
        private int cooldown = 60;
        public FireballGoal(Mob boss) { this.boss = boss; }

        @Override public boolean canUse() { LivingEntity t = target(boss); return t != null && t.isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = target(boss);
            if (t == null) return;
            if (--cooldown > 0) return;
            if (boss.distanceToSqr(t) < 16 || !boss.hasLineOfSight(t)) { cooldown = 10; return; }
            boolean enraged = boss.getTags().contains("dt_enraged");
            cooldown = enraged ? 35 : 80;
            Level level = boss.level();
            double dx = t.getX() - boss.getX();
            double dy = t.getY(0.5) - boss.getY(0.5);
            double dz = t.getZ() - boss.getZ();
            int n = enraged ? 2 : 1;
            for (int i = -n; i <= n; i++) {
                SmallFireball fb = new SmallFireball(level, boss, new Vec3(dx + i * 2.5, dy, dz - i * 2.5));
                fb.setPos(boss.getX(), boss.getY(0.6), boss.getZ());
                level.addFreshEntity(fb);
            }
        }
    }
}

class TowerZombie extends Zombie {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Zombie.createAttributes(), BossKit.DMG_ZOMBIE)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override public void setBaby(boolean baby) { super.setBaby(false); }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.SWORD, BossKit.DMG_ZOMBIE);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.FireballGoal(this));
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerSkeleton extends Skeleton {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Skeleton.createAttributes(), BossKit.DMG_SKELETON);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.BOW, BossKit.DMG_SKELETON);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.FireballGoal(this));
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerPiglin extends PiglinBrute {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerPiglin(EntityType<? extends PiglinBrute> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(PiglinBrute.createAttributes(), BossKit.DMG_PIGLIN);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.AXE, BossKit.DMG_PIGLIN);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.FireballGoal(this));
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}
