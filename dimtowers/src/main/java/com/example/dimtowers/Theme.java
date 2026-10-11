package com.example.dimtowers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public enum Theme {
    OVERWORLD("overworld",
            Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE, Blocks.CHISELED_TUFF_BRICKS,
            Blocks.POLISHED_DEEPSLATE, Blocks.TUFF_BRICKS, Blocks.POLISHED_DEEPSLATE, Blocks.OXIDIZED_CUT_COPPER,
            Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILE_WALL,
            Blocks.COPPER_BULB.defaultBlockState().setValue(BlockStateProperties.LIT, true),
            Blocks.TINTED_GLASS, Blocks.SOUL_LANTERN, melee(),
            List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER,
                    EntityType.CREEPER, EntityType.WITCH, EntityType.BOGGED)),
    NETHER("nether",
            Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS,
            Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRYING_OBSIDIAN,
            Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICK_WALL,
            Blocks.SHROOMLIGHT.defaultBlockState(),
            Blocks.RED_STAINED_GLASS, Blocks.LANTERN, melee(),
            List.of(EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.MAGMA_CUBE,
                    EntityType.ZOMBIFIED_PIGLIN, EntityType.SKELETON)),
    END("end",
            Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK,
            Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
            Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE_BRICK_WALL,
            Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(),
            Blocks.PURPLE_STAINED_GLASS, Blocks.SOUL_LANTERN, melee(),
            List.of(EntityType.ENDERMAN, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.SHULKER)),
    PIGLIN("piglin",
            Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.GILDED_BLACKSTONE, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE, Blocks.GOLD_BLOCK,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
            Blocks.SHROOMLIGHT.defaultBlockState(),
            Blocks.ORANGE_STAINED_GLASS, Blocks.LANTERN, new String[]{"dimtowers:tower_piglin"},
            List.of(EntityType.PIGLIN_BRUTE, EntityType.PIGLIN, EntityType.HOGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.BLAZE)),
    FROZEN("frozen",
            Blocks.QUARTZ_BRICKS, Blocks.PACKED_ICE, Blocks.QUARTZ_PILLAR, Blocks.CHISELED_QUARTZ_BLOCK,
            Blocks.SMOOTH_QUARTZ, Blocks.PACKED_ICE, Blocks.STONE_BRICKS, Blocks.BLUE_ICE,
            Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_SLAB, Blocks.DIORITE_WALL,
            Blocks.SEA_LANTERN.defaultBlockState(),
            Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.SOUL_LANTERN, melee(),
            List.of(EntityType.STRAY, EntityType.STRAY, EntityType.SKELETON, EntityType.ZOMBIE, EntityType.WITCH)),
    DESERT("desert",
            Blocks.CUT_SANDSTONE, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE,
            Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE, Blocks.GOLD_BLOCK,
            Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE_SLAB, Blocks.SANDSTONE_WALL,
            Blocks.GLOWSTONE.defaultBlockState(),
            Blocks.YELLOW_STAINED_GLASS, Blocks.LANTERN, melee(),
            List.of(EntityType.HUSK, EntityType.HUSK, EntityType.SKELETON, EntityType.SPIDER,
                    EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.WITCH));

    public final String id;
    public final BlockState wall, wallAlt, pillar, trim, floor, floorAlt, plinth, accent, light, glass,
            lanternHanging, lanternStanding;
    public final Block stairs, slab, wallPiece;
    /** Entity type ids the boss Trial Spawner can pick from. */
    public final String[] bosses;
    /** Entity types the per-floor monster spawners can pick from. */
    public final List<EntityType<?>> mobs;

    Theme(String id, Block wall, Block wallAlt, Block pillar, Block trim, Block floor, Block floorAlt,
          Block plinth, Block accent, Block stairs, Block slab, Block wallPiece, BlockState light,
          Block glass, Block lantern, String[] bosses, List<EntityType<?>> mobs) {
        this.id = id;
        this.wall = wall.defaultBlockState();
        this.wallAlt = wallAlt.defaultBlockState();
        this.pillar = pillar.defaultBlockState();
        this.trim = trim.defaultBlockState();
        this.floor = floor.defaultBlockState();
        this.floorAlt = floorAlt.defaultBlockState();
        this.plinth = plinth.defaultBlockState();
        this.accent = accent.defaultBlockState();
        this.stairs = stairs;
        this.slab = slab;
        this.wallPiece = wallPiece;
        this.light = light;
        this.glass = glass.defaultBlockState();
        this.lanternStanding = lantern.defaultBlockState();
        this.lanternHanging = lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
        this.bosses = bosses;
        this.mobs = mobs;
    }

    private static String[] melee() {
        return new String[]{"dimtowers:tower_zombie", "dimtowers:tower_skeleton", "dimtowers:tower_boss"};
    }

    public static Theme byName(String n) {
        for (Theme t : values()) if (t.id.equals(n)) return t;
        return OVERWORLD;
    }
}
