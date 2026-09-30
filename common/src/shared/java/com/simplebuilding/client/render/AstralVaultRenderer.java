package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.blocks.custom.AstralVaultBlock;
import com.simplebuilding.version.McClientVersion;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Delegates vanilla chests unchanged; the vault shares the vanilla animated lid model. */
public class AstralVaultRenderer extends ChestRenderer<EnderChestBlockEntity> {
    private static final SpriteId SPRITE = Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("simplebuilding", "astral_vault"));
    private final SpriteGetter sprites;
    private final ChestModel model;
    public AstralVaultRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        sprites = context.sprites();
        model = LAYERS.map(layer -> new ChestModel(context.bakeLayer(layer))).select(ChestType.SINGLE);
    }
    public static class State extends ChestRenderState { public boolean vault; }
    @Override public ChestRenderState createRenderState() { return new State(); }
    @Override public void extractRenderState(EnderChestBlockEntity chest, ChestRenderState state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        super.extractRenderState(chest, state, partialTicks, camera, overlay);
        ((State) state).vault = chest.getBlockState().getBlock() instanceof AstralVaultBlock;
    }
    @Override public void submit(ChestRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!((State) state).vault) { super.submit(state, pose, collector, camera); return; }
        pose.pushPose();
        pose.mulPose(ChestRenderer.modelTransformation(state.facing));
        float closed = 1.0F - state.open;
        McClientVersion.submitChestModel(collector, model, 1.0F - closed * closed * closed, pose,
                state.lightCoords, SPRITE, sprites, state.breakProgress);
        pose.popPose();
    }
}
