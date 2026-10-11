package com.example.dimtowers;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class TowerBoss extends WitherSkeleton {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerBoss(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return WitherSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.SCALE, 2.0);
    }

    public void setup(Component name) {
        setCustomName(name);
        setCustomNameVisible(true);

        var enchants = level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchants.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> knockback = enchants.getOrThrow(Enchantments.KNOCKBACK);
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        sword.enchant(sharpness, 5);
        sword.enchant(knockback, 3);

        setItemSlot(EquipmentSlot.MAINHAND, sword);
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0f);

        setPersistenceRequired();
        setHealth(getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new FireballGoal(this));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            spawnAtLocation(new ItemStack(Items.OMINOUS_TRIAL_KEY));
        }
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }
    @Override public void setCustomName(@Nullable Component n) {
        super.setCustomName(n);
        bossEvent.setName(getDisplayName());
    }

    static class FireballGoal extends Goal {
        private final TowerBoss boss;
        private int cooldown = 60;
        FireballGoal(TowerBoss boss) { this.boss = boss; }

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
