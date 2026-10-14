package com.example.dimtowers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

@Mod(DimensionalBattleTowers.MOD_ID)
public class DimensionalBattleTowers {
    public static final String MOD_ID = "dimtowers";

    public static final TagKey<Structure> TOWERS =
            TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "towers"));

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<TowerStructure>> TOWER_STRUCTURE =
            STRUCTURE_TYPES.register("tower", () -> (StructureType<TowerStructure>) () -> TowerStructure.CODEC);

    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, MOD_ID);
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<MaxEnchantFunction>> MAX_ENCHANT =
            LOOT_FUNCTIONS.register("max_enchant", () -> new LootItemFunctionType<>(MaxEnchantFunction.CODEC));

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
    public static final DeferredHolder<EntityType<?>, EntityType<TowerPiglin>> TOWER_PIGLIN =
            ENTITIES.register("tower_piglin", () -> EntityType.Builder.<TowerPiglin>of(TowerPiglin::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10).build("tower_piglin"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerVindicator>> TOWER_VINDICATOR =
            ENTITIES.register("tower_vindicator", () -> EntityType.Builder.<TowerVindicator>of(TowerVindicator::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10).build("tower_vindicator"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerBlaze>> TOWER_BLAZE =
            ENTITIES.register("tower_blaze", () -> EntityType.Builder.<TowerBlaze>of(TowerBlaze::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(10).build("tower_blaze"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerGuardian>> TOWER_GUARDIAN =
            ENTITIES.register("tower_guardian", () -> EntityType.Builder.<TowerGuardian>of(TowerGuardian::new, MobCategory.MONSTER)
                    .sized(0.85F, 0.85F).fireImmune().clientTrackingRange(10).build("tower_guardian"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerEvoker>> TOWER_EVOKER =
            ENTITIES.register("tower_evoker", () -> EntityType.Builder.<TowerEvoker>of(TowerEvoker::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10).build("tower_evoker"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerRavager>> TOWER_RAVAGER =
            ENTITIES.register("tower_ravager", () -> EntityType.Builder.<TowerRavager>of(TowerRavager::new, MobCategory.MONSTER)
                    .sized(1.95F, 2.2F).fireImmune().clientTrackingRange(10).build("tower_ravager"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerEnder>> TOWER_ENDER =
            ENTITIES.register("tower_ender", () -> EntityType.Builder.<TowerEnder>of(TowerEnder::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.9F).fireImmune().clientTrackingRange(10).build("tower_ender"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerSlime>> TOWER_SLIME =
            ENTITIES.register("tower_slime", () -> EntityType.Builder.<TowerSlime>of(TowerSlime::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F).fireImmune().clientTrackingRange(10).build("tower_slime"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerDrowned>> TOWER_DROWNED =
            ENTITIES.register("tower_drowned", () -> EntityType.Builder.<TowerDrowned>of(TowerDrowned::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10).build("tower_drowned"));

    public DimensionalBattleTowers(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        PIECE_TYPES.register(modBus);
        LOOT_FUNCTIONS.register(modBus);
        ENTITIES.register(modBus);
        OminousKey.ITEMS.register(modBus);
        modBus.addListener(this::attributes);
        modBus.addListener(this::creativeTabs);
        NeoForge.EVENT_BUS.addListener(BossKit::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(FloorMobs::onJoin);
        NeoForge.EVENT_BUS.addListener(FloorMobs::onTick);
        NeoForge.EVENT_BUS.addListener(FloorMobs::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(FloorMobs::onLevelTick);
        NeoForge.EVENT_BUS.addListener(this::villagerTrades);
        NeoForge.EVENT_BUS.addListener(OminousKey::onUse);
        NeoForge.EVENT_BUS.addListener(OminousKey::onServerTick);
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(OminousKey.KEY.get());
    }

    private void villagerTrades(VillagerTradesEvent e) {
        if (e.getType() != VillagerProfession.CARTOGRAPHER) return;
        e.getTrades().get(3).add(new StructureMapTrade(
                20, TOWERS, "filled_map.dimtowers.tower", MapDecorationTypes.RED_X, 3, 15));
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(TOWER_BOSS.get(), TowerBoss.createBossAttributes().build());
        e.put(TOWER_ZOMBIE.get(), TowerZombie.createBossAttributes().build());
        e.put(TOWER_SKELETON.get(), TowerSkeleton.createBossAttributes().build());
        e.put(TOWER_PIGLIN.get(), TowerPiglin.createBossAttributes().build());
        e.put(TOWER_VINDICATOR.get(), TowerVindicator.createBossAttributes().build());
        e.put(TOWER_BLAZE.get(), TowerBlaze.createBossAttributes().build());
        e.put(TOWER_GUARDIAN.get(), TowerGuardian.createBossAttributes().build());
        e.put(TOWER_EVOKER.get(), TowerEvoker.createBossAttributes().build());
        e.put(TOWER_RAVAGER.get(), TowerRavager.createBossAttributes().build());
        e.put(TOWER_ENDER.get(), TowerEnder.createBossAttributes().build());
        e.put(TOWER_SLIME.get(), TowerSlime.createBossAttributes().build());
        e.put(TOWER_DROWNED.get(), TowerDrowned.createBossAttributes().build());
    }

    private static final class StructureMapTrade implements VillagerTrades.ItemListing {
        private final int emeraldCost;
        private final TagKey<Structure> destination;
        private final String displayName;
        private final Holder<MapDecorationType> destinationType;
        private final int maxUses;
        private final int villagerXp;

        StructureMapTrade(int emeraldCost, TagKey<Structure> destination, String displayName,
                          Holder<MapDecorationType> destinationType, int maxUses, int villagerXp) {
            this.emeraldCost = emeraldCost;
            this.destination = destination;
            this.displayName = displayName;
            this.destinationType = destinationType;
            this.maxUses = maxUses;
            this.villagerXp = villagerXp;
        }

        @Override
        @Nullable
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            if (!(trader.level() instanceof ServerLevel serverLevel)) {
                return null;
            }
            BlockPos structurePos = serverLevel.findNearestMapStructure(this.destination, trader.blockPosition(), 100, true);
            if (structurePos == null) {
                return null;
            }
            ItemStack map = MapItem.create(serverLevel, structurePos.getX(), structurePos.getZ(), (byte) 2, true, true);
            MapItem.renderBiomePreviewMap(serverLevel, map);
            MapItemSavedData.addTargetDecoration(map, structurePos, "+", this.destinationType);
            map.set(DataComponents.ITEM_NAME, Component.translatable(this.displayName));
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, this.emeraldCost),
                    java.util.Optional.of(new ItemCost(Items.COMPASS)),
                    map,
                    this.maxUses,
                    this.villagerXp,
                    0.2F);
        }
    }
}
