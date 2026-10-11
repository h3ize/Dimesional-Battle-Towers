package com.example.dimtowers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record TowerConfig(String theme) implements FeatureConfiguration {
    public static final Codec<TowerConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("theme").forGetter(TowerConfig::theme)).apply(i, TowerConfig::new));
}
