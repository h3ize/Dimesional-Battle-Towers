package com.example.dimtowers;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public enum Theme {
    OVERWORLD("overworld", "Warden of the Deep",
            Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE, Blocks.CHISELED_TUFF_BRICKS,
            Blocks.POLISHED_DEEPSLATE, Blocks.TUFF_BRICKS, Blocks.POLISHED_DEEPSLATE, Blocks.OXIDIZED_CUT_COPPER,
            Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILE_WALL,
            Blocks.COPPER_BULB.defaultBlockState().setValue(BlockStateProperties.LIT, true)),
    NETHER("nether", "Lord of Ash",
            Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS,
            Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRYING_OBSIDIAN,
            Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICK_WALL,
            Blocks.SHROOMLIGHT.defaultBlockState()),
    END("end", "Void Sentinel",
            Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK,
            Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
            Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE_BRICK_WALL,
            Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());

    public final String id;
    public final Component bossName;
    public final BlockState wall, wallAlt, pillar, trim, floor, floorAlt, plinth, accent, light;
    public final Block stairs, slab, wallPiece;

    Theme(String id, String boss, Block wall, Block wallAlt, Block pillar, Block trim, Block floor, Block floorAlt,
          Block plinth, Block accent, Block stairs, Block slab, Block wallPiece, BlockState light) {
        this.id = id;
        this.bossName = Component.literal(boss);
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
    }

    public static Theme byName(String n) {
        for (Theme t : values()) if (t.id.equals(n)) return t;
        return OVERWORLD;
    }
}
