package com.example.dimtowers;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

public class TowerFeature extends Feature<TowerConfig> {
    static final int H = 8;       // floor-to-floor height
    static final int FLOORS = 4;  // rooms below the boss arena

    public TowerFeature(Codec<TowerConfig> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<TowerConfig> ctx) {
        WorldGenLevel level = ctx.level();
        Theme theme = Theme.byName(ctx.config().theme());
        int cx = ctx.origin().getX(), cz = ctx.origin().getZ();
        int s0 = findGround(level, theme, cx, cz);
        if (s0 == Integer.MIN_VALUE) return false;
        new Builder(level, ctx.random(), theme, cx, cz, s0).build();
        return true;
    }

    private static int findGround(WorldGenLevel level, Theme theme, int cx, int cz) {
        if (theme == Theme.NETHER) {
            for (int y = 74; y >= 36; y--) {
                BlockPos p = new BlockPos(cx, y, cz);
                BlockState st = level.getBlockState(p);
                if (st.isAir() || !st.getFluidState().isEmpty() || !st.isFaceSturdy(level, p, Direction.UP)) continue;
                boolean clear = true;
                for (int dy = 1; dy <= 10 && clear; dy++) clear = level.getBlockState(p.above(dy)).isAir();
                if (clear) return y;
            }
            return Integer.MIN_VALUE;
        }
        if (theme == Theme.END) {
            int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
            return h > 10 ? h - 1 : Integer.MIN_VALUE;
        }
        int[] off = {0, 0, 6, 6, -6, 6, 6, -6, -6, -6};
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
        for (int i = 0; i < off.length; i += 2) {
            int x = cx + off[i], z = cz + off[i + 1];
            int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            if (!level.getBlockState(new BlockPos(x, h - 1, z)).getFluidState().isEmpty()) return Integer.MIN_VALUE;
            max = Math.max(max, h);
            min = Math.min(min, h);
        }
        if (max - min > 6) return Integer.MIN_VALUE;
        return max - 1;
    }

    private static final List<String> COMMON_LOOT = List.of(
            "chests/desert_pyramid", "chests/jungle_temple", "chests/simple_dungeon", "chests/abandoned_mineshaft",
            "chests/stronghold_corridor", "chests/stronghold_crossing", "chests/stronghold_library",
            "chests/village/village_weaponsmith", "chests/village/village_armorer", "chests/village/village_toolsmith",
            "chests/woodland_mansion", "chests/pillager_outpost", "chests/igloo_chest", "chests/shipwreck_treasure",
            "chests/underwater_ruin_big", "chests/buried_treasure", "chests/trial_chambers/reward");
    private static final List<String> OVERWORLD_LOOT = List.of("chests/ancient_city", "chests/ancient_city");
    private static final List<String> NETHER_LOOT = List.of("chests/bastion_treasure", "chests/bastion_other",
            "chests/bastion_bridge", "chests/bastion_hoglin_stable", "chests/nether_bridge", "chests/ruined_portal");
    private static final List<String> END_LOOT = List.of("chests/ancient_city", "chests/woodland_mansion",
            "chests/bastion_treasure", "chests/stronghold_library");

    private static final class Builder {
        final WorldGenLevel level;
        final RandomSource rnd;
        final Theme t;
        final int cx, cz, s0;
        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        Builder(WorldGenLevel level, RandomSource rnd, Theme t, int cx, int cz, int s0) {
            this.level = level; this.rnd = rnd; this.t = t; this.cx = cx; this.cz = cz; this.s0 = s0;
        }

        int s(int k) { return s0 + k * H; }
        void set(int dx, int y, int dz, BlockState st) { level.setBlock(m.set(cx + dx, y, cz + dz), st, 2); }
        void clr(int dx, int y, int dz) { set(dx, y, dz, air); }
        BlockState at(int dx, int y, int dz) { return level.getBlockState(new BlockPos(cx + dx, y, cz + dz)); }
        static int abs(int v) { return Math.abs(v); }

        ResourceKey<LootTable> loot() {
            java.util.ArrayList<String> pool = new java.util.ArrayList<>(COMMON_LOOT);
            pool.addAll(t == Theme.OVERWORLD ? OVERWORLD_LOOT : t == Theme.NETHER ? NETHER_LOOT : END_LOOT);
            return key(pool.get(rnd.nextInt(pool.size())));
        }
        static ResourceKey<LootTable> key(String p) {
            return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace(p));
        }

        void doubleChest(int dx, int y, int dz, Direction f) {
            BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f).setValue(ChestBlock.TYPE, ChestType.LEFT);
            Direction toPartner = ChestBlock.getConnectedDirection(left);
            BlockPos p1 = new BlockPos(cx + dx, y, cz + dz);
            BlockPos p2 = p1.relative(toPartner);
            level.setBlock(p1, left, 2);
            level.setBlock(p2, left.setValue(ChestBlock.TYPE, ChestType.RIGHT), 2);
            RandomizableContainer.setBlockEntityLootTable(level, rnd, p1, loot());
            RandomizableContainer.setBlockEntityLootTable(level, rnd, p2, loot());
        }

        boolean pilaster(int dx, int dz) {
            return (abs(dx) == 4 && abs(dz) == 8) || (abs(dz) == 4 && abs(dx) == 8);
        }

        void build() {
            final int top = Math.min(s(FLOORS) + 14, level.getMaxBuildHeight() - 2);
            final int sTop = s(FLOORS);
            final BlockState slabTop = t.slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            final BlockState slabBottom = t.slab.defaultBlockState();

            // 1. clear the site
            for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++)
                for (int y = s0; y <= top; y++) clr(dx, y, dz);

            // 2. inverted-pyramid foundation
            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                int depth = 40 - 4 * Math.max(abs(dx), abs(dz));
                for (int i = 0; i <= depth; i++) {
                    int y = s0 - i;
                    BlockState st = at(dx, y, dz);
                    if (i > 0 && !(st.isAir() || !st.getFluidState().isEmpty() || st.canBeReplaced())) break;
                    set(dx, y, dz, t.plinth);
                }
            }

            // 3. floors (S0..S4, S4 is the boss arena floor)
            for (int k = 0; k <= FLOORS; k++)
                for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++)
                    set(dx, s(k), dz, ((dx + dz) & 1) == 0 ? t.floor : t.floorAlt);

            // 4. double-thick outer walls + parapet
            for (int y = s0 + 1; y <= sTop + 3; y++)
                for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                    if (Math.max(abs(dx), abs(dz)) < 6) continue;
                    set(dx, y, dz, rnd.nextInt(7) == 0 ? t.wallAlt : t.wall);
                }
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                int r = Math.max(abs(dx), abs(dz));
                if (r == 7) {
                    for (int k = 1; k <= FLOORS; k++) set(dx, s(k), dz, t.trim);
                    if (((dx + dz) & 1) == 0) set(dx, sTop + 4, dz, t.wall);   // battlements
                }
            }

            // 5. corner buttresses with tall spires
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
                for (int ax = 7; ax <= 8; ax++) for (int az = 7; az <= 8; az++) {
                    if (ax == 7 && az == 7) continue;
                    boolean tip = ax == 8 && az == 8;
                    int h = tip ? sTop + 9 : sTop + 6;
                    for (int y = s0 + 1; y <= h; y++) set(sx * ax, y, sz * az, tip ? t.pillar : t.wall);
                    if (tip) {
                        set(sx * ax, sTop + 10, sz * az, t.wallPiece.defaultBlockState());
                        set(sx * ax, sTop + 11, sz * az, t.wallPiece.defaultBlockState());
                        set(sx * ax, sTop + 12, sz * az, t.light);
                    }
                }

            // 6. pilasters, skirt and ledges
            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                if (pilaster(dx, dz)) {
                    for (int y = s0 + 1; y <= sTop + 3; y++) set(dx, y, dz, t.pillar);
                    continue;
                }
                if (Math.max(abs(dx), abs(dz)) != 8 || Math.min(abs(dx), abs(dz)) > 5) continue;
                if (!(dz == 8 && abs(dx) <= 2)) set(dx, s0 + 1, dz, slabBottom);
                for (int k = 1; k <= FLOORS; k++) set(dx, s(k), dz, slabTop);
            }

            // 7. arrow-slit windows
            for (int k = 0; k < FLOORS; k++) for (int y = s(k) + 3; y <= s(k) + 5; y++)
                for (int tt = -2; tt <= 2; tt += 4) for (int r = 6; r <= 7; r++) {
                    clr(tt, y, r); clr(tt, y, -r); clr(r, y, tt); clr(-r, y, tt);
                }

            // 8. grand entrance (south side)
            for (int x = -1; x <= 1; x++) for (int y = s0 + 1; y <= s0 + 4; y++) { clr(x, y, 6); clr(x, y, 7); }
            for (int x = -2; x <= 2; x++) { set(x, s0 + 5, 6, t.trim); set(x, s0 + 5, 7, t.trim); }
            for (int sx = -2; sx <= 2; sx += 4) for (int y = s0 + 1; y <= s0 + 4; y++) {
                set(sx, y, 6, t.pillar); set(sx, y, 7, t.pillar);
            }
            set(-2, s0 + 4, 7, t.light); set(2, s0 + 4, 7, t.light);

            // 9. interior pillars with ceiling lights
            for (int k = 0; k < FLOORS; k++) for (int sx = -3; sx <= 3; sx += 6) for (int sz = -3; sz <= 3; sz += 6) {
                for (int y = s(k) + 1; y <= s(k) + H - 1; y++) set(sx, y, sz, t.pillar);
                set(sx, s(k + 1), sz, t.light);
            }

            // 10. staircases (alternate walls) so every floor connects upward
            for (int k = 0; k < FLOORS; k++) {
                boolean even = k % 2 == 0;
                int zRow = even ? -5 : 5;
                Direction dir = even ? Direction.EAST : Direction.WEST;
                int startX = even ? -5 : 5, step = even ? 1 : -1;
                BlockState stair = t.stairs.defaultBlockState().setValue(StairBlock.FACING, dir);
                for (int i = 0; i < H; i++) {
                    int x = startX + step * i;
                    int y = s(k) + 1 + i;
                    for (int yy = s(k) + 1; yy < y; yy++) set(x, yy, zRow, t.floorAlt);
                    set(x, y, zRow, stair);
                    if (i < H - 1) clr(x, s(k) + H, zRow);   // hole in the ceiling above the stairs
                }
            }

            // 11. loot: two double chests on every floor (NO end city chests down here)
            for (int k = 0; k < FLOORS; k++) {
                doubleChest(5, s(k) + 1, 0, Direction.WEST);
                doubleChest(-5, s(k) + 1, 0, Direction.EAST);
            }

            // 12. boss arena on the roof
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(dx, sTop, dz, t.accent);

            // vault alcove (north wall)
            for (int y = sTop + 1; y <= sTop + 3; y++) { set(-1, y, -5, t.pillar); set(1, y, -5, t.pillar); }
            set(0, sTop + 2, -5, t.trim);
            set(0, sTop + 3, -5, t.trim);
            BlockPos vp = new BlockPos(cx, sTop + 1, cz - 5);
            level.setBlock(vp, Blocks.VAULT.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 2);
            BlockEntity be = level.getBlockEntity(vp);
            if (be != null) {
                try {
                    CompoundTag tag = TagParser.parseTag("{config:{key_item:{id:\"minecraft:ominous_trial_key\",count:1},"
                            + "loot_table:\"minecraft:chests/trial_chambers/reward_ominous\"}}");
                    be.loadWithComponents(tag, level.registryAccess());
                    be.setChanged();
                } catch (Exception ignored) { }
            }

            // 4 end city chests, only up here
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos cp = new BlockPos(cx + sx * 4, sTop + 1, cz + sz * 4);
                level.setBlock(cp, Blocks.CHEST.defaultBlockState()
                        .setValue(ChestBlock.FACING, sx > 0 ? Direction.WEST : Direction.EAST), 2);
                RandomizableContainer.setBlockEntityLootTable(level, rnd, cp, key("chests/end_city_treasure"));
            }

            // 13. the boss
            TowerBoss boss = DimensionalTowers.TOWER_BOSS.get().create(level.getLevel());
            if (boss != null) {
                boss.moveTo(cx + 0.5, sTop + 1, cz + 0.5, rnd.nextFloat() * 360f, 0f);
                boss.setup(t.bossName);
                level.addFreshEntity(boss);
            }
        }
    }
}
