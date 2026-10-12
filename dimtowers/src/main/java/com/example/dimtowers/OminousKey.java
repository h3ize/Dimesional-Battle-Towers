package com.example.dimtowers;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
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
        int age;

        Pending(ServerPlayer player, BlockPos pos) {
            this.player = player;
            this.pos = pos;
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

    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getEntity() instanceof ServerPlayer player) || !e.getItemStack().is(KEY.get())) return;
        BlockState state = e.getLevel().getBlockState(e.getPos());
        if (!state.is(Blocks.VAULT) || state.getValue(BlockStateProperties.VAULT_STATE) != VaultState.ACTIVE) return;
        PENDING.add(new Pending(player, e.getPos().immutable()));
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
                    AdvancementHolder holder = level.getServer().getAdvancements().get(VAULT_ADVANCEMENT);
                    if (holder != null) p.player.getAdvancements().award(holder, "boss_vault");
                    continue;
                }
            }
            if (++p.age < CHECK_TICKS) PENDING.add(p);
        }
    }
}
