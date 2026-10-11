package com.example.dimtowers;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = DimensionalTowers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(DimensionalTowers.TOWER_BOSS.get(), WitherSkeletonRenderer::new);
        e.registerEntityRenderer(DimensionalTowers.TOWER_ZOMBIE.get(), ZombieRenderer::new);
        e.registerEntityRenderer(DimensionalTowers.TOWER_SKELETON.get(), SkeletonRenderer::new);
        e.registerEntityRenderer(DimensionalTowers.TOWER_PIGLIN.get(), ctx -> new PiglinRenderer(ctx,
                ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_INNER_ARMOR, ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR, false));
    }
}
