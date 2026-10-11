package com.example.dimtowers;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Shared boss logic for the three tower boss mobs. */
public final class BossKit {
    private BossKit() {}

    public interface TowerMob { void setup(Component name); }

    public static AttributeSupplier.Builder boost(AttributeSupplier.Builder b, double damage) {
        return b.add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.SCALE, 2.0);
    }

    public static void setup(Mob mob, Component name, boolean bow) {
        mob.setCustomName(name);
        mob.setCustomNameVisible(true);
        var enchants = mob.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack weapon;
        if (bow) {
            weapon = new ItemStack(Items.BOW);
            weapon.enchant(enchants.getOrThrow(Enchantments.POWER), 5);
            weapon.enchant(enchants.getOrThrow(Enchantments.PUNCH), 2);
        } else {
            weapon = new ItemStack(Items.NETHERITE_SWORD);
            weapon.enchant(enchants.getOrThrow(Enchantments.SHARPNESS), 5);
            weapon.enchant(enchants.getOrThrow(Enchantments.KNOCKBACK), 3);
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0f);
        mob.setPersistenceRequired();
        mob.setHealth(mob.getMaxHealth());
    }

    /** Keeps the helmet from being destroyed by daylight. */
    public static void maintain(Mob mob) {
        if (mob.tickCount % 20 != 0) return;
        ItemStack h = mob.getItemBySlot(EquipmentSlot.HEAD);
        if (h.isEmpty() || h.getDamageValue() > 0) mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
    }

    public static void dropKey(Mob mob) {
        if (!mob.level().isClientSide) mob.spawnAtLocation(new ItemStack(Items.OMINOUS_TRIAL_KEY));
    }

    public static class FireballGoal extends Goal {
        private final Mob boss;
        private int cooldown = 60;
        public FireballGoal(Mob boss) { this.boss = boss; }

        @Override public boolean canUse() { LivingEntity t = boss.getTarget(); return t != null && t.isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = boss.getTarget();
            if (t == null) return;
            if (--cooldown > 0) return;
            if (boss.distanceToSqr(t) < 16 || !boss.hasLineOfSight(t)) { cooldown = 10; return; }
            cooldown = boss.getHealth() < boss.getMaxHealth() / 2 ? 45 : 80;
            Level level = boss.level();
            double dx = t.getX() - boss.getX();
            double dy = t.getY(0.5) - boss.getY(0.5);
            double dz = t.getZ() - boss.getZ();
            for (int i = -1; i <= 1; i++) {
                SmallFireball fb = new SmallFireball(level, boss, new Vec3(dx + i * 2.5, dy, dz - i * 2.5));
                fb.setPos(boss.getX(), boss.getY(0.6), boss.getZ());
                level.addFreshEntity(fb);
            }
        }
    }
}

class TowerZombie extends Zombie implements BossKit.TowerMob {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Zombie.createAttributes(), 12.0);
    }

    @Override public void setup(Component name) { BossKit.setup(this, name, false); }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.FireballGoal(this));
    }

    @Override public void die(DamageSource source) { super.die(source); BossKit.dropKey(this); }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.maintain(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}

class TowerSkeleton extends Skeleton implements BossKit.TowerMob {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(Skeleton.createAttributes(), 7.0);
    }

    @Override public void setup(Component name) { BossKit.setup(this, name, true); }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new BossKit.FireballGoal(this));
    }

    @Override public void die(DamageSource source) { super.die(source); BossKit.dropKey(this); }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        BossKit.maintain(this);
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }
}