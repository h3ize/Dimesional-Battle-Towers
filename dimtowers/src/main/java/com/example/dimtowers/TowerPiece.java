package com.example.dimtowers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
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
    /** Distance between floors. */
    static final int H = 12;
    /** Spawner floors below the boss arena. */
    static final int FLOORS = 5;
    /** Outer half-width of the shell (tower is 31 wide, ~35 with buttresses). */
    static final int R = 15;
    /** Half-width of the usable interior (shell is two blocks thick). */
    static final int I = R - 2;
    /** Offset of the four interior pillars from the centre. */
    static final int PIL = 7;
    /** Window / wall-bay centres along each face. */
    static final int[] WIN = {-12, -6, 0, 6, 12};
    /** Interior and exterior pilaster positions along each face (between the windows). */
    static final int[] PILASTER = {-9, -3, 3, 9};

    private final String themeId;
    private final int cx, cz, s0;

    public TowerPiece(String themeId, int cx, int cz, int s0) {
        super(DimensionalTowers.TOWER_PIECE.get(), 0,
                new BoundingBox(cx - (R + 4), s0 - 60, cz - (R + 4), cx + (R + 4), s0 + FLOORS * H + 24, cz + (R + 4)));
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
    private static final List<String> ILLAGER_LOOT = List.of("chests/pillager_outpost", "chests/pillager_outpost",
            "chests/woodland_mansion", "chests/village/village_weaponsmith", "chests/village/village_armorer");
    private static final List<String> END_LOOT = List.of("chests/ancient_city", "chests/woodland_mansion",
            "chests/bastion_treasure", "chests/stronghold_library");

    private final class Builder {
        final WorldGenLevel level;
        final RandomSource rnd;
        final BoundingBox box;
        final Theme t;
        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState bookshelf = Blocks.BOOKSHELF.defaultBlockState();
        final BlockState barrel = Blocks.BARREL.defaultBlockState();
        final BlockState candle = Blocks.CANDLE.defaultBlockState()
                .setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, Boolean.TRUE);
        final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        final int yMax;

        Builder(WorldGenLevel level, RandomSource rnd, BoundingBox box, Theme t) {
            this.level = level; this.rnd = rnd; this.box = box; this.t = t;
            this.yMax = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 2;
        }

        int s(int k) { return s0 + k * H; }
        int abs(int v) { return Math.abs(v); }

        void set(int dx, int y, int dz, BlockState st) {
            int x = cx + dx, z = cz + dz;
            if (y > yMax || !box.isInside(x, y, z)) return;
            level.setBlock(m.set(x, y, z), st, 2);
        }
        void clr(int dx, int y, int dz) { set(dx, y, dz, air); }
        BlockState at(int dx, int y, int dz) { return level.getBlockState(new BlockPos(cx + dx, y, cz + dz)); }
        boolean colInside(int dx, int dz) { return box.isInside(cx + dx, box.minY(), cz + dz); }

        /** Face-relative set. face 0=south(+z) 1=north(-z) 2=east(+x) 3=west(-x); tt = along the wall, r = distance from centre. */
        void fs(int face, int tt, int r, int y, BlockState st) {
            switch (face) {
                case 0 -> set(tt, y, r, st);
                case 1 -> set(tt, y, -r, st);
                case 2 -> set(r, y, tt, st);
                default -> set(-r, y, tt, st);
            }
        }

        BlockState wallBlock() { return rnd.nextInt(7) == 0 ? t.wallAlt : t.wall; }

        /** Compass-rose floor with a diamond lattice and a trim border against the walls. */
        BlockState floorAt(int dx, int dz) {
            int r = Math.max(abs(dx), abs(dz));
            int man = abs(dx) + abs(dz);
            if (r == I) return t.trim;
            if (dx == 0 || dz == 0) return t.floorAlt;
            if (man % 6 == 0) return t.floorAlt;
            return t.floor;
        }

        BlockState carpet() {
            return switch (t) {
                case NETHER -> Blocks.BLACK_CARPET.defaultBlockState();
                case END, DARK_FOREST -> Blocks.PURPLE_CARPET.defaultBlockState();
                case OAK, JUNGLE, SWAMP -> Blocks.GREEN_CARPET.defaultBlockState();
                case PILLAGER -> Blocks.GRAY_CARPET.defaultBlockState();
                case TAIGA, BADLANDS -> Blocks.ORANGE_CARPET.defaultBlockState();
                default -> Blocks.RED_CARPET.defaultBlockState();
            };
        }

        ResourceKey<LootTable> loot() {
            ArrayList<String> pool = new ArrayList<>(COMMON_LOOT);
            switch (t) {
                case NETHER, PIGLIN -> pool.addAll(NETHER_LOOT);
                case END -> pool.addAll(END_LOOT);
                case PILLAGER, DARK_FOREST -> pool.addAll(ILLAGER_LOOT);
                default -> pool.addAll(OVERWORLD_LOOT);
            }
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

        void blockEntityNbt(BlockPos pos, String snbt) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return;
            try {
                be.loadWithComponents(TagParser.parseTag(snbt), level.registryAccess());
                be.setChanged();
            } catch (Exception ignored) { }
        }

        /** Random-monster spawner for a regular floor. Ignores light so the lit rooms still spawn mobs. */
        void monsterSpawner(int dx, int y, int dz) {
            BlockPos p = new BlockPos(cx + dx, y, cz + dz);
            if (y > yMax || !box.isInside(p)) return;
            level.setBlock(p, Blocks.SPAWNER.defaultBlockState(), 2);
            EntityType<?> type = t.mobs.get(rnd.nextInt(t.mobs.size()));
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            blockEntityNbt(p, "{SpawnData:{entity:{id:\"" + id + "\"},custom_spawn_rules:{"
                    + "block_light_limit:{min_inclusive:0,max_inclusive:15},"
                    + "sky_light_limit:{min_inclusive:0,max_inclusive:15}}},"
                    + "SpawnCount:4s,SpawnRange:7s,MaxNearbyEntities:8s,RequiredPlayerRange:24s,"
                    + "MinSpawnDelay:200s,MaxSpawnDelay:500s}");
        }

        /** Boss Trial Spawner: one boss, then it ejects the ominous trial key. */
        void bossTrialSpawner(int dx, int y, int dz) {
            BlockPos p = new BlockPos(cx + dx, y, cz + dz);
            if (y > yMax || !box.isInside(p)) return;
            level.setBlock(p, Blocks.TRIAL_SPAWNER.defaultBlockState(), 2);
            StringBuilder pot = new StringBuilder();
            for (String id : t.bosses) {
                if (pot.length() > 0) pot.append(',');
                pot.append("{weight:1,data:{entity:{id:\"").append(id).append("\"}}}");
            }
            blockEntityNbt(p, "{normal_config:{spawn_range:5,total_mobs:1.0f,simultaneous_mobs:1.0f,"
                    + "total_mobs_added_per_player:0.0f,simultaneous_mobs_added_per_player:0.0f,ticks_between_spawn:40,"
                    + "spawn_potentials:[" + pot + "],"
                    + "loot_tables_to_eject:[{weight:1,data:\"dimtowers:spawners/boss_key\"}]},"
                    + "required_player_range:12,target_cooldown_length:1200000}");
        }

        /** Interior pilasters between the windows on all four walls, from y0 up to y1. */
        void interiorPilasters(int y0, int y1) {
            for (int f = 0; f < 4; f++) for (int tt : PILASTER) {
                fs(f, tt, I, y0, t.trim);
                for (int y = y0 + 1; y < y1; y++) fs(f, tt, I, y, t.pillar);
                fs(f, tt, I, y1, t.trim);
            }
        }

        /** Corner brazier posts in the four inside corners. */
        void cornerPosts(int b) {
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                set(sx * I, b + 1, sz * I, t.trim);
                set(sx * I, b + 2, sz * I, t.pillar);
                set(sx * I, b + 3, sz * I, t.lanternStanding);
            }
        }

        void build() {
            final int sTop = s(FLOORS);
            final int top = Math.min(sTop + 22, yMax);
            final BlockState slabTop = t.slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            final BlockState slabBot = t.slab.defaultBlockState();
            final BlockState wallPiece = t.wallPiece.defaultBlockState();
            final BlockState carpet = carpet();

            // 1. clear the whole volume
            for (int dx = -(R + 3); dx <= R + 3; dx++) for (int dz = -(R + 3); dz <= R + 3; dz++) {
                if (!colInside(dx, dz)) continue;
                for (int y = s0; y <= top; y++) clr(dx, y, dz);
            }

            // 2. tapered foundation down to the ground
            for (int dx = -(R + 2); dx <= R + 2; dx++) for (int dz = -(R + 2); dz <= R + 2; dz++) {
                if (!colInside(dx, dz)) continue;
                int depth = 56 - 3 * Math.max(abs(dx), abs(dz));
                for (int i = 0; i <= depth; i++) {
                    int y = s0 - i;
                    if (y < level.getMinBuildHeight()) break;
                    BlockState st = at(dx, y, dz);
                    if (i > 0 && !(st.isAir() || !st.getFluidState().isEmpty() || st.canBeReplaced())) break;
                    set(dx, y, dz, t.plinth);
                }
            }

            // 3. floors (compass-rose + diamond pattern); k == FLOORS is the boss arena
            for (int k = 0; k <= FLOORS; k++)
                for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++)
                    set(dx, s(k), dz, floorAt(dx, dz));

            // 4. double-thick walls with a plinth course, string courses and a crenellated parapet
            for (int y = s0 + 1; y <= sTop + 5; y++)
                for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                    if (Math.max(abs(dx), abs(dz)) < R - 1) continue;
                    set(dx, y, dz, y == s0 + 1 ? t.plinth : wallBlock());
                }
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                if (Math.max(abs(dx), abs(dz)) != R) continue;
                for (int k = 1; k <= FLOORS; k++) set(dx, s(k), dz, t.trim);
                if (((dx + dz) & 1) == 0) set(dx, sTop + 6, dz, t.wall);
            }

            // 5. cornices (one per floor), a low base ledge and an outer apron step
            for (int dx = -(R + 2); dx <= R + 2; dx++) for (int dz = -(R + 2); dz <= R + 2; dz++) {
                int r = Math.max(abs(dx), abs(dz));
                if (r == R + 1) {
                    for (int k = 1; k <= FLOORS; k++) set(dx, s(k), dz, slabTop);
                    if (!(dz == R + 1 && abs(dx) <= 4)) set(dx, s0 + 1, dz, slabBot);
                } else if (r == R + 2) {
                    if (!(dz == R + 2 && abs(dx) <= 4)) set(dx, s0 + 1, dz, slabBot);
                }
            }

            // 6. pilasters on every face
            for (int f = 0; f < 4; f++) for (int tt : PILASTER) {
                for (int y = s0 + 1; y <= sTop + 5; y++) fs(f, tt, R + 1, y, t.pillar);
                fs(f, tt, R + 1, sTop + 6, wallPiece);
            }

            // 7. corner turrets with tall lantern spires
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                for (int ax = R - 1; ax <= R + 1; ax++) for (int az = R - 1; az <= R + 1; az++) {
                    boolean corner = ax == R + 1 && az == R + 1;
                    for (int y = s0 + 1; y <= sTop + 8; y++) set(sx * ax, y, sz * az, corner ? t.pillar : wallBlock());
                    set(sx * ax, sTop + 9, sz * az, t.trim);
                    if (!(ax == R && az == R)) set(sx * ax, sTop + 10, sz * az, slabBot);
                }
                for (int y = sTop + 10; y <= sTop + 13; y++) set(sx * R, y, sz * R, t.pillar);
                set(sx * R, sTop + 14, sz * R, wallPiece);
                set(sx * R, sTop + 15, sz * R, t.light);
            }

            // 8. tall arched windows (five per face per floor)
            for (int k = 0; k < FLOORS; k++) for (int f = 0; f < 4; f++) for (int tt : WIN) {
                if (k == 0 && f == 0 && tt == 0) continue; // the main door
                int b = s(k);
                for (int w = -1; w <= 1; w++) {
                    int c = tt + w;
                    for (int y = b + 3; y <= b + 8; y++) { fs(f, c, R, y, t.glass); fs(f, c, R - 1, y, air); }
                    if (w == 0) { fs(f, c, R, b + 9, t.glass); fs(f, c, R - 1, b + 9, air); }
                    else fs(f, c, R, b + 9, t.trim);
                    fs(f, c, R, b + 10, t.trim);
                    fs(f, c, R, b + 2, t.trim);
                }
                for (int y = b + 3; y <= b + 9; y++) {
                    if (abs(tt - 2) < R - 1) fs(f, tt - 2, R, y, t.pillar);
                    if (abs(tt + 2) < R - 1) fs(f, tt + 2, R, y, t.pillar);
                }
            }

            // 9. grand entrance on the south face
            for (int dx = -2; dx <= 2; dx++) for (int y = s0 + 1; y <= s0 + 7; y++) { clr(dx, y, R - 1); clr(dx, y, R); }
            for (int dx = -1; dx <= 1; dx++) { clr(dx, s0 + 8, R - 1); clr(dx, s0 + 8, R); }
            for (int z = R - 1; z <= R + 1; z++) {
                set(-2, s0 + 8, z, t.trim); set(2, s0 + 8, z, t.trim);
                for (int dx = -2; dx <= 2; dx++) set(dx, s0 + 9, z, t.trim);
                for (int y = s0 + 1; y <= s0 + 9; y++) { set(-3, y, z, t.pillar); set(3, y, z, t.pillar); }
            }
            set(-3, s0 + 10, R + 1, t.light); set(3, s0 + 10, R + 1, t.light);

            // 10. interior floors
            for (int k = 0; k < FLOORS; k++) {
                int b = s(k);

                // ceiling beams: a square through the pillars plus a cross through the middle
                for (int i = -PIL; i <= PIL; i++) {
                    set(i, b + H - 1, PIL, t.trim); set(i, b + H - 1, -PIL, t.trim);
                    set(PIL, b + H - 1, i, t.trim); set(-PIL, b + H - 1, i, t.trim);
                    set(i, b + H - 1, 0, t.trim);   set(0, b + H - 1, i, t.trim);
                }

                // pillars with a trim base and glowing capitals
                for (int sx = -PIL; sx <= PIL; sx += 2 * PIL) for (int sz = -PIL; sz <= PIL; sz += 2 * PIL) {
                    set(sx, b + 1, sz, t.trim);
                    for (int y = b + 2; y <= b + H - 2; y++) set(sx, y, sz, t.pillar);
                    set(sx, b + H - 1, sz, t.trim);
                    set(sx, b + H, sz, t.light);
                }

                // chandeliers on the beam crossings, plus a big one over the spawner
                int[][] ch = {{0, PIL}, {0, -PIL}, {PIL, 0}, {-PIL, 0}};
                for (int[] c : ch) {
                    set(c[0], b + H - 2, c[1], chain);
                    set(c[0], b + H - 3, c[1], chain);
                    set(c[0], b + H - 4, c[1], t.lanternHanging);
                }
                for (int y = b + H - 2; y >= b + H - 5; y--) set(0, y, 0, chain);
                set(0, b + H - 6, 0, t.lanternHanging);

                // carpet runner (leads in from the front door on the ground floor)
                int zMax = k == 0 ? R : R - 5;
                for (int dz = -(R - 5); dz <= zMax; dz++) {
                    if (abs(dz) <= 3) continue;
                    for (int dx = -1; dx <= 1; dx++) set(dx, b + 1, dz, carpet);
                }

                // spawner dais: layered floor medallion, slab collar and four lantern posts
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                    int r = Math.max(abs(dx), abs(dz));
                    set(dx, b, dz, r <= 1 ? t.accent : r == 2 ? t.floorAlt : t.trim);
                    if (r == 1 && (dx != 0 || dz != 0)) set(dx, b + 1, dz, slabBot);
                }
                for (int sx = -3; sx <= 3; sx += 6) for (int sz = -3; sz <= 3; sz += 6) {
                    set(sx, b + 1, sz, t.trim);
                    set(sx, b + 2, sz, t.pillar);
                    set(sx, b + 3, sz, t.lanternStanding);
                }
                monsterSpawner(0, b + 1, 0);

                // lantern posts flanking the runner
                for (int sx = -3; sx <= 3; sx += 6) for (int sz = -6; sz <= 6; sz += 12) {
                    set(sx, b + 1, sz, t.trim);
                    set(sx, b + 2, sz, t.lanternStanding);
                }

                // wall pilasters, corner braziers
                interiorPilasters(b + 1, b + H - 1);
                cornerPosts(b);

                // library nooks and barrels under each window, topped with candles
                for (int f = 0; f < 4; f++) for (int tt : WIN) {
                    if (k == 0 && f == 0 && tt == 0) continue;  // front door
                    if (f >= 2 && tt == 0) continue;            // treasure chests
                    BlockState base = rnd.nextInt(4) == 0 ? barrel : bookshelf;
                    fs(f, tt, I, b + 1, base);
                    fs(f, tt, I, b + 2, base);
                    fs(f, tt, I, b + 3, candle);
                }

                // two-wide switchback staircase up to the next floor, with a baluster rail
                boolean even = k % 2 == 0;
                int zRow = even ? -(R - 3) : (R - 3), zIn = even ? -(R - 4) : (R - 4);
                int zRail = even ? zIn + 1 : zIn - 1;
                Direction dir = even ? Direction.EAST : Direction.WEST;
                int startX = even ? -(H - 1) : (H - 1), step = even ? 1 : -1;
                BlockState stair = t.stairs.defaultBlockState().setValue(StairBlock.FACING, dir);
                for (int i = 0; i < H; i++) {
                    int x = startX + step * i;
                    int y = b + 1 + i;
                    for (int z : new int[]{zRow, zIn}) {
                        for (int yy = b + 1; yy < y; yy++) set(x, yy, z, t.floorAlt);
                        set(x, y, z, stair);
                        if (i < H - 1) clr(x, b + H, z);
                    }
                    if (i < H - 1) set(x, y, zRail, wallPiece);
                }

                doubleChest(I, b + 1, 0, Direction.WEST);
                doubleChest(-I, b + 1, 0, Direction.EAST);
            }

            // 11. boss arena (open to the sky)
            for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
                int r = Math.max(abs(dx), abs(dz));
                set(dx, sTop, dz, r <= 1 ? t.accent : r == 2 ? t.floorAlt : r == 3 ? t.trim : r == 4 ? t.floorAlt : t.trim);
            }
            bossTrialSpawner(0, sTop + 1, 0);
            for (int sx = -6; sx <= 6; sx += 12) for (int sz = -6; sz <= 6; sz += 12) {
                for (int y = sTop + 1; y <= sTop + 3; y++) set(sx, y, sz, t.pillar);
                set(sx, sTop + 4, sz, wallPiece);
                set(sx, sTop + 5, sz, t.light);
            }
            // braziers at the four compass points of the dais
            int[][] br = {{6, 0}, {-6, 0}, {0, 6}, {0, -6}};
            for (int[] c : br) { set(c[0], sTop + 1, c[1], t.trim); set(c[0], sTop + 2, c[1], t.lanternStanding); }
            // wall lights, pilasters and corner posts
            for (int f = 0; f < 4; f++) for (int tt : WIN) fs(f, tt, R - 1, sTop + 3, t.light);
            interiorPilasters(sTop + 1, sTop + 4);
            cornerPosts(sTop);

            // vault on the south side, framed
            for (int y = sTop + 1; y <= sTop + 3; y++) { set(-1, y, I, t.pillar); set(1, y, I, t.pillar); }
            set(0, sTop + 2, I, t.trim);
            set(0, sTop + 3, I, t.trim);
            BlockPos vp = new BlockPos(cx, sTop + 1, cz + I);
            if (sTop + 1 <= yMax && box.isInside(vp)) {
                level.setBlock(vp, Blocks.VAULT.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 2);
                blockEntityNbt(vp, "{config:{key_item:{id:\"minecraft:ominous_trial_key\",count:1},"
                        + "loot_table:\"dimtowers:vault/reward\"}}");
            }

            // treasure chests
            int[][] cp = {{R - 3, R - 3, 0}, {-(R - 3), R - 3, 1}, {I, -6, 0}, {-I, -6, 1}};
            for (int[] c : cp) {
                BlockPos p = new BlockPos(cx + c[0], sTop + 1, cz + c[1]);
                if (sTop + 1 > yMax || !box.isInside(p)) continue;
                level.setBlock(p, Blocks.CHEST.defaultBlockState()
                        .setValue(ChestBlock.FACING, c[2] == 0 ? Direction.WEST : Direction.EAST), 2);
                RandomizableContainer.setBlockEntityLootTable(level, rnd, p, key("chests/end_city_treasure"));
            }
        }
    }
}
