package com.example.dimtowers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

public class TowerPiece extends StructurePiece {
    static final int H = 8;
    static final int FLOORS = 4;

    private final String themeId;
    private final int cx, cz, s0;

    public TowerPiece(String themeId, int cx, int cz, int s0) {
        super(DimensionalTowers.TOWER_PIECE.get(), 0,
                new BoundingBox(cx - 9, s0 - 44, cz - 9, cx + 9, s0 + FLOORS * H + 14, cz + 9));
        this.themeId = themeId; this.cx = cx; this.cz = cz; this.s0 = s0;
    }

    public TowerPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(DimensionalTowers.TOWER_PIECE.get(), tag);
        this.themeId = tag.getString("Theme");
        this.cx = tag.getInt("CX");
        this.cz = tag.getInt("CZ");
        this.s0 = tag.getInt("S0");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Theme", themeId);
        tag.putInt("CX", cx);
        tag.putInt("CZ", cz);
        tag.putInt("S0", s0);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        new Builder(level, rnd, box, Theme.byName(themeId)).build();
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

    private final class Builder {
        final WorldGenLevel level;
        final RandomSource rnd;
        final BoundingBox box;
        final Theme t;
        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        Builder(WorldGenLevel level, RandomSource rnd, BoundingBox box, Theme t) {
            this.level = level; this.rnd = rnd; this.box = box; this.t = t;
        }

        int s(int k) { return s0 + k * H; }
        static int abs(int v) { return Math.abs(v); }

        void set(int dx, int y, int dz, BlockState st) {
            int x = cx + dx, z = cz + dz;
            if (!box.isInside(x, y, z)) return;
            level.setBlock(m.set(x, y, z), st, 2);
        }
        void clr(int dx, int y, int dz) { set(dx, y, dz, air); }
        BlockState at(int dx, int y, int dz) { return level.getBlockState(new BlockPos(cx + dx, y, cz + dz)); }
        boolean colInside(int dx, int dz) { return box.isInside(cx + dx, box.minY(), cz + dz); }

        ResourceKey<LootTable> loot() {
            ArrayList<String> pool = new ArrayList<>(COMMON_LOOT);
            pool.addAll(t == Theme.OVERWORLD ? OVERWORLD_LOOT : t == Theme.NETHER ? NETHER_LOOT : END_LOOT);
            return key(pool.get(rnd.nextInt(pool.size())));
        }
        ResourceKey<LootTable> key(String p) {
            return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace(p));
        }

        void doubleChest(int dx, int y, int dz, Direction f) {
            BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f).setValue(ChestBlock.TYPE, ChestType.LEFT);
            Direction toPartner = ChestBlock.getConnectedDirection(left);
            BlockPos p1 = new BlockPos(cx + dx, y, cz + dz);
            BlockPos p2 = p1.relative(toPartner);
            if (box.isInside(p1)) {
                level.setBlock(p1, left, 2);
                RandomizableContainer.setBlockEntityLootTable(level, rnd, p1, loot());
            }
            if (box.isInside(p2)) {
                level.setBlock(p2, left.setValue(ChestBlock.TYPE, ChestType.RIGHT), 2);
                RandomizableContainer.setBlockEntityLootTable(level, rnd, p2, loot());
            }
        }

        boolean pilaster(int dx, int dz) {
            return (abs(dx) == 4 && abs(dz) == 8) || (abs(dz) == 4 && abs(dx) == 8);
        }

        void build() {
            final int top = Math.min(s(FLOORS) + 14, level.getMaxBuildHeight() - 2);
            final int sTop = s(FLOORS);
            final BlockState slabTop = t.slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            final BlockState slabBottom = t.slab.defaultBlockState();

            for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
                if (!colInside(dx, dz)) continue;
                for (int y = s0; y <= top; y++) clr(dx, y, dz);
            }

            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                if (!colInside(dx, dz)) continue;
                int depth = 40 - 4 * Math.max(abs(dx), abs(dz));
                for (int i = 0; i <= depth; i++) {
                    int y = s0 - i;
                    if (y < level.getMinBuildHeight()) break;
                    BlockState st = at(dx, y, dz);
                    if (i > 0 && !(st.isAir() || !st.getFluidState().isEmpty() || st.canBeReplaced())) break;
                    set(dx, y, dz, t.plinth);
                }
            }

            for (int k = 0; k <= FLOORS; k++)
                for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++)
                    set(dx, s(k), dz, ((dx + dz) & 1) == 0 ? t.floor : t.floorAlt);

            for (int y = s0 + 1; y <= sTop + 3; y++)
                for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                    if (Math.max(abs(dx), abs(dz)) < 6) continue;
                    set(dx, y, dz, rnd.nextInt(7) == 0 ? t.wallAlt : t.wall);
                }
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                int r = Math.max(abs(dx), abs(dz));
                if (r == 7) {
                    for (int k = 1; k <= FLOORS; k++) set(dx, s(k), dz, t.trim);
                    if (((dx + dz) & 1) == 0) set(dx, sTop + 4, dz, t.wall);
                }
            }

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

            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                if (pilaster(dx, dz)) {
                    for (int y = s0 + 1; y <= sTop + 3; y++) set(dx, y, dz, t.pillar);
                    continue;
                }
                if (Math.max(abs(dx), abs(dz)) != 8 || Math.min(abs(dx), abs(dz)) > 5) continue;
                if (!(dz == 8 && abs(dx) <= 2)) set(dx, s0 + 1, dz, slabBottom);
                for (int k = 1; k <= FLOORS; k++) set(dx, s(k), dz, slabTop);
            }

            for (int k = 0; k < FLOORS; k++) for (int y = s(k) + 3; y <= s(k) + 5; y++)
                for (int tt = -2; tt <= 2; tt += 4) for (int r = 6; r <= 7; r++) {
                    clr(tt, y, r); clr(tt, y, -r); clr(r, y, tt); clr(-r, y, tt);
                }

            for (int x = -1; x <= 1; x++) for (int y = s0 + 1; y <= s0 + 4; y++) { clr(x, y, 6); clr(x, y, 7); }
            for (int x = -2; x <= 2; x++) { set(x, s0 + 5, 6, t.trim); set(x, s0 + 5, 7, t.trim); }
            for (int sx = -2; sx <= 2; sx += 4) for (int y = s0 + 1; y <= s0 + 4; y++) {
                set(sx, y, 6, t.pillar); set(sx, y, 7, t.pillar);
            }
            set(-2, s0 + 4, 7, t.light); set(2, s0 + 4, 7, t.light);

            for (int k = 0; k < FLOORS; k++) for (int sx = -3; sx <= 3; sx += 6) for (int sz = -3; sz <= 3; sz += 6) {
                for (int y = s(k) + 1; y <= s(k) + H - 1; y++) set(sx, y, sz, t.pillar);
                set(sx, s(k + 1), sz, t.light);
            }

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
                    if (i < H - 1) clr(x, s(k) + H, zRow);
                }
            }

            for (int k = 0; k < FLOORS; k++) {
                doubleChest(5, s(k) + 1, 0, Direction.WEST);
                doubleChest(-5, s(k) + 1, 0, Direction.EAST);
            }

            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(dx, sTop, dz, t.accent);

            for (int y = sTop + 1; y <= sTop + 3; y++) { set(-1, y, -5, t.pillar); set(1, y, -5, t.pillar); }
            set(0, sTop + 2, -5, t.trim);
            set(0, sTop + 3, -5, t.trim);
            BlockPos vp = new BlockPos(cx, sTop + 1, cz - 5);
            if (box.isInside(vp)) {
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
            }

            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos cp = new BlockPos(cx + sx * 4, sTop + 1, cz + sz * 4);
                if (!box.isInside(cp)) continue;
                level.setBlock(cp, Blocks.CHEST.defaultBlockState()
                        .setValue(ChestBlock.FACING, sx > 0 ? Direction.WEST : Direction.EAST), 2);
                RandomizableContainer.setBlockEntityLootTable(level, rnd, cp, key("chests/end_city_treasure"));
            }

            BlockPos sp = new BlockPos(cx, sTop + 1, cz);
            if (box.isInside(sp)) {
                ServerLevel sl = level.getLevel();
                Mob boss = switch (rnd.nextInt(3)) {
                    case 0 -> DimensionalTowers.TOWER_ZOMBIE.get().create(sl);
                    case 1 -> DimensionalTowers.TOWER_SKELETON.get().create(sl);
                    default -> DimensionalTowers.TOWER_BOSS.get().create(sl);
                };
                if (boss instanceof BossKit.TowerMob tm) {
                    boss.moveTo(cx + 0.5, sTop + 1, cz + 0.5, rnd.nextFloat() * 360f, 0f);
                    tm.setup(t.bossName);
                    level.addFreshEntity(boss);
                }
            }
        }
    }
}