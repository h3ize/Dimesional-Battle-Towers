package com.example.dimtowers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TowerPiece extends StructurePiece {
    /** Distance between floors. */
    static final int H = 12;
    /** Spawner floors below the boss arena. */
    static final int FLOORS = 5;
    /** Outer half-width of the shell (tower is 35 wide, ~39 with buttresses). */
    static final int R = 17;
    /** Half-width of the usable interior (shell is two blocks thick). */
    static final int I = R - 2;
    /** Offset of the four interior pillars from the centre. */
    static final int PIL = 8;
    /** Window / wall-bay centres along each face. */
    static final int[] WIN = {-12, -6, 0, 6, 12};
    /** Interior and exterior pilaster positions along each face (between the windows). */
    static final int[] PILASTER = {-9, -3, 3, 9};
    /** Prop spots on every floor (x, z): clear of the stairs, chests, pillars and vault alcove. */
    static final int[][] SPOTS = {{13, 4}, {13, -4}, {-13, 4}, {-13, -4}, {13, 7}, {13, -7}, {-13, 7}, {-13, -7}};
    /** Every floor is a Trial Spawner room (clear it for a key). */
    static final int[] TRIAL_TOTAL = {6, 8, 10, 12, 6};
    static final int[] TRIAL_SIMUL = {2, 3, 4, 5, 3};
    /** Cooldown after a trial spawner is beaten, in ticks (72000 = 1 hour). */
    static final int SPAWNER_COOLDOWN = 72000;
    /** The boss spawner stays dormant for a long time after the boss dies (1200000 ticks = ~16 hours). */
    static final int BOSS_COOLDOWN = 1200000;
    static final String[] ARMOR_TIER = {"", "chainmail", "iron", "diamond", "netherite"};
    static final String[] TOOL_TIER = {"wooden", "stone", "iron", "diamond", "netherite"};
    /** Armour-stand armour tops out at iron. Duplicates make the common sets more likely. */
    static final String[] STAND_ARMOR = {"leather", "chainmail", "golden", "golden", "iron", "iron", "iron"};
    static final String[] ARMOR_SLOTS = {"boots", "leggings", "chestplate", "helmet"};
    static final String[] STAND_TOOLS = {"iron_sword", "iron_axe", "iron_pickaxe", "iron_shovel", "iron_hoe", "stone_sword",
            "stone_axe", "stone_pickaxe", "golden_sword", "golden_axe", "golden_pickaxe", "bow"};
    /** Rare armour-stand weapons (about one stand in twelve holds one). */
    static final String[] RARE_TOOLS = {"diamond_sword", "diamond_axe", "diamond_pickaxe"};
    static boolean has(int[] arr, int v) { for (int a : arr) if (a == v) return true; return false; }
    static final Set<EntityType<?>> ARMORED = Set.<EntityType<?>>of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.WITHER_SKELETON, EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN, EntityType.PILLAGER, EntityType.VINDICATOR);

    private final String themeId;
    private final int cx, cz, s0;

    public TowerPiece(String themeId, int cx, int cz, int s0) {
        super(DimensionalBattleTowers.TOWER_PIECE.get(), 0,
                new BoundingBox(cx - (R + 4), s0 - 60, cz - (R + 4), cx + (R + 4), s0 + FLOORS * H + 24, cz + (R + 4)));
        this.themeId = themeId; this.cx = cx; this.cz = cz; this.s0 = s0;
    }

    public TowerPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(DimensionalBattleTowers.TOWER_PIECE.get(), tag);
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
                case NETHER, DEEP_DARK -> Blocks.BLACK_CARPET.defaultBlockState();
                case END, DARK_FOREST -> Blocks.PURPLE_CARPET.defaultBlockState();
                case CHERRY -> Blocks.PINK_CARPET.defaultBlockState();
                case OCEAN, WARPED -> Blocks.CYAN_CARPET.defaultBlockState();
                case CRIMSON -> Blocks.RED_CARPET.defaultBlockState();
                case OAK, JUNGLE, SWAMP -> Blocks.GREEN_CARPET.defaultBlockState();
                case PILLAGER -> Blocks.GRAY_CARPET.defaultBlockState();
                case TAIGA, BADLANDS, SAVANNA -> Blocks.ORANGE_CARPET.defaultBlockState();
                default -> Blocks.RED_CARPET.defaultBlockState();
            };
        }

        /** Second carpet colour, used for rug borders and runner edges. */
        BlockState carpetAlt() {
            return switch (t) {
                case NETHER, DEEP_DARK -> Blocks.GRAY_CARPET.defaultBlockState();
                case END, DARK_FOREST -> Blocks.MAGENTA_CARPET.defaultBlockState();
                case CHERRY -> Blocks.WHITE_CARPET.defaultBlockState();
                case OCEAN -> Blocks.LIGHT_BLUE_CARPET.defaultBlockState();
                case WARPED -> Blocks.LIGHT_BLUE_CARPET.defaultBlockState();
                case CRIMSON -> Blocks.BLACK_CARPET.defaultBlockState();
                case OAK, JUNGLE, SWAMP -> Blocks.LIME_CARPET.defaultBlockState();
                case PILLAGER -> Blocks.BLACK_CARPET.defaultBlockState();
                case TAIGA, BADLANDS, SAVANNA -> Blocks.YELLOW_CARPET.defaultBlockState();
                default -> Blocks.WHITE_CARPET.defaultBlockState();
            };
        }

        BlockState stairBlock(Direction facing, boolean upsideDown) {
            BlockState st = t.stairs.defaultBlockState().setValue(StairBlock.FACING, facing);
            return upsideDown ? st.setValue(BlockStateProperties.HALF, Half.TOP) : st;
        }
        BlockState slabDouble() { return t.slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE); }
        BlockState slabTop() { return t.slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP); }
        Direction dirOf(int ddx, int ddz) {
            if (ddx != 0) return ddx > 0 ? Direction.EAST : Direction.WEST;
            return ddz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        /** Direction pointing from an outer cell toward the nearest wall face (used for corbels). */
        Direction towardWall(int dx, int dz) {
            return abs(dx) >= abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
        }

        ResourceKey<LootTable> loot() {
            ArrayList<String> pool = new ArrayList<>(COMMON_LOOT);
            switch (t) {
                case NETHER, PIGLIN, CRIMSON, WARPED -> pool.addAll(NETHER_LOOT);
                case END -> pool.addAll(END_LOOT);
                case PILLAGER, DARK_FOREST -> pool.addAll(ILLAGER_LOOT);
                default -> pool.addAll(OVERWORLD_LOOT);
            }
            return key(pool.get(rnd.nextInt(pool.size())));
        }
        /** Theme loot for the treasury floor, the four top-floor chests. */
        ResourceKey<LootTable> treasureKey() {
            return ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(DimensionalBattleTowers.MOD_ID, "chests/treasure_" + t.id));
        }
        ResourceKey<LootTable> barrelKey() {
            return ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(DimensionalBattleTowers.MOD_ID, "chests/barrel"));
        }
        ResourceKey<LootTable> key(String p) {
            return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace(p));
        }

        /** A single chest with the given loot table, facing the room. */
        void chestAt(BlockPos p, Direction f, ResourceKey<LootTable> lootKey) {
            if (p.getY() > yMax || !box.isInside(p)) return;
            level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f), 2);
            RandomizableContainer.setBlockEntityLootTable(level, rnd, p, lootKey);
        }

        /** Wall-centre chest shrine: pillars either side, a trim lintel and floor lanterns. */
        void chestShrine(int face, int b, ResourceKey<LootTable> lootKey) {
            for (int y = b + 1; y <= b + 3; y++) { fs(face, -1, I, y, t.pillar); fs(face, 1, I, y, t.pillar); }
            fs(face, 0, I, b + 3, t.trim);
            fs(face, -1, I - 1, b + 1, t.lanternStanding);
            fs(face, 1, I - 1, b + 1, t.lanternStanding);
            chestAt(pfs(face, 0, I, b + 1), inward(face), lootKey);
        }

        /** Barrel in a wall nook, lid facing the room, filled from the early-game barrel loot table. */
        void barrelAt(int face, int tt, int y) {
            BlockPos p = pfs(face, tt, I, y);
            if (y > yMax || !box.isInside(p)) return;
            level.setBlock(p, barrel.setValue(BlockStateProperties.FACING, inward(face)), 2);
            RandomizableContainer.setBlockEntityLootTable(level, rnd, p, barrelKey());
        }

        void blockEntityNbt(BlockPos pos, String snbt) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return;
            try {
                be.loadWithComponents(TagParser.parseTag(snbt), level.registryAccess());
                be.setChanged();
            } catch (Exception ignored) { }
        }


        String weaponFor(EntityType<?> type, int k, boolean diamond) {
            String tool = (k == 3 && !diamond) ? "iron" : TOOL_TIER[k];
            if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.BOGGED) return "bow";
            if (type == EntityType.PILLAGER) return "crossbow";
            if (type == EntityType.VINDICATOR) return tool + "_axe";
            if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED
                    || type == EntityType.WITHER_SKELETON) return tool + "_sword";
            if (type == EntityType.ZOMBIFIED_PIGLIN || type == EntityType.PIGLIN) return "golden_sword";
            if (type == EntityType.PIGLIN_BRUTE) return "golden_axe";
            return null;
        }

        /**
         * Spawn-data entity for a floor mob. Gear, potion effects and (on the top floor) extra health scale with height.
         * Because this is more than a bare {id:...}, the spawner will NOT call finalizeSpawn, so weapons are set here.
         */
        String mobNbt(EntityType<?> type, int k, int diamondSlot) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            StringBuilder sb = new StringBuilder("{id:'" + id + "',Tags:['dt_floor']");
            if (k >= 1 && ARMORED.contains(type)) {
                // Floor 3 is iron with at most ONE diamond piece (diamondSlot 0-3 = boots..helmet); no full diamond sets.
                sb.append(",ArmorItems:[");
                for (int i = 0; i < 4; i++) {
                    String tier = k == 3 ? (diamondSlot == i ? "diamond" : "iron") : ARMOR_TIER[k];
                    sb.append(i > 0 ? "," : "").append("{id:'minecraft:").append(tier).append("_").append(ARMOR_SLOTS[i]).append("',count:1}");
                }
                sb.append("],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f]");
            }
            String w = weaponFor(type, k, diamondSlot == 4);
            if (w != null) sb.append(",HandItems:[{id:'minecraft:").append(w).append("',count:1},{}],HandDropChances:[0.0f,0.0f]");
            List<String> fx = new ArrayList<>();
            if (k >= 3) fx.add("{id:'minecraft:speed',amplifier:0b,duration:-1,show_particles:0b}");
            if (k >= 4) {
                fx.add("{id:'minecraft:strength',amplifier:1b,duration:-1,show_particles:0b}");
                fx.add("{id:'minecraft:resistance',amplifier:0b,duration:-1,show_particles:0b}");
            }
            if (!fx.isEmpty()) sb.append(",active_effects:[").append(String.join(",", fx)).append("]");
            if (k >= 4) sb.append(",attributes:[{id:'minecraft:generic.max_health',base:80.0d}],Health:80.0f");
            return sb.append("}").toString();
        }

        /** Spawn entry for a floor spawner. Diamond gear never appears on mobs (floor 3 is plain iron); it only drops from chests. */
        String floorPotentials(EntityType<?> type, int k) {
            return "{weight:1,data:{entity:" + mobNbt(type, k, -1) + "}}";
        }

        /**
         * Clearable floor: a Trial Spawner that spawns ONE mob type (picked per spawner from the floor's list).
         * Kill the wave and it ejects a regular trial key, which opens this floor's vault. Counts, armour and effects
         * scale with the floor number. The ominous config is identical so an omen can't swap in vanilla mobs.
         */
        void trialFloorSpawner(int dx, int y, int dz, int k) {
            BlockPos p = new BlockPos(cx + dx, y, cz + dz);
            if (y > yMax || !box.isInside(p)) return;
            level.setBlock(p, Blocks.TRIAL_SPAWNER.defaultBlockState(), 2);
            List<EntityType<?>> choices = t.mobsForFloor(k);
            EntityType<?> type = choices.get(rnd.nextInt(choices.size()));
            String config = "{spawn_range:6,total_mobs:" + TRIAL_TOTAL[k] + ".0f,simultaneous_mobs:" + TRIAL_SIMUL[k]
                    + ".0f,total_mobs_added_per_player:3.0f,simultaneous_mobs_added_per_player:1.0f,ticks_between_spawn:" + FloorMobs.FLOOR_SPAWN_TICKS + ","
                    + "spawn_potentials:[" + floorPotentials(type, k) + "],"
                    + "loot_tables_to_eject:[{weight:1,data:'minecraft:spawners/trial_chamber/key'}]}";
            blockEntityNbt(p, "{normal_config:" + config + ",ominous_config:" + config
                    + ",required_player_range:14,target_cooldown_length:" + SPAWNER_COOLDOWN + "}");
        }

        /** Boss Trial Spawner: exactly one boss type for this tower (picked at generation), then it ejects the ominous trial key. */
        void bossTrialSpawner(int dx, int y, int dz) {
            BlockPos p = new BlockPos(cx + dx, y, cz + dz);
            if (y > yMax || !box.isInside(p)) return;
            level.setBlock(p, Blocks.TRIAL_SPAWNER.defaultBlockState(), 2);
            String id = t.bosses[rnd.nextInt(t.bosses.length)];
            String pot = "{weight:1,data:{entity:{id:'" + id + "',Tags:['" + t.bossTag() + "']}}}";
            String config = "{spawn_range:5,total_mobs:1.0f,simultaneous_mobs:1.0f,"
                    + "total_mobs_added_per_player:0.0f,simultaneous_mobs_added_per_player:0.0f,ticks_between_spawn:40,"
                    + "spawn_potentials:[" + pot + "],"
                    + "loot_tables_to_eject:[{weight:1,data:'dimtowers:spawners/boss_key'}]}";
            blockEntityNbt(p, "{normal_config:" + config + ",ominous_config:" + config
                    + ",required_player_range:12,target_cooldown_length:" + BOSS_COOLDOWN + "}");
        }


        BlockPos pfs(int face, int tt, int r, int y) {
            return switch (face) {
                case 0 -> new BlockPos(cx + tt, y, cz + r);
                case 1 -> new BlockPos(cx + tt, y, cz - r);
                case 2 -> new BlockPos(cx + r, y, cz + tt);
                default -> new BlockPos(cx - r, y, cz + tt);
            };
        }
        /** Direction facing into the room from a given wall face. */
        Direction inward(int face) {
            return switch (face) { case 0 -> Direction.NORTH; case 1 -> Direction.SOUTH; case 2 -> Direction.WEST; default -> Direction.EAST; };
        }

        /** Places a block (and optional block-entity NBT) only when it is inside this chunk's box. */
        void setBe(BlockPos p, BlockState st, String nbt) {
            if (p.getY() > yMax || !box.isInside(p)) return;
            level.setBlock(p, st, 2);
            if (nbt != null) blockEntityNbt(p, nbt);
        }

        /** Loads an entity from SNBT at the given position, only when that position is inside this chunk's box. */
        void entity(String snbt, double x, double y, double z) {
            if (y > yMax || !box.isInside(BlockPos.containing(x, y, z))) return;
            try {
                CompoundTag tag = TagParser.parseTag(snbt);
                ListTag pos = new ListTag();
                pos.add(DoubleTag.valueOf(x)); pos.add(DoubleTag.valueOf(y)); pos.add(DoubleTag.valueOf(z));
                tag.put("Pos", pos);
                Optional<Entity> e = EntityType.create(tag, level.getLevel());
                e.ifPresent(level::addFreshEntity);
            } catch (Exception ignored) { }
        }

        /**
         * Armour stand with random gear: usually one full set (iron at best), sometimes a mix of two materials,
         * occasionally a missing piece, plus a random tool that is only rarely diamond. No netherite, no trims.
         */
        void armorStand(int dx, int y, int dz) {
            String base = STAND_ARMOR[rnd.nextInt(STAND_ARMOR.length)];
            String other = STAND_ARMOR[rnd.nextInt(STAND_ARMOR.length)];
            boolean mixed = !other.equals(base) && rnd.nextInt(10) < 5;
            StringBuilder armor = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                if (i > 0) armor.append(',');
                if (rnd.nextInt(12) == 0) { armor.append("{}"); continue; }
                String mat = mixed && rnd.nextBoolean() ? other : base;
                armor.append("{id:'minecraft:").append(mat).append('_').append(ARMOR_SLOTS[i]).append("',count:1}");
            }
            String tool = STAND_TOOLS[rnd.nextInt(STAND_TOOLS.length)];
            float yaw = dx > 0 ? 90f : 270f;
            entity("{id:'minecraft:armor_stand',ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,DisabledSlots:4144959,Rotation:[" + yaw + "f,0f],"
                    + "ArmorItems:[" + armor + "],HandItems:[{id:'minecraft:" + tool + "',count:1},{}]}", cx + dx + 0.5, y, cz + dz + 0.5);
        }

        void decoratedPot(int dx, int y, int dz, String contents) {
            String sh = "'minecraft:" + t.sherd + "_pottery_sherd'";
            String plain = "'minecraft:brick'";
            String sherds = rnd.nextBoolean() ? sh + "," + plain + "," + sh + "," + plain : plain + "," + sh + "," + plain + "," + sh;
            setBe(new BlockPos(cx + dx, y, cz + dz), Blocks.DECORATED_POT.defaultBlockState(),
                    "{sherds:[" + sherds + "],item:{id:'" + contents + "',count:" + (2 + rnd.nextInt(5)) + "}}");
        }


        /** Barrel standing on the floor, lid up, filled from the barrel loot table. */
        void propBarrel(int dx, int y, int dz) {
            BlockPos p = new BlockPos(cx + dx, y, cz + dz);
            if (y > yMax || !box.isInside(p)) return;
            level.setBlock(p, barrel.setValue(BlockStateProperties.FACING, Direction.UP), 2);
            RandomizableContainer.setBlockEntityLootTable(level, rnd, p, barrelKey());
        }

        /** Floor 1 (k=0): armory. Armour stands with random gear, candle tables and supply barrels. */
        void armory(int b) {
            for (int i = 0; i < SPOTS.length; i++) {
                int x = SPOTS[i][0], z = SPOTS[i][1];
                switch (i % 4) {
                    case 0, 2 -> armorStand(x, b + 1, z);
                    case 1 -> { set(x, b + 1, z, slabDouble()); set(x, b + 2, z, candle); }
                    default -> propBarrel(x, b + 1, z);
                }
            }
        }

        /** Floor 2 (k=1): library. Reading nooks with lecterns, desks lit by candles, bookshelf stacks, chiseled shelves. */
        void library(int b) {
            for (int i = 0; i < SPOTS.length; i++) {
                int x = SPOTS[i][0], z = SPOTS[i][1];
                int side = x > 0 ? 1 : -1;
                Direction toRoom = side > 0 ? Direction.WEST : Direction.EAST;
                switch (i % 4) {
                    case 0 -> {
                        set(x, b + 1, z, Blocks.LECTERN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, toRoom));
                        for (int y = b + 1; y <= b + 3; y++) set(side * I, y, z, bookshelf);
                    }
                    case 1 -> { set(x, b + 1, z, slabDouble()); set(x, b + 2, z, candle); }
                    case 2 -> { for (int y = b + 1; y <= b + 3; y++) set(x, y, z, bookshelf); }
                    default -> {
                        BlockState cb = Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, toRoom);
                        set(x, b + 1, z, cb);
                        set(x, b + 2, z, cb);
                        set(x, b + 3, z, candle);
                    }
                }
            }
        }

        /** Floor 3 (k=2): candle hall. Lanterns and candlesticks alternating on trim-and-pillar pedestals. */
        void trophyHall(int b) {
            for (int i = 0; i < SPOTS.length; i++) {
                int x = SPOTS[i][0], z = SPOTS[i][1];
                set(x, b + 1, z, t.trim);
                set(x, b + 2, z, t.pillar);
                set(x, b + 3, z, i % 2 == 0 ? t.lanternStanding : candle);
            }
        }

        /** Floor 4 (k=3): treasury. Decorated pots on gold plinths, with the theme's loot in the chests. */
        void treasury(int b) {
            for (int i = 0; i < SPOTS.length; i++) {
                int x = SPOTS[i][0], z = SPOTS[i][1];
                String[] loot = {"minecraft:emerald", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:amethyst_shard"};
                if (i % 2 == 0) {
                    decoratedPot(x, b + 1, z, loot[(i / 2) % 4]);
                } else {
                    set(x, b + 1, z, Blocks.GOLD_BLOCK.defaultBlockState());
                    decoratedPot(x, b + 2, z, loot[(i / 2) % 4]);
                }
            }
        }

        /** Floor 5 (k=4): champion hall. Armour stands with random gear (never netherite) between lantern braziers. */
        void championHall(int b) {
            for (int i = 0; i < SPOTS.length; i++) {
                int x = SPOTS[i][0], z = SPOTS[i][1];
                if (i % 2 == 0) {
                    armorStand(x, b + 1, z);
                } else {
                    set(x, b + 1, z, t.trim);
                    set(x, b + 2, z, t.pillar);
                    set(x, b + 3, z, t.lanternStanding);
                }
            }
        }

        /** Candle shelves on the interior pilasters, left out in front of the staircase. */
        void dress(int k, int b) {
            final int stairFace = k % 2 == 0 ? 1 : 0;
            final int stairTt = k % 2 == 0 ? -3 : 3;
            for (int f = 0; f < 4; f++) for (int tt : new int[]{-3, 3}) {
                if (f == stairFace && tt == stairTt) continue;
                BlockPos fp = pfs(f, tt, I - 1, b + 4);
                int ddx = fp.getX() - cx, ddz = fp.getZ() - cz;
                set(ddx, b + 4, ddz, slabTop());
                set(ddx, b + 5, ddz, candle);
            }
        }

        /** Floors 2-5: this floor's vault (opens with the key the floor's Trial Spawner ejects), framed like the boss vault. */
        void floorVault(int k, int b, int vf) {
            for (int y = b + 1; y <= b + 3; y++) { fs(vf, -1, I, y, t.pillar); fs(vf, 1, I, y, t.pillar); }
            fs(vf, 0, I, b + 2, t.trim);
            fs(vf, 0, I, b + 3, t.trim);
            setBe(pfs(vf, 0, I, b + 1), Blocks.VAULT.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, inward(vf)),
                    "{config:{key_item:{id:'minecraft:trial_key',count:1},loot_table:'dimtowers:vault/floor_" + t.id + "'}}");
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

        /** True if (dx, dz) lies in the footprint of floor k's staircase (nothing should be laid under it). */
        boolean underStairs(int k, int dx, int dz) {
            boolean even = k % 2 == 0;
            int zRow = even ? -(R - 3) : (R - 3), zIn = even ? -(R - 4) : (R - 4);
            int x0 = even ? -(H - 1) : 0, x1 = even ? 0 : (H - 1);
            return (dz == zRow || dz == zIn) && dx >= x0 && dx <= x1;
        }

        /** Runners, a patterned ring round the dais and bordered rugs under each group of props. */
        void carpets(int b, int k) {
            BlockState main = carpet(), alt = carpetAlt();
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                if (Math.max(abs(dx), abs(dz)) == 4) set(dx, b + 1, dz, ((dx + dz) & 1) == 0 ? main : alt);
            }
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                for (int x = I - 3; x <= I - 1; x++) for (int z = 3; z <= 8; z++) {
                    boolean edge = x == I - 3 || x == I - 1 || z == 3 || z == 8;
                    set(sx * x, b + 1, sz * z, edge ? alt : main);
                }
            }
            // North-south runner: runs right up to the wall at both ends (underStairs keeps it off the staircase).
            for (int dz = -I; dz <= (k == 0 ? R : I); dz++) {
                if (abs(dz) <= 3) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    if (underStairs(k, dx, dz)) continue;
                    set(dx, b + 1, dz, dx == 0 ? main : alt);
                }
            }
            for (int dx = -I; dx <= I; dx++) {
                if (abs(dx) <= 3) continue;
                for (int dz = -1; dz <= 1; dz++) set(dx, b + 1, dz, dz == 0 ? main : alt);
            }
        }

        BlockState pane(int face) {
            BlockState st = t.glass;
            return face < 2
                    ? st.setValue(BlockStateProperties.EAST, Boolean.TRUE).setValue(BlockStateProperties.WEST, Boolean.TRUE)
                    : st.setValue(BlockStateProperties.NORTH, Boolean.TRUE).setValue(BlockStateProperties.SOUTH, Boolean.TRUE);
        }

        /**
         * Open, floating two-wide staircase running along the wall: bare stair blocks with nothing underneath, no
         * balustrade and no stringer. The shaft above every step is fully cleared so nothing pokes into the floor above.
         * Safe to call again after the rest of the tower is built.
         */
        void stairs(int k) {
            int b = s(k);
            boolean even = k % 2 == 0;
            int zRow = even ? -(R - 3) : (R - 3), zIn = even ? -(R - 4) : (R - 4);
            Direction dir = even ? Direction.EAST : Direction.WEST;
            int startX = even ? -(H - 1) : (H - 1), step = even ? 1 : -1;
            final BlockState stair = stairBlock(dir, false);
            for (int i = 0; i < H; i++) {
                int x = startX + step * i;
                int y = b + 1 + i;
                int open = i < H - 1 ? b + H + 3 : y + 3;
                for (int z : new int[]{zRow, zIn}) {
                    for (int yy = b + 1; yy < y; yy++) clr(x, yy, z);
                    // The runner passes under the raised steps: keep it continuous instead of leaving bare floor.
                    if (i >= 1 && abs(x) <= 1) set(x, b + 1, z, x == 0 ? carpet() : carpetAlt());
                    set(x, y, z, stair);
                    for (int yy = y + 1; yy <= open; yy++) clr(x, yy, z);
                }
            }
            int endX = startX + step * (H - 1);
            for (int i = 1; i <= 3; i++) for (int z : new int[]{zRow, zIn}) {
                for (int yy = b + H + 1; yy <= b + H + 3; yy++) clr(endX + step * i, yy, z);
            }
        }

        /** Carpet at the foot of a floor's staircase. */
        void stairFoot(int k) {
            int b = s(k);
            boolean even = k % 2 == 0;
            int zRow = even ? -(R - 3) : (R - 3), zIn = even ? -(R - 4) : (R - 4);
            int startX = even ? -(H - 1) : (H - 1), step = even ? 1 : -1;
            int footX = startX - step;
            set(footX, b + 1, zRow, carpetAlt());
            set(footX, b + 1, zIn, carpetAlt());
        }

        void build() {
            final int sTop = s(FLOORS);
            final int top = Math.min(sTop + 24, yMax);
            final BlockState slabTop = t.slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            final BlockState slabBot = t.slab.defaultBlockState();
            final BlockState wallPiece = t.wallPiece.defaultBlockState();

            for (int dx = -(R + 3); dx <= R + 3; dx++) for (int dz = -(R + 3); dz <= R + 3; dz++) {
                if (!colInside(dx, dz)) continue;
                for (int y = s0; y <= top; y++) clr(dx, y, dz);
            }

            for (int dx = -(R + 2); dx <= R + 2; dx++) for (int dz = -(R + 2); dz <= R + 2; dz++) {
                if (!colInside(dx, dz)) continue;
                int depth = 58 - 2 * Math.max(abs(dx), abs(dz));
                for (int i = 0; i <= depth; i++) {
                    int y = s0 - i;
                    if (y < level.getMinBuildHeight()) break;
                    BlockState st = at(dx, y, dz);
                    if (i > 0 && !(st.isAir() || !st.getFluidState().isEmpty() || st.canBeReplaced())) break;
                    set(dx, y, dz, t.plinth);
                }
            }

            for (int k = 0; k <= FLOORS; k++)
                for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++)
                    set(dx, s(k), dz, floorAt(dx, dz));

            for (int y = s0 + 1; y <= sTop + 5; y++)
                for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                    int r = Math.max(abs(dx), abs(dz));
                    if (r < R - 1) continue;
                    boolean base = y == s0 + 1 || (r == R && y <= s0 + 3);
                    set(dx, y, dz, base ? t.plinth : wallBlock());
                }
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                if (Math.max(abs(dx), abs(dz)) != R) continue;
                for (int k = 1; k <= FLOORS; k++) set(dx, s(k), dz, t.trim);
                if (((dx + dz) & 1) == 0) set(dx, sTop + 6, dz, t.wall);
            }

            for (int dx = -(R + 2); dx <= R + 2; dx++) for (int dz = -(R + 2); dz <= R + 2; dz++) {
                int r = Math.max(abs(dx), abs(dz));
                if (r == R + 1) {
                    for (int k = 1; k <= FLOORS; k++) {
                        set(dx, s(k), dz, slabTop);
                        set(dx, s(k) - 1, dz, stairBlock(towardWall(dx, dz), true));
                    }
                    set(dx, sTop + 5, dz, stairBlock(towardWall(dx, dz), true));
                    set(dx, sTop + 6, dz, slabBot);
                    if (!(dz == R + 1 && abs(dx) <= 4)) {
                        set(dx, s0 + 1, dz, t.plinth);
                        set(dx, s0 + 2, dz, stairBlock(towardWall(dx, dz), false));
                    }
                } else if (r == R + 2) {
                    if (!(dz == R + 2 && abs(dx) <= 4)) set(dx, s0 + 1, dz, slabBot);
                }
            }

            for (int f = 0; f < 4; f++) for (int tt : PILASTER) {
                for (int y = s0 + 1; y <= sTop + 5; y++) fs(f, tt, R + 1, y, t.pillar);
                fs(f, tt, R + 1, sTop + 6, wallPiece);
                fs(f, tt, R + 1, sTop + 7, t.light);
                if (f == 0 && abs(tt) == 3) continue;
                for (int y = s0 + 1; y <= s0 + 4; y++) fs(f, tt, R + 2, y, t.pillar);
                fs(f, tt, R + 2, s0 + 5, stairBlock(inward(f), false));
            }

            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                for (int ax = R - 1; ax <= R + 1; ax++) for (int az = R - 1; az <= R + 1; az++) {
                    boolean corner = ax == R + 1 && az == R + 1;
                    for (int y = s0 + 1; y <= sTop + 8; y++) set(sx * ax, y, sz * az, corner ? t.pillar : wallBlock());
                    if (!corner) for (int k = 1; k <= FLOORS; k++) set(sx * ax, s(k), sz * az, t.trim);
                    set(sx * ax, sTop + 9, sz * az, t.trim);
                    if (ax == R && az == R) set(sx * ax, sTop + 10, sz * az, t.wall);
                    else set(sx * ax, sTop + 10, sz * az, stairBlock(towardWall(sx * (ax - R), sz * (az - R)), false));
                }
                for (int y = sTop + 11; y <= sTop + 15; y++) set(sx * R, y, sz * R, t.pillar);
                set(sx * R, sTop + 16, sz * R, wallPiece);
                set(sx * R, sTop + 17, sz * R, t.light);
                set(sx * R, sTop + 18, sz * R, Blocks.BELL.defaultBlockState());
            }

            for (int k = 0; k < FLOORS; k++) for (int f = 0; f < 4; f++) for (int tt : WIN) {
                if (k == 0 && f == 0 && tt == 0) continue;
                int b = s(k);
                BlockState glass = pane(f);
                for (int w = -1; w <= 1; w++) {
                    int c = tt + w;
                    for (int y = b + 3; y <= b + 8; y++) { fs(f, c, R, y, glass); fs(f, c, R - 1, y, air); }
                    if (w == 0) { fs(f, c, R, b + 9, glass); fs(f, c, R - 1, b + 9, air); }
                    else fs(f, c, R, b + 9, t.trim);
                    fs(f, c, R, b + 10, t.trim);
                    fs(f, c, R, b + 2, t.trim);
                    fs(f, c, R + 1, b + 2, stairBlock(inward(f), false));
                }
                fs(f, tt, R + 1, b + 10, t.lanternHanging);
                for (int y = b + 3; y <= b + 9; y++) {
                    if (abs(tt - 2) < R - 1) fs(f, tt - 2, R, y, t.pillar);
                    if (abs(tt + 2) < R - 1) fs(f, tt + 2, R, y, t.pillar);
                }
            }

            for (int dx = -2; dx <= 2; dx++) for (int y = s0 + 1; y <= s0 + 7; y++) { clr(dx, y, R - 1); clr(dx, y, R); }
            for (int dx = -1; dx <= 1; dx++) { clr(dx, s0 + 8, R - 1); clr(dx, s0 + 8, R); }
            for (int z = R - 1; z <= R + 1; z++) {
                set(-2, s0 + 8, z, t.trim); set(2, s0 + 8, z, t.trim);
                for (int dx = -2; dx <= 2; dx++) set(dx, s0 + 9, z, t.trim);
                for (int y = s0 + 1; y <= s0 + 9; y++) { set(-3, y, z, t.pillar); set(3, y, z, t.pillar); }
            }
            set(-3, s0 + 10, R + 1, t.light); set(3, s0 + 10, R + 1, t.light);

            for (int dx = -4; dx <= 4; dx++) for (int dz = R + 1; dz <= R + 4; dz++) {
                if (!colInside(dx, dz)) continue;
                for (int i = 0; i <= 8; i++) {
                    int y = s0 - i;
                    if (y < level.getMinBuildHeight()) break;
                    if (i > 0) {
                        BlockState st = at(dx, y, dz);
                        if (!(st.isAir() || !st.getFluidState().isEmpty() || st.canBeReplaced())) break;
                    }
                    set(dx, y, dz, i > 0 ? t.plinth : (abs(dx) + dz) % 2 == 0 ? t.floor : t.floorAlt);
                }
                if (dz > R + 2) for (int y = s0 + 1; y <= s0 + 7; y++) clr(dx, y, dz);
            }
            for (int sx = -4; sx <= 4; sx += 8) {
                set(sx, s0 + 1, R + 4, t.trim);
                set(sx, s0 + 2, R + 4, t.pillar);
                set(sx, s0 + 3, R + 4, t.lanternStanding);
            }

            for (int k = 0; k < FLOORS; k++) {
                int b = s(k);
                final int vf = k % 2 == 1 ? 1 : 0;

                for (int i = -PIL; i <= PIL; i++) {
                    set(i, b + H - 1, PIL, t.trim); set(i, b + H - 1, -PIL, t.trim);
                    set(PIL, b + H - 1, i, t.trim); set(-PIL, b + H - 1, i, t.trim);
                    set(i, b + H - 1, 0, t.trim);   set(0, b + H - 1, i, t.trim);
                }

                for (int sx = -PIL; sx <= PIL; sx += 2 * PIL) for (int sz = -PIL; sz <= PIL; sz += 2 * PIL) {
                    set(sx, b + 1, sz, t.trim);
                    for (int y = b + 2; y <= b + H - 2; y++) set(sx, y, sz, t.pillar);
                    set(sx, b + H - 1, sz, t.trim);
                    set(sx, b + H, sz, t.light);
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        Direction toPillar = dirOf(-d[0], -d[1]);
                        set(sx + d[0], b + 1, sz + d[1], stairBlock(toPillar, false));
                        set(sx + d[0], b + H - 2, sz + d[1], stairBlock(toPillar, true));
                    }
                }

                int[][] ch = {{0, PIL}, {0, -PIL}, {PIL, 0}, {-PIL, 0}};
                for (int[] c : ch) {
                    set(c[0], b + H - 2, c[1], chain);
                    set(c[0], b + H - 3, c[1], chain);
                    set(c[0], b + H - 4, c[1], t.lanternHanging);
                }
                for (int[] c : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
                    set(c[0], b + H - 2, c[1], chain);
                    set(c[0], b + H - 3, c[1], t.lanternHanging);
                }
                for (int y = b + H - 2; y >= b + H - 5; y--) set(0, y, 0, chain);
                set(0, b + H - 6, 0, t.lanternHanging);

                carpets(b, k);

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
                trialFloorSpawner(0, b + 1, 0, k);

                for (int sx = -3; sx <= 3; sx += 6) for (int sz = -6; sz <= 6; sz += 12) {
                    set(sx, b + 1, sz, t.trim);
                    set(sx, b + 2, sz, t.lanternStanding);
                }

                for (int f = 0; f < 4; f++) {
                    Direction outward = inward(f).getOpposite();
                    for (int tt = -I; tt <= I; tt++) {
                        fs(f, tt, I, b + H - 1, stairBlock(outward, true));
                        if (has(WIN, tt) || has(PILASTER, tt)) continue;
                        if (k == 0 && f == 0 && abs(tt) <= 2) continue;
                        fs(f, tt, I, b + 1, stairBlock(outward, false));
                    }
                }

                interiorPilasters(b + 1, b + H - 1);
                cornerPosts(b);

                for (int f = 0; f < 4; f++) for (int tt : WIN) {
                    if (k == 0 && f == 0 && tt == 0) continue;
                    if (f >= 2 && tt == 0) continue;
                    if (k >= 1 && tt == 0 && f == vf) continue;
                    if (k == 2 && rnd.nextInt(5) == 0) {
                        fs(f, tt, I, b + 1, Blocks.INFESTED_STONE_BRICKS.defaultBlockState());
                        fs(f, tt, I, b + 2, Blocks.INFESTED_STONE_BRICKS.defaultBlockState());
                    } else if (rnd.nextInt(3) == 0) {
                        barrelAt(f, tt, b + 1);
                        barrelAt(f, tt, b + 2);
                    } else {
                        fs(f, tt, I, b + 1, bookshelf);
                        fs(f, tt, I, b + 2, bookshelf);
                    }
                    fs(f, tt, I, b + 3, candle);
                }

                stairs(k);
                stairFoot(k);

                chestShrine(2, b, k == 3 ? treasureKey() : loot());
                chestShrine(3, b, k == 3 ? treasureKey() : loot());

                switch (k) {
                    case 0 -> armory(b);
                    case 1 -> library(b);
                    case 2 -> trophyHall(b);
                    case 3 -> treasury(b);
                    default -> championHall(b);
                }
                dress(k, b);
                if (k >= 1) floorVault(k, b, vf);
            }

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
            int[][] br = {{6, 0}, {-6, 0}, {0, 6}, {0, -6}};
            for (int[] c : br) { set(c[0], sTop + 1, c[1], t.trim); set(c[0], sTop + 2, c[1], t.lanternStanding); }
            for (int f = 0; f < 4; f++) for (int tt : WIN) fs(f, tt, R - 1, sTop + 3, t.light);
            interiorPilasters(sTop + 1, sTop + 4);
            cornerPosts(sTop);

            for (int y = sTop + 1; y <= sTop + 3; y++) { set(-1, y, I, t.pillar); set(1, y, I, t.pillar); }
            set(0, sTop + 2, I, t.trim);
            set(0, sTop + 3, I, t.trim);
            BlockPos vp = new BlockPos(cx, sTop + 1, cz + I);
            if (sTop + 1 <= yMax && box.isInside(vp)) {
                level.setBlock(vp, Blocks.VAULT.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                        .setValue(BlockStateProperties.OMINOUS, true), 2);
                blockEntityNbt(vp, "{config:{key_item:{id:\"minecraft:ominous_trial_key\",count:1},"
                        + "loot_table:\"dimtowers:vault/reward_" + t.id + "\"}}");
            }

            // Treasure chests in the three centre bays (2nd, 3rd, 4th of the five) on the east and west walls.
            for (int side = 0; side < 2; side++) {
                int wx = side == 0 ? I : -I;
                Direction facing = side == 0 ? Direction.WEST : Direction.EAST;
                for (int tt : new int[]{-6, 0, 6}) {
                    BlockPos p = new BlockPos(cx + wx, sTop + 1, cz + tt);
                    if (sTop + 1 > yMax || !box.isInside(p)) continue;
                    level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
                    RandomizableContainer.setBlockEntityLootTable(level, rnd, p, treasureKey());
                }
            }

            for (int k = 0; k < FLOORS; k++) stairs(k);
        }
    }
}
