package com.example.dimtowers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class MaxEnchantFunction extends LootItemConditionalFunction {
    public static final MapCodec<MaxEnchantFunction> CODEC =
            RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, MaxEnchantFunction::new));

    protected MaxEnchantFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return DimensionalBattleTowers.MAX_ENCHANT.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Registry<Enchantment> registry = context.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        registry.holders().forEach(holder -> {
            Enchantment enchantment = holder.value();
            if (!enchantment.isSupportedItem(stack)) return;
            if (holder.is(EnchantmentTags.CURSE)) return;
            if (holder.is(Enchantments.SILK_TOUCH)) return;
            enchants.set(holder, enchantment.getMaxLevel());
        });
        EnchantmentHelper.setEnchantments(stack, enchants.toImmutable());
        return stack;
    }
}
