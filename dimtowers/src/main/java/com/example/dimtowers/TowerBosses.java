package com.example.dimtowers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootTable;

import javax.annotation.Nullable;

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
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.GROUND_SLAM, BossKit.Move.PULL, BossKit.Move.RALLY, BossKit.Move.LIFE_DRAIN, BossKit.Move.LEAP));
        this.registerBossGoals();
    }
    protected void registerBossGoals() { }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossAiStep();
        BossKit.setup(this, BossKit.Weapon.SWORD, BossKit.DMG_ZOMBIE);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    protected void bossAiStep() { }
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
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.ARROW_VOLLEY, BossKit.Move.BACKSTEP, BossKit.Move.RALLY, BossKit.Move.STORM_CALL, BossKit.Move.CURSE));
        this.registerBossGoals();
    }
    protected void registerBossGoals() { }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossAiStep();
        BossKit.setup(this, BossKit.Weapon.BOW, BossKit.DMG_SKELETON);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    protected void bossAiStep() { }
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
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.CALL_HOGLINS, BossKit.Move.WAR_CRY, BossKit.Move.CHARGE, BossKit.Move.FIRE_NOVA, BossKit.Move.GROUND_SLAM, BossKit.Move.LEAP));
        this.registerBossGoals();
    }
    protected void registerBossGoals() { }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossAiStep();
        BossKit.setup(this, BossKit.Weapon.AXE, BossKit.DMG_PIGLIN);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    protected void bossAiStep() { }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerVindicator extends Vindicator {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerVindicator(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Vindicator.createAttributes(), BossKit.DMG_VINDICATOR);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.AXE, BossKit.DMG_VINDICATOR);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.LEAP, BossKit.Move.CHARGE, BossKit.Move.WAR_CRY, BossKit.Move.GROUND_SLAM, BossKit.Move.SHIELD));
        this.registerBossGoals();
    }
    protected void registerBossGoals() { }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossAiStep();
        BossKit.setup(this, BossKit.Weapon.AXE, BossKit.DMG_VINDICATOR);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    protected void bossAiStep() { }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerBlaze extends Blaze {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerBlaze(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Blaze.createAttributes(), BossKit.DMG_BLAZE);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_BLAZE);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.BLAZE_BARRAGE, BossKit.Move.FIRE_NOVA, BossKit.Move.FIRE_RAIN, BossKit.Move.METEOR, BossKit.Move.SHIELD));
        this.registerBossGoals();
    }
    protected void registerBossGoals() { }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossAiStep();
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_BLAZE);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    protected void bossAiStep() { }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerDrowned extends Drowned {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerDrowned(EntityType<? extends Drowned> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Zombie.createAttributes(), BossKit.DMG_DROWNED)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override public void setBaby(boolean baby) { super.setBaby(false); }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.TRIDENT, BossKit.DMG_DROWNED);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.TRIDENT_FAN, BossKit.Move.PULL, BossKit.Move.RALLY, BossKit.Move.STORM_CALL, BossKit.Move.ROAR, BossKit.Move.SHIELD));
        this.registerBossGoals();
    }
    protected void registerBossGoals() { }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossAiStep();
        BossKit.setup(this, BossKit.Weapon.TRIDENT, BossKit.DMG_DROWNED);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    protected void bossAiStep() { }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerGuardian extends Guardian {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerGuardian(EntityType<? extends Guardian> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Guardian.createAttributes(), BossKit.DMG_GUARDIAN);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_GUARDIAN);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.GROUND_SLAM, BossKit.Move.ROAR, BossKit.Move.RALLY, BossKit.Move.LIFE_DRAIN, BossKit.Move.CURSE, BossKit.Move.STORM_CALL));
        this.goalSelector.addGoal(3, new BossKit.HoverGoal(this));
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_GUARDIAN);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerEvoker extends Evoker {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerEvoker(EntityType<? extends Evoker> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Evoker.createAttributes(), BossKit.DMG_EVOKER);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        this.setPatrolLeader(false);
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_EVOKER);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.FANG_WAVE, BossKit.Move.FANG_STORM, BossKit.Move.FANG_RING, BossKit.Move.CURSE, BossKit.Move.VEX_SWARM, BossKit.Move.STORM_CALL, BossKit.Move.SHIELD, BossKit.Move.LIFE_DRAIN));
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_EVOKER);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerRavager extends Ravager {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerRavager(EntityType<? extends Ravager> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Ravager.createAttributes(), BossKit.DMG_RAVAGER, 1.4);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        this.setPatrolLeader(false);
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_RAVAGER);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.LEAP, BossKit.Move.GROUND_SLAM, BossKit.Move.CHARGE, BossKit.Move.ROAR, BossKit.Move.PULL, BossKit.Move.RALLY));
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_RAVAGER);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerEnder extends EnderMan {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerEnder(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(EnderMan.createAttributes(), BossKit.DMG_ENDER);
    }

    protected void registerBossGoals() {
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.getAvailableGoals().removeIf(w -> w.getGoal().getClass().getSimpleName().contains("Block"));
    }

    @Override protected boolean teleport() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE) && source.getEntity() instanceof LivingEntity shooter) {
            DamageSource ds = shooter instanceof Player p ? this.damageSources().playerAttack(p) : this.damageSources().mobAttack(shooter);
            return super.hurt(ds, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_ENDER);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.DRAGON_BREATH, BossKit.Move.PULL, BossKit.Move.CURSE, BossKit.Move.STORM_CALL, BossKit.Move.SHIELD));
        this.registerBossGoals();
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossAiStep();
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_ENDER);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    protected void bossAiStep() { }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerSlime extends Slime {
    private static final int KING_SIZE = 6;
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private int lastSplit = -1000;

    public TowerSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        this.xpReward = BossKit.XP;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Monster.createMonsterAttributes(), BossKit.DMG_SLIME, 1.0);
    }

    @Override
    public void setSize(int size, boolean resetHealth) {
        super.setSize(size, resetHealth);
        if (size >= KING_SIZE) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BossKit.HEALTH);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BossKit.DMG_SLIME);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
            this.xpReward = BossKit.XP;
            if (resetHealth) this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() { return this.getType().getDefaultLootTable(); }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, type, data);
        this.setSize(KING_SIZE, true);
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_SLIME);
        return d;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.SignatureGoal(this, BossKit.Move.GROUND_SLAM, BossKit.Move.LEAP, BossKit.Move.RALLY, BossKit.Move.ROAR, BossKit.Move.SHIELD, BossKit.Move.CHARGE));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && this.level() instanceof ServerLevel sl && this.isAlive() && this.tickCount - lastSplit > 60
                && BossKit.countMinions(sl, this) < 10) {
            lastSplit = this.tickCount;
            for (int i = 0; i < 2; i++) {
                Slime s = EntityType.SLIME.create(sl);
                if (s == null) continue;
                s.moveTo(this.getX() + (this.random.nextDouble() - 0.5) * 4, this.getY(), this.getZ() + (this.random.nextDouble() - 0.5) * 4,
                        this.random.nextFloat() * 360f, 0f);
                s.setSize(2, true);
                s.setPersistenceRequired();
                s.addTag("dt_minion");
                LivingEntity t = BossKit.target(this);
                if (t != null) s.setTarget(t);
                sl.addFreshEntity(s);
            }
        }
        return r;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (this.isDeadOrDying() && this.getSize() > 1) {
            super.setSize(1, false);
        }
        super.remove(reason);
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (!this.getTags().contains("dt_ready")) this.setSize(KING_SIZE, true);
        BossKit.setup(this, BossKit.Weapon.NONE, BossKit.DMG_SLIME);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.tick(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}
