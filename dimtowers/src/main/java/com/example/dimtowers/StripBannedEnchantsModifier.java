package com.example.dimtowers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StripBannedEnchantsModifier extends LootModifier {
    public static final MapCodec<StripBannedEnchantsModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, StripBannedEnchantsModifier::new));

    public StripBannedEnchantsModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        for (ItemStack stack : loot) {
            ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored == null || stored.isEmpty()) continue;

            ItemEnchantments.Mutable fixed = new ItemEnchantments.Mutable(stored);
            boolean changed = false;
            for (Holder<Enchantment> h : stored.keySet()) {
                if (isBannedName(h)) {
                    fixed.set(h, 0);
                    changed = true;
                }
            }
            if (!changed) continue;

            ItemEnchantments result = fixed.toImmutable();
            if (result.isEmpty()) {
                Registry<Enchantment> registry = context.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                List<Holder.Reference<Enchantment>> allowed = new ArrayList<>();
                registry.holders().filter(h -> !h.is(EnchantmentTags.CURSE) && !isBannedName(h)).forEach(allowed::add);
                if (!allowed.isEmpty()) {
                    Holder.Reference<Enchantment> pick = allowed.get(context.getRandom().nextInt(allowed.size()));
                    ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    m.set(pick, 1 + context.getRandom().nextInt(Math.max(1, pick.value().getMaxLevel())));
                    result = m.toImmutable();
                }
            }
            stack.set(DataComponents.STORED_ENCHANTMENTS, result);
        }
        return loot;
    }
    public static boolean isBannedName(Holder<Enchantment> holder) {
        StringBuilder sb = new StringBuilder();
        holder.unwrapKey().ifPresent(k -> sb.append(k.location()).append(' '));
        Enchantment e = holder.value();
        if (e.description().getContents() instanceof TranslatableContents tc) sb.append(tc.getKey()).append(' ');
        sb.append(e.description().getString());
        String all = sb.toString().toLowerCase(Locale.ROOT);
        return all.contains("error") || all.contains("illegal");
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
