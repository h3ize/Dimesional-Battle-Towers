package com.example.dimtowers;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DimensionalTowers.MOD_ID)
public class DimensionalTowers {
    public static final String MOD_ID = "dimtowers";

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<TowerStructure>> TOWER_STRUCTURE =
            STRUCTURE_TYPES.register("tower", () -> (StructureType<TowerStructure>) () -> TowerStructure.CODEC);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MOD_ID);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TOWER_PIECE =
            PIECE_TYPES.register("tower_piece", () -> (StructurePieceType) TowerPiece::new);

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<TowerBoss>> TOWER_BOSS =
            ENTITIES.register("tower_boss", () -> EntityType.Builder.<TowerBoss>of(TowerBoss::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F).fireImmune().clientTrackingRange(10).build("tower_boss"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerZombie>> TOWER_ZOMBIE =
            ENTITIES.register("tower_zombie", () -> EntityType.Builder.<TowerZombie>of(TowerZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10).build("tower_zombie"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerSkeleton>> TOWER_SKELETON =
            ENTITIES.register("tower_skeleton", () -> EntityType.Builder.<TowerSkeleton>of(TowerSkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).fireImmune().clientTrackingRange(10).build("tower_skeleton"));

    public DimensionalTowers(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        PIECE_TYPES.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(this::attributes);
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(TOWER_BOSS.get(), TowerBoss.createBossAttributes().build());
        e.put(TOWER_ZOMBIE.get(), TowerZombie.createBossAttributes().build());
        e.put(TOWER_SKELETON.get(), TowerSkeleton.createBossAttributes().build());
    }
}