package com.example.dimtowers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class BossKit {
    private BossKit() {}

    public enum Weapon { SWORD, BOW, AXE, TRIDENT, NONE }

    public enum Move {
        WITHER_SKULLS,
        ARROW_VOLLEY,
        GROUND_SLAM,
        CALL_HOGLINS,
        LEAP,
        BLAZE_BARRAGE,
        FIRE_NOVA,
        BLINK,
        DRAGON_BREATH,
        TRIDENT_FAN,
        FANG_WAVE,
        FANG_RING,
        FANG_STORM,
        CURSE,
        VEX_SWARM,
        PULL,
        RALLY,
        WAR_CRY,
        BACKSTEP,
        WITHER_NOVA,
        CHARGE,
        ROAR,
        FIRE_RAIN,
        STORM_CALL,
        SHIELD,
        LIFE_DRAIN,
        METEOR
    }

    public static final double DMG_ZOMBIE = 20.0, DMG_SKELETON = 12.0, DMG_WITHER = 20.0, DMG_PIGLIN = 22.0,
            DMG_VINDICATOR = 22.0, DMG_BLAZE = 16.0, DMG_ENDER = 24.0, DMG_SLIME = 20.0,
            DMG_DROWNED = 22.0, DMG_GUARDIAN = 22.0, DMG_EVOKER = 18.0, DMG_RAVAGER = 26.0;
    public static final double HEALTH = 750.0;
    public static final int XP = 3000;

    public static AttributeSupplier.Builder boost(AttributeSupplier.Builder b, double damage) {
        return boost(b, damage, 2.0);
    }

    public static AttributeSupplier.Builder boost(AttributeSupplier.Builder b, double damage, double scale) {
        return b.add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0)
                .add(Attributes.SCALE, scale);
    }

    public static Theme theme(Entity e) {
        for (String s : e.getTags()) if (s.startsWith("dt_theme_")) return Theme.byName(s.substring(9));
        return Theme.OVERWORLD;
    }

    public static void setup(Mob mob, Weapon weaponType, double damage) {
        if (mob.getTags().contains("dt_ready")) return;
        mob.addTag("dt_ready");
        float frac = mob.getMaxHealth() <= 0 ? 1f : mob.getHealth() / mob.getMaxHealth();
        mob.setCustomName(mob.getType().getDescription());
        mob.setCustomNameVisible(true);
        if (weaponType == Weapon.NONE) {
            mob.addTag("dt_nogear");
        } else {
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
                    weapon.enchant(enchants.getOrThrow(Enchantments.SHARPNESS), 2);
                }
                case TRIDENT -> {
                    weapon = new ItemStack(Items.TRIDENT);
                    weapon.enchant(enchants.getOrThrow(Enchantments.IMPALING), 5);
                }
                default -> {
                    weapon = new ItemStack(Items.NETHERITE_SWORD);
                    weapon.enchant(enchants.getOrThrow(Enchantments.SHARPNESS), 3);
                    weapon.enchant(enchants.getOrThrow(Enchantments.KNOCKBACK), 3);
                }
            }
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0f);
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        if (mob instanceof AbstractPiglin p) p.setImmuneToZombification(true);
        mob.setPersistenceRequired();
        mob.setHealth(mob.getMaxHealth() * frac);
    }

    @Nullable
    public static LivingEntity target(Mob mob) {
        LivingEntity t = mob.getTarget();
        if (t == null && mob instanceof AbstractPiglin) {
            t = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        }
        return t;
    }

    private static void announce(ServerLevel sl, Mob boss, String key) {
        Component msg = Component.translatable(key, boss.getDisplayName()).withStyle(ChatFormatting.RED);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(boss) < 64.0 * 64.0) p.displayClientMessage(msg, true);
    }

    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        if (mob.tickCount % 20 == 0 && !mob.getTags().contains("dt_nogear")) {
            ItemStack h = mob.getItemBySlot(EquipmentSlot.HEAD);
            if (h.isEmpty() || h.getDamageValue() > 0) mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        }
        float frac = mob.getHealth() / mob.getMaxHealth();
        if (frac <= 0.75f) wave(mob, sl, "dt_wave1", 4, 0);
        if (frac <= 0.40f) wave(mob, sl, "dt_wave2", 5, 1);
        if (frac <= 0.15f) wave(mob, sl, "dt_wave3", 6, 2);
        if (frac <= 0.5f && !mob.getTags().contains("dt_enraged")) {
            mob.addTag("dt_enraged");
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 1, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 1, false, true));
            sl.playSound(null, mob.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0f, 1.0f);
            announce(sl, mob, "message.dimtowers.enrage");
        }
        if (frac <= 0.25f && !mob.getTags().contains("dt_desperate")) {
            mob.addTag("dt_desperate");
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 2, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 2, false, true));
            sl.playSound(null, mob.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.8f);
            announce(sl, mob, "message.dimtowers.desperate");
        }
        int strikeEvery = mob.getTags().contains("dt_desperate") ? 80 : 140;
        if (mob.getTags().contains("dt_enraged") && mob.tickCount % strikeEvery == 0) {
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

    private static void wave(Mob boss, ServerLevel sl, String tag, int count, int boostLevel) {
        if (boss.getTags().contains(tag)) return;
        boss.addTag(tag);
        List<EntityType<?>> pool = theme(boss).waveMobs();
        for (int i = 0; i < count; i++) {
            spawnMinion(sl, boss, pool.get(boss.getRandom().nextInt(pool.size())), boostLevel);
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0f, 0.8f);
        announce(sl, boss, "message.dimtowers.wave");
    }

    @Nullable
    public static Mob spawnMinion(ServerLevel sl, Mob boss, EntityType<?> type, int boostLevel) {
        Entity e = type.create(sl);
        if (!(e instanceof Mob m)) return null;
        RandomSource r = boss.getRandom();
        m.moveTo(boss.getX() + (r.nextDouble() - 0.5) * 8, boss.getY(), boss.getZ() + (r.nextDouble() - 0.5) * 8,
                r.nextFloat() * 360f, 0f);
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        for (EquipmentSlot slot : EquipmentSlot.values()) m.setDropChance(slot, 0f);
        m.setPersistenceRequired();
        m.addTag("dt_minion");
        m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        if (boostLevel > 0) {
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, boostLevel - 1, false, false));
        }
        if (m instanceof AbstractPiglin p) p.setImmuneToZombification(true);
        if (m instanceof Hoglin h) h.setImmuneToZombification(true);
        LivingEntity t = target(boss);
        if (t != null) m.setTarget(t);
        sl.addFreshEntity(m);
        return m;
    }

    public static int countMinions(ServerLevel sl, Mob boss) {
        return sl.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(40),
                m -> m.getTags().contains("dt_minion") && m.isAlive()).size();
    }

    private static boolean isTowerBoss(LivingEntity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(DimensionalBattleTowers.MOD_ID)
                && !e.getTags().contains("dt_minion");
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel sl) || !isTowerBoss(dead)) return;
        List<Mob> minions = sl.getEntitiesOfClass(Mob.class, dead.getBoundingBox().inflate(64),
                m -> m.getTags().contains("dt_minion") && m.isAlive());
        for (Mob m : minions) {
            sl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + m.getBbHeight() / 2, m.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
            m.discard();
        }
        List<Vex> vexes = sl.getEntitiesOfClass(Vex.class, dead.getBoundingBox().inflate(64), v -> v.getOwner() == dead);
        for (Vex v : vexes) v.discard();
    }

    public static class HoverGoal extends Goal {
        private final Mob mob;

        public HoverGoal(Mob mob) {
            this.mob = mob;
        }

        @Nullable
        private Player nearest() {
            return mob.level().getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 40.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        }

        @Override public boolean canUse() { return nearest() != null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            Player p = nearest();
            if (p == null) return;
            double dx = p.getX() - mob.getX(), dz = p.getZ() - mob.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double ax = 0.0, az = 0.0;
            if (dist > 1.0E-3) {
                double push = dist > 9.0 ? 0.05 : dist < 5.0 ? -0.05 : 0.0;
                double side = (mob.tickCount / 100) % 2 == 0 ? 0.02 : -0.02;
                ax = dx / dist * push - dz / dist * side;
                az = dz / dist * push + dx / dist * side;
            }
            double ay = Math.max(-0.05, Math.min(0.05, (p.getY() + 2.0 - mob.getY()) * 0.05));
            mob.setDeltaMovement(mob.getDeltaMovement().add(ax, ay, az));
        }
    }

    private interface Proj { @Nullable Entity make(Level level, Mob boss, Vec3 dir, int index); }

    public static class SignatureGoal extends Goal {
        private final Mob boss;
        private final Move[] moves;
        private final int[] cd;
        private int windup = -1;
        private int windupIdx = -1;
        private int leapTicks = 0;
        private int chargeTicks = 0;
        private Vec3 chargeDir = Vec3.ZERO;
        private final List<double[]> bolts = new ArrayList<>();

        public SignatureGoal(Mob boss, Move... moves) {
            this.boss = boss;
            this.moves = moves;
            this.cd = new int[moves.length];
            for (int i = 0; i < cd.length; i++) cd[i] = 60 + 50 * i;
        }

        @Override public boolean canUse() { LivingEntity t = target(boss); return t != null && t.isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() {
            windup = -1;
            bolts.clear();
        }

        @Override
        public void tick() {
            if (!(boss.level() instanceof ServerLevel sl)) return;
            LivingEntity t = target(boss);
            if (t == null) return;
            boolean enraged = boss.getTags().contains("dt_enraged");

            if (leapTicks > 0) {
                leapTicks--;
                if (leapTicks < 34 && boss.onGround()) { leapTicks = 0; slam(sl, 6.0, 12f); }
            }
            for (int k = bolts.size() - 1; k >= 0; k--) {
                double[] b = bolts.get(k);
                b[3]--;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, b[0], b[1] + 0.2, b[2], 6, 0.6, 0.1, 0.6, 0.02);
                if (b[3] <= 0) {
                    LightningBolt strike = EntityType.LIGHTNING_BOLT.create(sl);
                    if (strike != null) {
                        strike.moveTo(b[0], b[1], b[2]);
                        sl.addFreshEntity(strike);
                    }
                    bolts.remove(k);
                }
            }
            if (chargeTicks > 0) {
                chargeTicks--;
                boss.setDeltaMovement(chargeDir.x, boss.getDeltaMovement().y, chargeDir.z);
                for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(1.0),
                        x -> x != boss && x.isAlive() && !(x instanceof Enemy))) {
                    v.hurt(sl.damageSources().mobAttack(boss), 10f);
                    v.setDeltaMovement(v.getDeltaMovement().add(chargeDir.x * 0.9, 0.5, chargeDir.z * 0.9));
                    v.hurtMarked = true;
                }
            }
            if (windup > 0) {
                windup--;
                if (windup % 4 == 0) {
                    sl.sendParticles(ParticleTypes.CRIT, boss.getX(), boss.getY() + 0.2, boss.getZ(), 12, 1.5, 0.1, 1.5, 0.1);
                }
                if (windup == 0) { windup = -1; execute(windupIdx, sl, t, enraged); }
                return;
            }
            for (int i = 0; i < cd.length; i++) {
                if (cd[i] > 0) cd[i]--;
                if (cd[i] > 0 && boss.tickCount % 4 == 0) cd[i]--;
            }
            for (int i = 0; i < moves.length; i++) if (cd[i] <= 0 && start(i, sl, t, enraged)) break;
        }

        private void begin(int idx, int windupTicks, int cooldown) {
            windup = windupTicks;
            windupIdx = idx;
            cd[idx] = cooldown;
        }

        private void sound(ServerLevel sl, SoundEvent s, float vol, float pitch) {
            sl.playSound(null, boss.blockPosition(), s, SoundSource.HOSTILE, vol, pitch);
        }

        private boolean start(int i, ServerLevel sl, LivingEntity t, boolean enraged) {
            double d2 = boss.distanceToSqr(t);
            switch (moves[i]) {
                case GROUND_SLAM -> {
                    if (d2 > 144) return false;
                    begin(i, 20, enraged ? 90 : 150);
                    sound(sl, SoundEvents.ANVIL_LAND, 1.5f, 0.6f);
                    return true;
                }
                case LEAP -> {
                    if (d2 < 36 || d2 > 576 || !boss.onGround() || !boss.hasLineOfSight(t)) return false;
                    begin(i, 14, enraged ? 100 : 160);
                    sound(sl, SoundEvents.RAVAGER_ROAR, 1.5f, 1.0f);
                    return true;
                }
                case CALL_HOGLINS -> {
                    if (countMinions(sl, boss) >= 8) { cd[i] = 100; return false; }
                    int n = enraged ? 3 : 2;
                    for (int j = 0; j < n; j++) spawnMinion(sl, boss, EntityType.HOGLIN, enraged ? 1 : 0);
                    sound(sl, SoundEvents.HOGLIN_ANGRY, 2.0f, 0.7f);
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, boss.getX(), boss.getY(1.0), boss.getZ(), 15, 1, 1, 1, 0.0);
                    cd[i] = enraged ? 260 : 400;
                    return true;
                }
                case STORM_CALL -> {
                    if (d2 > 900) return false;
                    RandomSource r = boss.getRandom();
                    int n = enraged ? 6 : 4;
                    for (int j = 0; j < n; j++) {
                        double ox = j == 0 ? 0.0 : (r.nextDouble() - 0.5) * 10.0;
                        double oz = j == 0 ? 0.0 : (r.nextDouble() - 0.5) * 10.0;
                        bolts.add(new double[]{t.getX() + ox, t.getY(), t.getZ() + oz, 30 + j * 4});
                    }
                    sound(sl, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);
                    cd[i] = enraged ? 160 : 260;
                    return true;
                }
                case SHIELD -> {
                    if (boss.hasEffect(MobEffects.DAMAGE_RESISTANCE)) return false;
                    begin(i, 10, enraged ? 400 : 520);
                    sound(sl, SoundEvents.BEACON_ACTIVATE, 2.0f, 0.8f);
                    return true;
                }
                case LIFE_DRAIN -> {
                    if (d2 > 100) return false;
                    begin(i, 20, enraged ? 200 : 300);
                    sound(sl, SoundEvents.WARDEN_SONIC_CHARGE, 2.0f, 1.0f);
                    return true;
                }
                case METEOR -> {
                    if (d2 > 900) return false;
                    RandomSource r = boss.getRandom();
                    int n = enraged ? 6 : 4;
                    for (int j = 0; j < n; j++) {
                        LargeFireball lf = new LargeFireball(sl, boss, new Vec3(0, -1, 0), 1);
                        lf.setPos(t.getX() + (r.nextDouble() - 0.5) * 12, t.getY() + 16, t.getZ() + (r.nextDouble() - 0.5) * 12);
                        sl.addFreshEntity(lf);
                    }
                    sound(sl, SoundEvents.GHAST_SHOOT, 2.0f, 0.5f);
                    cd[i] = enraged ? 170 : 280;
                    return true;
                }
                case PULL -> {
                    if (d2 > 400 || d2 < 36) return false;
                    begin(i, 15, enraged ? 180 : 280);
                    sound(sl, SoundEvents.TRIDENT_RIPTIDE_1, 2.0f, 0.6f);
                    return true;
                }
                case RALLY -> {
                    if (countMinions(sl, boss) >= 8) { cd[i] = 100; return false; }
                    List<EntityType<?>> pool = theme(boss).waveMobs();
                    int n = enraged ? 4 : 3;
                    for (int j = 0; j < n; j++) spawnMinion(sl, boss, pool.get(boss.getRandom().nextInt(pool.size())), enraged ? 1 : 0);
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, boss.getX(), boss.getY(1.0), boss.getZ(), 20, 1, 1, 1, 0.0);
                    sound(sl, SoundEvents.EVOKER_PREPARE_SUMMON, 2.0f, 0.7f);
                    cd[i] = enraged ? 300 : 450;
                    return true;
                }
                case WAR_CRY -> {
                    if (countMinions(sl, boss) == 0) { cd[i] = 60; return false; }
                    begin(i, 15, enraged ? 260 : 380);
                    sound(sl, SoundEvents.RAVAGER_ROAR, 2.0f, 0.8f);
                    return true;
                }
                case BACKSTEP -> {
                    if (d2 > 36 || !boss.onGround()) return false;
                    Vec3 away = new Vec3(boss.getX() - t.getX(), 0, boss.getZ() - t.getZ());
                    if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                    away = away.normalize();
                    boss.setDeltaMovement(away.x * 1.1, 0.5, away.z * 1.1);
                    sound(sl, SoundEvents.SKELETON_AMBIENT, 2.0f, 0.6f);
                    cd[i] = enraged ? 80 : 130;
                    return true;
                }
                case WITHER_NOVA -> {
                    if (d2 > 900) return false;
                    begin(i, 20, enraged ? 120 : 200);
                    sound(sl, SoundEvents.WITHER_AMBIENT, 2.0f, 0.7f);
                    return true;
                }
                case CHARGE -> {
                    if (d2 < 36 || d2 > 400 || !boss.onGround() || !boss.hasLineOfSight(t)) return false;
                    begin(i, 16, enraged ? 110 : 170);
                    sound(sl, SoundEvents.RAVAGER_ROAR, 1.5f, 0.8f);
                    return true;
                }
                case ROAR -> {
                    if (d2 > 196) return false;
                    begin(i, 20, enraged ? 160 : 240);
                    sound(sl, SoundEvents.RAVAGER_ROAR, 2.0f, 0.5f);
                    return true;
                }
                case FIRE_RAIN -> {
                    if (d2 > 900) return false;
                    RandomSource r = boss.getRandom();
                    int n = enraged ? 14 : 8;
                    for (int j = 0; j < n; j++) {
                        SmallFireball f = new SmallFireball(sl, boss, new Vec3(0, -1, 0));
                        f.setPos(t.getX() + (r.nextDouble() - 0.5) * 9, t.getY() + 14, t.getZ() + (r.nextDouble() - 0.5) * 9);
                        sl.addFreshEntity(f);
                    }
                    sound(sl, SoundEvents.BLAZE_SHOOT, 2.0f, 0.4f);
                    cd[i] = enraged ? 120 : 200;
                    return true;
                }
                case FANG_RING -> {
                    if (d2 > 100) return false;
                    begin(i, 15, enraged ? 100 : 170);
                    sound(sl, SoundEvents.EVOKER_PREPARE_ATTACK, 2.0f, 0.6f);
                    return true;
                }
                case FANG_STORM -> {
                    if (d2 > 900) return false;
                    RandomSource r = boss.getRandom();
                    int n = enraged ? 22 : 14;
                    for (int j = 0; j < n; j++) {
                        double fx = t.getX() + (r.nextDouble() - 0.5) * 12.0;
                        double fz = t.getZ() + (r.nextDouble() - 0.5) * 12.0;
                        sl.addFreshEntity(new EvokerFangs(sl, fx, t.getY(), fz, r.nextFloat() * 6.2831f, r.nextInt(40), boss));
                    }
                    sound(sl, SoundEvents.EVOKER_CAST_SPELL, 2.0f, 0.7f);
                    cd[i] = enraged ? 150 : 230;
                    return true;
                }
                case CURSE -> {
                    if (d2 > 625 || !boss.hasLineOfSight(t)) return false;
                    begin(i, 25, enraged ? 220 : 340);
                    sound(sl, SoundEvents.EVOKER_PREPARE_WOLOLO, 2.0f, 0.7f);
                    return true;
                }
                case VEX_SWARM -> {
                    List<Vex> owned = sl.getEntitiesOfClass(Vex.class, boss.getBoundingBox().inflate(40), v -> v.getOwner() == boss && v.isAlive());
                    if (owned.size() >= 8) { cd[i] = 100; return false; }
                    RandomSource r = boss.getRandom();
                    int n = enraged ? 4 : 2;
                    for (int j = 0; j < n; j++) {
                        Vex v = EntityType.VEX.create(sl);
                        if (v == null) continue;
                        v.moveTo(boss.getX() + (r.nextDouble() - 0.5) * 4, boss.getY() + 1.5, boss.getZ() + (r.nextDouble() - 0.5) * 4, 0f, 0f);
                        v.finalizeSpawn(sl, sl.getCurrentDifficultyAt(v.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                        v.setOwner(boss);
                        v.setBoundOrigin(boss.blockPosition());
                        v.setLimitedLife(20 * (20 + r.nextInt(20)));
                        v.addTag("dt_minion");
                        for (EquipmentSlot slot : EquipmentSlot.values()) v.setDropChance(slot, 0f);
                        sl.addFreshEntityWithPassengers(v);
                    }
                    sl.sendParticles(ParticleTypes.WITCH, boss.getX(), boss.getY(1.0), boss.getZ(), 30, 1.0, 1.0, 1.0, 0.1);
                    sound(sl, SoundEvents.EVOKER_PREPARE_SUMMON, 2.0f, 0.8f);
                    cd[i] = enraged ? 200 : 320;
                    return true;
                }
                case BLINK -> {
                    if (d2 < 49) return false;
                    blink(sl, t);
                    cd[i] = enraged ? 70 : 110;
                    return true;
                }
                default -> { }
            }

            if (boss.tickCount % 4 != 0) return false;
            if (d2 < 16 || !boss.hasLineOfSight(t)) return false;
            Level level = boss.level();
            switch (moves[i]) {
                case WITHER_SKULLS -> {
                    fan(level, t, enraged ? 3 : 1, 0.2f, (l, b, d, idx) -> {
                        WitherSkull s = new WitherSkull(l, b, d);
                        if (idx == 0) s.setDangerous(true);
                        return s;
                    });
                    sound(sl, SoundEvents.WITHER_SHOOT, 2.0f, 1.0f);
                    cd[i] = enraged ? 45 : 90;
                    return true;
                }
                case ARROW_VOLLEY -> {
                    fan(level, t, enraged ? 4 : 2, 0.12f, (l, b, d, idx) -> {
                        double h = Math.sqrt(d.x * d.x + d.z * d.z);
                        return arrow(l, b, d.x, d.y + h * 0.2, d.z, 1.8f, 4f);
                    });
                    if (enraged) {
                        RandomSource r = boss.getRandom();
                        for (int j = 0; j < 6; j++) {
                            Entity a = arrow(level, boss, 0, -1, 0, 1.6f, 3f);
                            if (a == null) continue;
                            a.setPos(t.getX() + (r.nextDouble() - 0.5) * 8, t.getY() + 14, t.getZ() + (r.nextDouble() - 0.5) * 8);
                            level.addFreshEntity(a);
                        }
                    }
                    sound(sl, SoundEvents.SKELETON_SHOOT, 2.0f, 0.7f);
                    cd[i] = enraged ? 50 : 100;
                    return true;
                }
                case BLAZE_BARRAGE -> {
                    final int power = enraged ? 2 : 1;
                    fan(level, t, enraged ? 2 : 1, 0.2f, (l, b, d, idx) -> new LargeFireball(l, b, d, power));
                    sound(sl, SoundEvents.BLAZE_SHOOT, 2.0f, 0.7f);
                    cd[i] = enraged ? 50 : 90;
                    return true;
                }
                case FIRE_NOVA -> {
                    for (int j = 0; j < 12; j++) {
                        double a = j * Math.PI / 6.0;
                        SmallFireball f = new SmallFireball(level, boss, new Vec3(Math.cos(a), -0.1, Math.sin(a)));
                        f.setPos(boss.getX() + Math.cos(a) * 1.5, boss.getY(0.5), boss.getZ() + Math.sin(a) * 1.5);
                        level.addFreshEntity(f);
                    }
                    sound(sl, SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
                    cd[i] = enraged ? 100 : 170;
                    return true;
                }
                case DRAGON_BREATH -> {
                    fan(level, t, enraged ? 1 : 0, 0.25f, (l, b, d, idx) -> new DragonFireball(l, b, d));
                    sound(sl, SoundEvents.ENDER_DRAGON_SHOOT, 1.5f, 1.0f);
                    cd[i] = enraged ? 100 : 160;
                    return true;
                }
                case TRIDENT_FAN -> {
                    fan(level, t, enraged ? 2 : 1, 0.14f, (l, b, d, idx) -> {
                        ThrownTrident tr = new ThrownTrident(l, b, new ItemStack(Items.TRIDENT));
                        tr.pickup = AbstractArrow.Pickup.DISALLOWED;
                        double h = Math.sqrt(d.x * d.x + d.z * d.z);
                        tr.shoot(d.x, d.y + h * 0.15, d.z, 2.0f, 3f);
                        return tr;
                    });
                    sound(sl, SoundEvents.DROWNED_SHOOT, 2.0f, 0.8f);
                    cd[i] = enraged ? 60 : 110;
                    return true;
                }
                case FANG_WAVE -> {
                    double dx = t.getX() - boss.getX(), dz = t.getZ() - boss.getZ();
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len < 1.0E-3) return false;
                    double ux = dx / len, uz = dz / len;
                    float yaw = (float) Math.atan2(dz, dx);
                    for (int j = 1; j <= 14; j++) {
                        level.addFreshEntity(new EvokerFangs(level, boss.getX() + ux * j * 1.5, t.getY(),
                                boss.getZ() + uz * j * 1.5, yaw, j, boss));
                    }
                    level.addFreshEntity(new EvokerFangs(level, t.getX(), t.getY(), t.getZ(), yaw, 8, boss));
                    if (enraged) {
                        for (int j = 0; j < 8; j++) {
                            double a = j * Math.PI / 4.0;
                            level.addFreshEntity(new EvokerFangs(level, t.getX() + Math.cos(a) * 2.0, t.getY(),
                                    t.getZ() + Math.sin(a) * 2.0, (float) a, 10 + j % 3, boss));
                        }
                    }
                    sound(sl, SoundEvents.EVOKER_PREPARE_ATTACK, 2.0f, 0.8f);
                    cd[i] = enraged ? 80 : 140;
                    return true;
                }
                default -> { return false; }
            }
        }

        private void execute(int idx, ServerLevel sl, LivingEntity t, boolean enraged) {
            switch (moves[idx]) {
                case GROUND_SLAM -> slam(sl, enraged ? 11.0 : 8.0, enraged ? 20f : 14f);
                case SHIELD -> {
                    boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, enraged ? 120 : 100, 2, false, true));
                    sl.sendParticles(ParticleTypes.END_ROD, boss.getX(), boss.getY(1.0), boss.getZ(), 40, 1.0, 1.2, 1.0, 0.05);
                    sound(sl, SoundEvents.ENCHANTMENT_TABLE_USE, 2.0f, 0.6f);
                }
                case LIFE_DRAIN -> {
                    List<Player> victims = sl.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(10.0),
                            x -> x.isAlive() && !x.isCreative() && !x.isSpectator());
                    for (Player p : victims) {
                        if (p.hurt(sl.damageSources().mobAttack(boss), enraged ? 10f : 7f)) {
                            boss.heal((float) (boss.getMaxHealth() * 0.015));
                            sl.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY(1.0), p.getZ(), 20, 0.4, 0.7, 0.4, 0.08);
                            sl.sendParticles(ParticleTypes.HEART, boss.getX(), boss.getY(1.5), boss.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                        }
                    }
                    sound(sl, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 1.2f);
                }
                case PULL -> {
                    List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(20.0, 4.0, 20.0),
                            x -> x != boss && x.isAlive() && !(x instanceof Enemy));
                    for (LivingEntity e : list) {
                        Vec3 to = boss.position().subtract(e.position()).multiply(1, 0, 1);
                        if (to.lengthSqr() < 1.0E-4) continue;
                        to = to.normalize();
                        e.setDeltaMovement(e.getDeltaMovement().add(to.x * 1.5, 0.3, to.z * 1.5));
                        e.hurtMarked = true;
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                        sl.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY(1.0), e.getZ(), 15, 0.4, 0.6, 0.4, 0.1);
                    }
                    sound(sl, SoundEvents.TRIDENT_RIPTIDE_3, 2.0f, 0.7f);
                }
                case WAR_CRY -> {
                    int lvl = enraged ? 1 : 0;
                    List<Mob> allies = sl.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(40),
                            x -> x.isAlive() && (x == boss || x.getTags().contains("dt_minion")));
                    for (Mob m : allies) {
                        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, lvl));
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, lvl));
                        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getY(1.0), m.getZ(), 8, 0.4, 0.6, 0.4, 0.0);
                    }
                    sound(sl, SoundEvents.RAVAGER_ROAR, 2.0f, 0.9f);
                }
                case WITHER_NOVA -> {
                    int n = enraged ? 16 : 10;
                    for (int j = 0; j < n; j++) {
                        double a = j * Math.PI * 2.0 / n;
                        WitherSkull ws = new WitherSkull(sl, boss, new Vec3(Math.cos(a), -0.05, Math.sin(a)));
                        ws.setPos(boss.getX() + Math.cos(a) * 1.5, boss.getY(0.6), boss.getZ() + Math.sin(a) * 1.5);
                        sl.addFreshEntity(ws);
                    }
                    sound(sl, SoundEvents.WITHER_SHOOT, 2.0f, 0.6f);
                }
                case CHARGE -> {
                    Vec3 d = new Vec3(t.getX() - boss.getX(), 0, t.getZ() - boss.getZ());
                    double len = d.length();
                    if (len < 1.0E-3) return;
                    chargeDir = d.scale((enraged ? 1.0 : 0.85) / len);
                    chargeTicks = 14;
                }
                case ROAR -> {
                    double rad = enraged ? 14.0 : 11.0;
                    List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(rad, 3.0, rad),
                            x -> x != boss && x.isAlive() && !(x instanceof Enemy));
                    for (LivingEntity e : list) {
                        if (e.distanceToSqr(boss) > rad * rad) continue;
                        e.hurt(sl.damageSources().mobAttack(boss), 8f);
                        Vec3 away = e.position().subtract(boss.position()).multiply(1, 0, 1);
                        if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                        away = away.normalize();
                        e.setDeltaMovement(e.getDeltaMovement().add(away.x * 1.6, 0.5, away.z * 1.6));
                        e.hurtMarked = true;
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, enraged ? 120 : 80, 1));
                    }
                    for (int k = 0; k < 24; k++) {
                        double a = k * Math.PI / 12.0;
                        sl.sendParticles(ParticleTypes.POOF, boss.getX() + Math.cos(a) * rad * 0.6, boss.getY() + 0.5, boss.getZ() + Math.sin(a) * rad * 0.6, 2, 0.2, 0.2, 0.2, 0.02);
                    }
                    sound(sl, SoundEvents.RAVAGER_STUNNED, 2.0f, 0.5f);
                }
                case FANG_RING -> {
                    double[] radii = {2.5, 5.0, 7.5, 10.0};
                    int[] counts = {8, 12, 16, 20};
                    int rings = enraged ? 4 : 3;
                    for (int k = 0; k < rings; k++) {
                        for (int j = 0; j < counts[k]; j++) {
                            double a = j * Math.PI * 2.0 / counts[k];
                            sl.addFreshEntity(new EvokerFangs(sl, boss.getX() + Math.cos(a) * radii[k], boss.getY(),
                                    boss.getZ() + Math.sin(a) * radii[k], (float) a, k * 6, boss));
                        }
                    }
                    sound(sl, SoundEvents.EVOKER_CAST_SPELL, 2.0f, 0.6f);
                }
                case CURSE -> {
                    List<Player> victims = sl.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(24.0),
                            p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
                    for (Player p : victims) {
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, enraged ? 160 : 100, 1));
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, enraged ? 300 : 200, 1));
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, enraged ? 140 : 80, 0));
                        sl.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY(1.0), p.getZ(), 25, 0.5, 0.8, 0.5, 0.05);
                    }
                    sound(sl, SoundEvents.EVOKER_CAST_SPELL, 2.0f, 0.5f);
                }
                case LEAP -> {
                    Vec3 d = new Vec3(t.getX() - boss.getX(), 0, t.getZ() - boss.getZ());
                    double len = d.length();
                    if (len < 1.0E-3) return;
                    double speed = Math.min(1.6, 0.35 + len * 0.07);
                    d = d.scale(speed / len);
                    boss.setDeltaMovement(d.x, 0.85, d.z);
                    leapTicks = 40;
                }
                default -> { }
            }
        }

        private void slam(ServerLevel sl, double radius, float dmg) {
            Vec3 c = boss.position();
            List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class,
                    boss.getBoundingBox().inflate(radius, 2.0, radius),
                    e -> e != boss && e.isAlive() && !(e instanceof Enemy));
            for (LivingEntity e : list) {
                if (e.distanceToSqr(boss) > radius * radius) continue;
                e.hurt(sl.damageSources().mobAttack(boss), dmg);
                Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                away = away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.x * 1.2, 0.7, away.z * 1.2));
                e.hurtMarked = true;
            }
            for (int i = 0; i < 36; i++) {
                double a = i * Math.PI / 18.0;
                for (double r = 2; r <= radius; r += 2) {
                    sl.sendParticles(ParticleTypes.CLOUD, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
                }
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 6, radius * 0.3, 0.2, radius * 0.3, 0.0);
            sound(sl, SoundEvents.IRON_GOLEM_ATTACK, 2.0f, 0.5f);
        }

        private void blink(ServerLevel sl, LivingEntity t) {
            RandomSource r = boss.getRandom();
            for (int a = 0; a < 10; a++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double nx = t.getX() + Math.cos(ang) * 2.5, nz = t.getZ() + Math.sin(ang) * 2.5;
                AABB bb = boss.getBoundingBox().move(nx - boss.getX(), t.getY() - boss.getY(), nz - boss.getZ());
                if (!sl.noCollision(boss, bb)) continue;
                sl.sendParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY(1.0), boss.getZ(), 40, 0.5, 1.0, 0.5, 0.3);
                boss.moveTo(nx, t.getY(), nz, boss.getYRot(), boss.getXRot());
                boss.getNavigation().stop();
                boss.getLookControl().setLookAt(t, 30f, 30f);
                sl.sendParticles(ParticleTypes.PORTAL, nx, boss.getY(1.0), nz, 40, 0.5, 1.0, 0.5, 0.3);
                sound(sl, SoundEvents.ENDERMAN_TELEPORT, 2.0f, 0.6f);
                break;
            }
        }

        private void fan(Level level, LivingEntity t, int n, float step, Proj p) {
            Vec3 base = new Vec3(t.getX() - boss.getX(), t.getY(0.5) - boss.getY(0.5), t.getZ() - boss.getZ());
            for (int i = -n; i <= n; i++) {
                Entity e = p.make(level, boss, base.yRot(i * step), i);
                if (e == null) continue;
                e.setPos(boss.getX(), boss.getY(0.6), boss.getZ());
                level.addFreshEntity(e);
            }
        }

        @Nullable
        private static Entity arrow(Level level, Mob boss, double dx, double dy, double dz, float speed, float inaccuracy) {
            Arrow a = EntityType.ARROW.create(level);
            if (a == null) return null;
            a.setOwner(boss);
            a.pickup = AbstractArrow.Pickup.DISALLOWED;
            a.setBaseDamage(3.5);
            a.setCritArrow(true);
            a.shoot(dx, dy, dz, speed, inaccuracy);
            return a;
        }
    }
}
