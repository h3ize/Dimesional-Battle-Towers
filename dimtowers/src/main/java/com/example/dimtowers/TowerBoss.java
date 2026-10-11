package com.example.dimtowers;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class TowerBoss extends WitherSkeleton implements BossKit.TowerMob {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public TowerBoss(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return BossKit.boost(WitherSkeleton.createAttributes(), 12.0);
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