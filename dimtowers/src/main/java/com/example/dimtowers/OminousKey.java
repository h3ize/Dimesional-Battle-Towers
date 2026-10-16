package com.example.dimtowers;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class OminousKey {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DimensionalBattleTowers.MOD_ID);
    public static final DeferredItem<Item> KEY = ITEMS.register("dimensional_tower_ominous_key",
            () -> new KeyItem(new Item.Properties().rarity(Rarity.EPIC)));

    private static final ResourceLocation VAULT_ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(DimensionalBattleTowers.MOD_ID, "vault_master");
    private static final int CHECK_TICKS = 5;
    private static final List<Pending> PENDING = new ArrayList<>();

    private OminousKey() {}

    private static final class Pending {
        final ServerPlayer player;
        final BlockPos pos;
        final boolean ominousKey;
        int age;

        Pending(ServerPlayer player, BlockPos pos, boolean ominousKey) {
            this.player = player;
            this.pos = pos;
            this.ominousKey = ominousKey;
        }
    }

    public static final class KeyItem extends Item {
        public KeyItem(Properties properties) {
            super(properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.RED);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }
    }
    private static boolean isTowerVault(VaultBlockEntity vault) {
        return vault.getConfig().lootTable().location().getNamespace().equals(DimensionalBattleTowers.MOD_ID);
    }

    private static void clearRewardedPlayers(ServerLevel level, VaultBlockEntity vault) {
        var registries = level.registryAccess();
        CompoundTag tag = vault.saveWithoutMetadata(registries);
        if (!tag.contains("server_data", Tag.TAG_COMPOUND)) return;
        CompoundTag serverData = tag.getCompound("server_data");
        if (!(serverData.get("rewarded_players") instanceof ListTag list) || list.isEmpty()) return;
        serverData.put("rewarded_players", new ListTag());
        vault.loadWithComponents(tag, registries);
        vault.setChanged();
    }
    private static void repairBossVault(ServerLevel level, BlockPos pos, VaultBlockEntity vault) {
        String keyId = BuiltInRegistries.ITEM.getKey(KEY.get()).toString();
        var registries = level.registryAccess();
        CompoundTag tag = vault.saveWithoutMetadata(registries);
        CompoundTag cfg = tag.getCompound("config");
        boolean keyOk = cfg.getCompound("key_item").getString("id").equals(keyId);
        boolean lootOk = cfg.getString("loot_table").startsWith(DimensionalBattleTowers.MOD_ID + ":");
        if (keyOk && lootOk) return;
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, DimensionalBattleTowers.TOWERS);
        if (start == null || !start.isValid()) return;
        String theme = null;
        for (var piece : start.getPieces()) if (piece instanceof TowerPiece tp) { theme = tp.themeId(); break; }
        if (theme == null) return;
        CompoundTag key = new CompoundTag();
        key.putString("id", keyId);
        key.putInt("count", 1);
        cfg.put("key_item", key);
        cfg.putString("loot_table", DimensionalBattleTowers.MOD_ID + ":vault/reward_" + theme);
        tag.put("config", cfg);
        vault.loadWithComponents(tag, registries);
        vault.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }

    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getEntity() instanceof ServerPlayer player) || !(e.getLevel() instanceof ServerLevel level)) return;
        BlockState state = level.getBlockState(e.getPos());
        if (!state.is(Blocks.VAULT)) return;
        if (!(level.getBlockEntity(e.getPos()) instanceof VaultBlockEntity vault)) return;
        if (e.getItemStack().is(KEY.get()) && state.getValue(BlockStateProperties.OMINOUS)) repairBossVault(level, e.getPos(), vault);
        if (!isTowerVault(vault)) return;
        clearRewardedPlayers(level, vault);
        if (state.getValue(BlockStateProperties.VAULT_STATE) != VaultState.ACTIVE) return;
        PENDING.add(new Pending(player, e.getPos().immutable(), e.getItemStack().is(KEY.get())));
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        List<Pending> checking = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Pending p : checking) {
            if (p.player.isRemoved()) continue;
            ServerLevel level = p.player.serverLevel();
            BlockState state = level.getBlockState(p.pos);
            if (state.is(Blocks.VAULT)) {
                VaultState vs = state.getValue(BlockStateProperties.VAULT_STATE);
                if (vs == VaultState.UNLOCKING || vs == VaultState.EJECTING) {
                    if (level.getBlockEntity(p.pos) instanceof VaultBlockEntity vault) clearRewardedPlayers(level, vault);
                    if (p.ominousKey) {
                        AdvancementHolder holder = level.getServer().getAdvancements().get(VAULT_ADVANCEMENT);
                        if (holder != null) p.player.getAdvancements().award(holder, "boss_vault");
                    }
                    continue;
                }
            }
            if (++p.age < CHECK_TICKS) PENDING.add(p);
        }
    }
}
