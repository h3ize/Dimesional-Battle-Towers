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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stepped keep: three tiers that taper as they climb (floors 0-1 wide, 2-3 medium, 4 + arena narrow),
 * terraces with battlements where it steps in, pointed corner spires, a balcony over the gate,
 * and a different room on every floor:
 *   0 Great Hall, 1 Library, 2 Armory, 3 Throne Hall, 4 Ritual Sanctum, 5 boss arena (open sky).
 */
public class TowerPiece extends StructurePiece {
    /** Distance between floors. */
    static final int H = 12;
    /** Spawner floors below the boss arena. */
    static final int FLOORS = 5;
    /** Outer half-width of the widest (bottom) tier. */
    static final int R = 15;
    /** Foundation depth under the centre; tapers outward. */
    static final int FOUND = 60;
    /** Half-width of the piece's bounding box. */
    static final int BOX = R + 5;

    /** Outer half-width of the shell at floor k (0..FLOORS): 15,15,13,13,11,11. */
    static int rad(int k) { return k <= 1 ? R : k <= 3 ? R - 2 : R - 4; }

    private final String themeId;
    private final int cx, cz, s0;

    public TowerPiece(String themeId, int cx, int cz, int s0) {
        super(DimensionalTowers.TOWER_PIECE.get(), 0,
                new BoundingBox(cx - BOX, s0 - FOUND - 4, cz - BOX, cx + BOX, s0 + FLOORS * H + 24, cz + BOX));
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
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState bookshelf = Blocks.BOOKSHELF.defaultBlockState();
        final BlockState cobweb = Blocks.COBWEB.defaultBlockState();
        final BlockState candle = Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true);
        final BlockState slabTop, slabBot, wallPiece;
        final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        final int yMax;
        // theme flavour
        BlockState carpetA, carpetB, flair, wearBlk;

        Builder(WorldGenLevel level, RandomSource rnd, BoundingBox box, Theme t) {
            this.level = level; this.rnd = rnd; this.box = box; this.t = t;
            this.yMax = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 2;
            this.slabTop = t.slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            this.slabBot = t.slab.defaultBlockState();
            this.wallPiece = t.wallPiece.defaultBlockState();
            switch (t) {
                case NETHER -> {
                    carpetA = Blocks.BLACK_CARPET.defaultBlockState(); carpetB = Blocks.ORANGE_CARPET.defaultBlockState();
                    flair = Blocks.LAVA_CAULDRON.defaultBlockState(); wearBlk = Blocks.NETHER_SPROUTS.defaultBlockState();
                }
                case PIGLIN -> {
                    carpetA = Blocks.YELLOW_CARPET.defaultBlockState(); carpetB = Blocks.BLACK_CARPET.defaultBlockState();
                    flair = Blocks.GOLD_BLOCK.defaultBlockState(); wearBlk = Blocks.NETHER_SPROUTS.defaultBlockState();
                }
                case END -> {
                    carpetA = Blocks.PURPLE_CARPET.defaultBlockState(); carpetB = Blocks.MAGENTA_CARPET.defaultBlockState();
                    flair = Blocks.AMETHYST_CLUSTER.defaultBlockState(); wearBlk = Blocks.AMETHYST_CLUSTER.defaultBlockState();
                }
                default -> {
                    carpetA = Blocks.RED_CARPET.defaultBlockState(); carpetB = Blocks.BLACK_CARPET.defaultBlockState();
                    flair = candle; wearBlk = Blocks.MOSS_CARPET.defaultBlockState();
                }
            }
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

        void ring(int r, int y, BlockState st) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                if (Math.max(abs(dx), abs(dz)) == r) set(dx, y, dz, st);
        }

        BlockState wallBlock() { return rnd.nextInt(7) == 0 ? t.wallAlt : t.wall; }

        BlockState floorAt(int dx, int dz) {
            int r = Math.max(abs(dx), abs(dz));
            if (dx == 0 || dz == 0 || abs(dx) == abs(dz)) return t.floorAlt;
            return r % 3 == 0 ? t.floorAlt : t.floor;
        }

        ResourceKey<LootTable> loot() {
            ArrayList<String> pool = new ArrayList<>(COMMON_LOOT);
            switch (t) {
                case NETHER, PIGLIN -> pool.addAll(NETHER_LOOT);
                case END -> pool.addAll(END_LOOT);
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
                    + "SpawnCount:4s,SpawnRange:9s,MaxNearbyEntities:10s,RequiredPlayerRange:28s,"
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
                    + "required_player_range:16,target_cooldown_length:1200000}");
        }

        // ------------------------------------------------------------------ build

        void build() {
            final int sTop = s(FLOORS);
            final int top = Math.min(sTop + 24, yMax);

            // 1. clear the whole volume
            for (int dx = -(R + 3); dx <= R + 3; dx++) for (int dz = -(R + 3); dz <= R + 3; dz++) {
                if (!colInside(dx, dz)) continue;
                for (int y = s0; y <= top; y++) clr(dx, y, dz);
            }

            // 2. tapered foundation down to the ground
            for (int dx = -(R + 2); dx <= R + 2; dx++) for (int dz = -(R + 2); dz <= R + 2; dz++) {
                if (!colInside(dx, dz)) continue;
                int depth = FOUND - 3 * Math.max(abs(dx), abs(dz));
                for (int i = 0; i <= depth; i++) {
                    int y = s0 - i;
                    if (y < level.getMinBuildHeight()) break;
                    BlockState st = at(dx, y, dz);
                    if (i > 0 && !(st.isAir() || !st.getFluidState().isEmpty() || st.canBeReplaced())) break;
                    set(dx, y, dz, t.plinth);
                }
            }

            // 3. floors (compass rose with diagonals); each floor only as wide as its tier
            for (int k = 0; k <= FLOORS; k++) {
                int rd = rad(k);
                for (int dx = -rd; dx <= rd; dx++) for (int dz = -rd; dz <= rd; dz++)
                    set(dx, s(k), dz, floorAt(dx, dz));
            }

            // 4. double-thick walls, tier by tier
            for (int k = 0; k <= FLOORS; k++) {
                int rd = rad(k);
                int yTop = k == FLOORS ? sTop + 5 : s(k + 1) - 1;
                for (int y = s(k) + 1; y <= yTop; y++)
                    for (int dx = -rd; dx <= rd; dx++) for (int dz = -rd; dz <= rd; dz++) {
                        if (Math.max(abs(dx), abs(dz)) < rd - 1) continue;
                        set(dx, y, dz, y == s0 + 1 ? t.plinth : wallBlock());
                    }
            }

            // 5. string courses where a tier continues, terraces with battlements where it steps in
            for (int k = 1; k <= FLOORS; k++) {
                int rp = rad(k - 1), rc = rad(k);
                if (rc == rp) {
                    ring(rc, s(k), t.trim);
                    ring(rc + 1, s(k), slabTop);               // cornice
                } else {
                    ring(rp, s(k), t.trim);
                    ring(rp - 1, s(k), t.floorAlt);
                    for (int dx = -rp; dx <= rp; dx++) for (int dz = -rp; dz <= rp; dz++)
                        if (Math.max(abs(dx), abs(dz)) == rp && ((dx + dz) & 1) == 0) set(dx, s(k) + 1, dz, t.wall);
                }
            }
            for (int dx = -rad(FLOORS); dx <= rad(FLOORS); dx++) for (int dz = -rad(FLOORS); dz <= rad(FLOORS); dz++)
                if (Math.max(abs(dx), abs(dz)) == rad(FLOORS) && ((dx + dz) & 1) == 0) set(dx, sTop + 6, dz, t.wall);
            // low base ledge (gap at the gate)
            for (int dx = -(R + 1); dx <= R + 1; dx++) for (int dz = -(R + 1); dz <= R + 1; dz++) {
                if (Math.max(abs(dx), abs(dz)) != R + 1) continue;
                if (!(dz == R + 1 && abs(dx) <= 5)) set(dx, s0 + 1, dz, slabBot);
            }

            // 6. pilasters, windows
            for (int k = 0; k <= FLOORS; k++) pilasters(k, sTop);
            for (int k = 0; k < FLOORS; k++) windows(k);

            // 7. corner spires: one tall turret per tier group (floors 0-1, 2-3, 4-arena)
            turret(rad(1), s(0) + 1, s(2) + 3, 4);
            turret(rad(3), s(2) + 1, s(4) + 3, 4);
            turret(rad(5), s(4) + 1, sTop + 8, 9);

            // 8. grand gate, balcony above it
            gate();
            balcony();

            // 9. rooms
            for (int k = 0; k < FLOORS; k++) { room(k); wear(k); }

            // 10. boss arena
            arena(sTop);
        }

        // ------------------------------------------------------------------ exterior

        void pilasters(int k, int sTop) {
            int rd = rad(k);
            int pt = (rd - 5) / 2;
            int yEnd = k == FLOORS ? sTop + 5 : s(k + 1) + 1;
            for (int f = 0; f < 4; f++) for (int tt = -pt; tt <= pt; tt += 2 * pt) {
                for (int y = s(k) + 1; y <= yEnd; y++) fs(f, tt, rd + 1, y, t.pillar);
                fs(f, tt, rd + 1, yEnd + 1, wallPiece);
            }
        }

        void windows(int k) {
            int rd = rad(k), b = s(k), wt = rd - 5;
            for (int f = 0; f < 4; f++) for (int tt = -wt; tt <= wt; tt += wt) {
                if (k == 0 && f == 0 && tt == 0) continue; // the main door
                for (int w = -1; w <= 1; w++) {
                    int c = tt + w;
                    for (int y = b + 3; y <= b + 8; y++) { fs(f, c, rd, y, t.glass); fs(f, c, rd - 1, y, air); }
                    if (w == 0) { fs(f, c, rd, b + 9, t.glass); fs(f, c, rd - 1, b + 9, air); }
                    else fs(f, c, rd, b + 9, t.trim);
                    fs(f, c, rd, b + 10, t.trim);
                    fs(f, c, rd, b + 2, t.trim);
                }
                for (int y = b + 3; y <= b + 9; y++) { fs(f, tt - 2, rd, y, t.pillar); fs(f, tt + 2, rd, y, t.pillar); }
            }
        }

        void turret(int rd, int y0, int yT, int spire) {
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                for (int ax = rd - 1; ax <= rd + 1; ax++) for (int az = rd - 1; az <= rd + 1; az++) {
                    boolean corner = ax == rd + 1 && az == rd + 1;
                    for (int y = y0; y <= yT; y++) set(sx * ax, y, sz * az, corner ? t.pillar : wallBlock());
                    set(sx * ax, yT + 1, sz * az, t.trim);
                    if (!(ax == rd && az == rd)) set(sx * ax, yT + 2, sz * az, slabBot);
                }
                for (int i = 0; i < spire; i++) set(sx * rd, yT + 2 + i, sz * rd, t.pillar);
                set(sx * rd, yT + 2 + spire, sz * rd, wallPiece);
                set(sx * rd, yT + 3 + spire, sz * rd, t.light);
            }
        }

        void gate() {
            for (int dx = -3; dx <= 3; dx++) for (int y = s0 + 1; y <= s0 + 8; y++) { clr(dx, y, R - 1); clr(dx, y, R); }
            for (int dx = -2; dx <= 2; dx++) { clr(dx, s0 + 9, R - 1); clr(dx, s0 + 9, R); }
            for (int z = R - 1; z <= R + 1; z++) {
                set(-3, s0 + 9, z, t.trim); set(3, s0 + 9, z, t.trim);
                for (int dx = -3; dx <= 3; dx++) set(dx, s0 + 10, z, t.trim);
                for (int y = s0 + 1; y <= s0 + 10; y++) { set(-4, y, z, t.pillar); set(4, y, z, t.pillar); }
            }
            set(-4, s0 + 11, R + 1, t.light); set(4, s0 + 11, R + 1, t.light);
            // forecourt: carpet and lantern posts
            for (int z = R + 1; z <= R + 3; z++) for (int dx = -1; dx <= 1; dx++) set(dx, s0 + 1, z, carpetA);
            for (int sx = -3; sx <= 3; sx += 6) {
                set(sx, s0 + 1, R + 3, t.pillar);
                set(sx, s0 + 2, R + 3, t.pillar);
                set(sx, s0 + 3, R + 3, t.lanternStanding);
            }
        }

        /** Balcony over the gate, reached through the middle window bay of floor 1. */
        void balcony() {
            int y = s(1);
            for (int tt = -3; tt <= 3; tt++) for (int z = R + 1; z <= R + 3; z++) set(tt, y, z, t.trim);
            for (int tt = -3; tt <= 3; tt++) set(tt, y + 1, R + 3, wallPiece);
            for (int z = R + 1; z <= R + 3; z++) { set(-3, y + 1, z, wallPiece); set(3, y + 1, z, wallPiece); }
            set(-3, y + 2, R + 3, t.lanternStanding); set(3, y + 2, R + 3, t.lanternStanding);
            for (int tt = -1; tt <= 1; tt++) for (int yy = y + 1; yy <= y + 5; yy++) { clr(tt, yy, R - 1); clr(tt, yy, R); }
            for (int z = R - 1; z <= R; z++) set(0, y + 6, z, t.trim);
        }

        // ------------------------------------------------------------------ rooms

        void room(int k) {
            final int rd = rad(k), b = s(k), IN = rd - 2, p = IN - 5;
            final int rs = Math.min(rd, rad(k + 1));          // stair radius (fits the tier above)
            final boolean even = k % 2 == 0;
            final int side = even ? -1 : 1;                    // side of the room the stairs live on

            // spawner pad
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(dx, b, dz, t.accent);

            // room-specific decor first, so structure overwrites cleanly
            switch (k) {
                case 0 -> greatHall(b, rd, IN);
                case 1 -> library(b, rd, IN);
                case 2 -> armory(b, IN);
                case 3 -> throneHall(b, IN);
                default -> sanctum(b);
            }

            // pillars with glowing capitals and trimmed bases
            for (int sx = -p; sx <= p; sx += 2 * p) for (int sz = -p; sz <= p; sz += 2 * p) {
                set(sx, b + 1, sz, t.trim);
                for (int y = b + 2; y <= b + H - 1; y++) set(sx, y, sz, t.pillar);
                set(sx, b + H - 1, sz, t.trim);
                set(sx, b + H, sz, t.light);
                set(sx - Integer.signum(sx), b + H - 1, sz, slabTop);
                set(sx, b + H - 1, sz - Integer.signum(sz), slabTop);
            }

            // perimeter cornice under the ceiling
            ring(IN, b + H - 1, t.trim);

            // chandeliers: four mid-bay ones plus a big one over the spawner
            int[][] ch = {{0, p}, {0, -p}, {p, 0}, {-p, 0}};
            for (int[] c : ch) {
                for (int y = b + H - 1; y >= b + H - 4; y--) set(c[0], y, c[1], chain);
                set(c[0], b + H - 5, c[1], t.lanternHanging);
            }
            for (int y = b + H - 1; y >= b + H - 5; y--) set(0, y, 0, chain);
            set(0, b + H - 6, 0, t.lanternHanging);
            int[][] arms = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] a : arms) { set(a[0], b + H - 1, a[1], t.trim); set(a[0], b + H - 2, a[1], t.lanternHanging); }

            monsterSpawner(0, b + 1, 0);

            // two-wide switchback staircase up to the next floor
            int zRow = side * (rs - 3), zIn = side * (rs - 4);
            Direction dir = even ? Direction.EAST : Direction.WEST;
            int startX = side * (rs - 2), step = even ? 1 : -1;
            BlockState stair = t.stairs.defaultBlockState().setValue(StairBlock.FACING, dir);
            for (int i = 0; i < H; i++) {
                int x = startX + step * i;
                int y = b + 1 + i;
                for (int z : new int[]{zRow, zIn}) {
                    for (int yy = b + 1; yy < y; yy++) set(x, yy, z, t.floorAlt);
                    set(x, y, z, stair);
                    if (i < H - 1) clr(x, b + H, z);
                }
            }

            doubleChest(IN, b + 1, 0, Direction.WEST);
            doubleChest(-IN, b + 1, 0, Direction.EAST);

            // cobwebs in the ceiling corners
            for (int sx = -IN; sx <= IN; sx += 2 * IN) for (int sz = -IN; sz <= IN; sz += 2 * IN) {
                set(sx, b + H - 2, sz, cobweb);
                set(sx - Integer.signum(sx), b + H - 2, sz, cobweb);
                set(sx, b + H - 2, sz - Integer.signum(sz), cobweb);
            }
        }

        /** Floor 0: runner from the gate, two long banquet tables with candles and chairs. */
        void greatHall(int b, int rd, int IN) {
            for (int z = 4; z <= rd - 1; z++) for (int dx = -1; dx <= 1; dx++) set(dx, b + 1, z, carpetA);
            for (int z = 4; z <= rd - 1; z++) { set(-2, b + 1, z, carpetB); set(2, b + 1, z, carpetB); }
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int z = -5; z <= 5; z++) {
                    set(sx * 4, b + 1, z, slabBot);
                    set(sx * 5, b + 1, z, slabBot);
                    if (z % 2 == 0) set(sx * 4, b + 2, z, candle);
                }
                BlockState chair = t.stairs.defaultBlockState()
                        .setValue(StairBlock.FACING, sx > 0 ? Direction.EAST : Direction.WEST);
                for (int z = -4; z <= 4; z += 2) set(sx * 6, b + 1, z, chair);
            }
        }

        /** Floor 1: tall bookshelf walls between the windows, reading carpets, enchanting tables. */
        void library(int b, int rd, int IN) {
            int g = rd - 8;
            for (int f = 0; f < 4; f++) for (int sgn = -1; sgn <= 1; sgn += 2) {
                for (int tt = 3; tt <= g; tt++) for (int y = b + 1; y <= b + 4; y++) fs(f, sgn * tt, IN, y, bookshelf);
                fs(f, sgn * 3, IN, b + 5, t.lanternStanding);
                fs(f, sgn * g, IN, b + 5, t.lanternStanding);
            }
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                int r = Math.max(abs(dx), abs(dz));
                if (r == 5) set(dx, b + 1, dz, carpetA);
                else if (r == 6) set(dx, b + 1, dz, carpetB);
            }
            for (int sx = -4; sx <= 4; sx += 8) for (int sz = -4; sz <= 4; sz += 8)
                set(sx, b + 1, sz, Blocks.ENCHANTING_TABLE.defaultBlockState());
        }

        /** Floor 2: bars around the spawner, a smithy line of workstations along both side walls. */
        void armory(int b, int IN) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (!isBar(dx, dz)) continue;
                BlockState bars = Blocks.IRON_BARS.defaultBlockState()
                        .setValue(BlockStateProperties.NORTH, isBar(dx, dz - 1))
                        .setValue(BlockStateProperties.SOUTH, isBar(dx, dz + 1))
                        .setValue(BlockStateProperties.EAST, isBar(dx + 1, dz))
                        .setValue(BlockStateProperties.WEST, isBar(dx - 1, dz));
                for (int y = b + 1; y <= b + 3; y++) set(dx, y, dz, bars);
            }
            for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++)
                if (Math.max(abs(dx), abs(dz)) == 5) set(dx, b + 1, dz, carpetB);
            BlockState[] st = {Blocks.ANVIL.defaultBlockState(), Blocks.SMITHING_TABLE.defaultBlockState(),
                    Blocks.FLETCHING_TABLE.defaultBlockState(), Blocks.BLAST_FURNACE.defaultBlockState(),
                    Blocks.CAULDRON.defaultBlockState(), Blocks.ANVIL.defaultBlockState()};
            int[] zs = {-6, -3, 3, 6, 9};
            for (int sx = -1; sx <= 1; sx += 2) for (int i = 0; i < zs.length; i++)
                set(sx * IN, b + 1, zs[i], st[(i + (sx > 0 ? 1 : 0)) % st.length]);
        }
        boolean isBar(int dx, int dz) {
            return Math.max(abs(dx), abs(dz)) == 2 && !(dx == 0 && dz == 2);
        }

        /** Floor 3: raised throne at the north end, carpet runner, colonnade. */
        void throneHall(int b, int IN) {
            for (int x = -5; x <= 5; x++) for (int z = -IN; z <= -(IN - 3); z++) set(x, b + 1, z, t.trim);
            for (int x = -2; x <= 2; x++) for (int z = -IN; z <= -(IN - 1); z++) set(x, b + 2, z, t.trim);
            BlockState seat = t.stairs.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            set(0, b + 3, -(IN - 1), seat);
            set(-1, b + 3, -(IN - 1), slabBot);
            set(1, b + 3, -(IN - 1), slabBot);
            for (int y = b + 3; y <= b + 7; y++) set(0, y, -IN, t.pillar);
            set(0, b + 8, -IN, wallPiece);
            set(0, b + 9, -IN, t.light);
            for (int sx = -3; sx <= 3; sx += 6) {
                for (int y = b + 2; y <= b + 6; y++) set(sx, y, -IN, t.pillar);
                set(sx, b + 7, -IN, t.light);
            }
            set(-2, b + 3, -IN, Blocks.GOLD_BLOCK.defaultBlockState());
            set(2, b + 3, -IN, Blocks.GOLD_BLOCK.defaultBlockState());
            set(-5, b + 2, -(IN - 3), flair);
            set(5, b + 2, -(IN - 3), flair);
            for (int z = -(IN - 4); z <= 4; z++) {
                for (int dx = -1; dx <= 1; dx++) set(dx, b + 1, z, carpetA);
                set(-2, b + 1, z, carpetB);
                set(2, b + 1, z, carpetB);
            }
            for (int sx = -4; sx <= 4; sx += 8) for (int z = -4; z <= 4; z += 4) {
                set(sx, b + 1, z, t.trim);
                for (int y = b + 2; y <= b + H - 1; y++) set(sx, y, z, t.pillar);
                set(sx, b + H, z, t.light);
            }
        }

        /** Floor 4: ritual circle on the floor, candles, hanging crystals. */
        void sanctum(int b) {
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 >= 30 && d2 <= 36) {
                    set(dx, b, dz, ((dx + dz) & 1) == 0 ? t.accent : t.floorAlt);
                    if (rnd.nextInt(3) == 0) set(dx, b + 1, dz, candle);
                } else if (d2 >= 12 && d2 <= 16) set(dx, b, dz, t.floorAlt);
            }
            set(-2, b + 1, 0, flair); set(2, b + 1, 0, flair); set(0, b + 1, 2, flair); set(0, b + 1, -2, flair);
            for (int sx = -3; sx <= 3; sx += 6) for (int sz = -3; sz <= 3; sz += 6) {
                for (int y = b + H - 1; y >= b + H - 3; y--) set(sx, y, sz, chain);
                set(sx, b + H - 4, sz, t.light);
            }
        }

        /** Scattered weathering: moss/sprouts/amethyst on the floor, cobwebs near the ceiling. */
        void wear(int k) {
            int b = s(k), IN = rad(k) - 2;
            for (int i = 0; i < 18; i++) {
                int x = rnd.nextInt(2 * IN + 1) - IN, z = rnd.nextInt(2 * IN + 1) - IN;
                if (colInside(x, z) && at(x, b + 1, z).isAir() && !at(x, b, z).isAir()) set(x, b + 1, z, wearBlk);
            }
            for (int i = 0; i < 14; i++) {
                int x = rnd.nextInt(2 * IN + 1) - IN, z = rnd.nextInt(2 * IN + 1) - IN;
                if (colInside(x, z) && at(x, b + H - 2, z).isAir()) set(x, b + H - 2, z, cobweb);
            }
        }

        // ------------------------------------------------------------------ arena

        boolean stairHole(int hs, int hside, int dx, int dz) {
            if (dz != hside * (hs - 3) && dz != hside * (hs - 4)) return false;
            int a = hside * (hs - 2), c = a + (hside < 0 ? 1 : -1) * (H - 2);
            return dx >= Math.min(a, c) && dx <= Math.max(a, c);
        }

        void arena(int sTop) {
            final int rd = rad(FLOORS), IN = rd - 2;
            final int hk = FLOORS - 1, hs = Math.min(rad(hk), rad(FLOORS)), hside = hk % 2 == 0 ? -1 : 1;

            for (int dx = -IN; dx <= IN; dx++) for (int dz = -IN; dz <= IN; dz++) {
                if (stairHole(hs, hside, dx, dz)) continue;
                int r = Math.max(abs(dx), abs(dz));
                if (r <= 1) set(dx, sTop, dz, t.accent);
                else if (r == 2) set(dx, sTop, dz, t.floorAlt);
                else if (r == 3) set(dx, sTop, dz, t.trim);
                else if (r <= IN - 1 && abs(dx) == abs(dz)) set(dx, sTop, dz, t.accent);
                else if (r == IN) set(dx, sTop, dz, t.trim);
            }
            bossTrialSpawner(0, sTop + 1, 0);

            // four braziers around the spawner
            int[][] br = {{4, 0}, {-4, 0}, {0, 4}, {0, -4}};
            for (int[] c : br) { set(c[0], sTop + 1, c[1], t.trim); set(c[0], sTop + 2, c[1], t.lanternStanding); }

            // tall pillars with lit capitals
            int[][] pl = {{7, 5}, {-7, 5}, {7, -3}, {-7, -3}};
            for (int[] c : pl) {
                set(c[0], sTop + 1, c[1], t.trim);
                for (int y = sTop + 2; y <= sTop + 5; y++) set(c[0], y, c[1], t.pillar);
                set(c[0], sTop + 6, c[1], wallPiece);
                set(c[0], sTop + 7, c[1], t.light);
            }
            for (int f = 0; f < 4; f++) for (int tt = -6; tt <= 6; tt += 6) fs(f, tt, rd - 1, sTop + 3, t.light);

            // vault on the south side, framed
            int vz = rd - 3;
            for (int y = sTop + 1; y <= sTop + 4; y++) { set(-1, y, vz, t.pillar); set(1, y, vz, t.pillar); }
            set(0, sTop + 2, vz, t.trim);
            set(0, sTop + 3, vz, t.trim);
            set(0, sTop + 4, vz, t.light);
            BlockPos vp = new BlockPos(cx, sTop + 1, cz + vz);
            if (sTop + 1 <= yMax && box.isInside(vp)) {
                level.setBlock(vp, Blocks.VAULT.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 2);
                blockEntityNbt(vp, "{config:{key_item:{id:\"minecraft:ominous_trial_key\",count:1},"
                        + "loot_table:\"dimtowers:vault/reward\"}}");
            }

            // treasure chests
            int[][] cp = {{9, 6, 0}, {-9, 6, 1}, {9, -3, 0}, {-9, -3, 1}, {9, 2, 0}, {9, -7, 0}};
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
