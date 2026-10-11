package com.example.dimtowers;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DimensionalTowers.MOD_ID)
public class DimensionalTowers {
    public static final String MOD_ID = "dimtowers";

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);
    public static final DeferredHolder<Feature<?>, TowerFeature> TOWER =
            FEATURES.register("tower", () -> new TowerFeature(TowerConfig.CODEC));

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<TowerBoss>> TOWER_BOSS =
            ENTITIES.register("tower_boss", () -> EntityType.Builder.<TowerBoss>of(TowerBoss::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F).fireImmune().clientTrackingRange(10).build("tower_boss"));

    public DimensionalTowers(IEventBus modBus) {
        FEATURES.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(this::attributes);
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(TOWER_BOSS.get(), TowerBoss.createBossAttributes().build());
    }
}
