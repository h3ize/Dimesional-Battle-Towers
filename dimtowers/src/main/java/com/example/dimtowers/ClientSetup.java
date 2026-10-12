package com.example.dimtowers;

import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = DimensionalBattleTowers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_BOSS.get(), WitherSkeletonRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_ZOMBIE.get(), ZombieRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_SKELETON.get(), SkeletonRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_PIGLIN.get(), TowerPiglinRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_VINDICATOR.get(), VindicatorRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_BLAZE.get(), BlazeRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_GUARDIAN.get(), GuardianRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_EVOKER.get(), EvokerRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_RAVAGER.get(), RavagerRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_ENDER.get(), EndermanRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_SLIME.get(), SlimeRenderer::new);
        e.registerEntityRenderer(DimensionalBattleTowers.TOWER_DROWNED.get(), DrownedRenderer::new);
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tintIndex) -> 0xFFFF3C3C, OminousKey.KEY.get());
    }
}
