package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.enchanting.AstralEnchantingTableBlockEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The floating book over the Astral Enchanting Table: Vanilla's enchanting-table book, same animation. */
public class AstralEnchantingTableRenderer implements BlockEntityRenderer<AstralEnchantingTableBlockEntity, EnchantTableRenderState> {
    private final SpriteGetter sprites;
    private final BookModel bookModel;

    public AstralEnchantingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public EnchantTableRenderState createRenderState() {
        return new EnchantTableRenderState();
    }

    @Override
    public void extractRenderState(AstralEnchantingTableBlockEntity entity, EnchantTableRenderState state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.flip = Mth.lerp(partialTicks, entity.oFlip, entity.flip);
        state.open = Mth.lerp(partialTicks, entity.oOpen, entity.open);
        state.time = entity.time + partialTicks;
        float turn = entity.rot - entity.oRot;
        while (turn >= (float) Math.PI) turn -= (float) (Math.PI * 2);
        while (turn < (float) -Math.PI) turn += (float) (Math.PI * 2);
        state.yRot = entity.oRot + turn * partialTicks;
    }

    @Override
    public void submit(EnchantTableRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.75F, 0.5F);
        poseStack.translate(0.0F, 0.1F + Mth.sin(state.time * 0.1F) * 0.01F, 0.0F);
        poseStack.rotate(Axis.YP, -state.yRot);
        poseStack.rotateDegrees(Axis.ZP, 80.0F);
        float ff1 = Mth.frac(state.flip + 0.25F) * 1.6F - 0.3F;
        float ff2 = Mth.frac(state.flip + 0.75F) * 1.6F - 0.3F;
        BookModel.State book = BookModel.State.forAnimation(state.time, Mth.clamp(ff1, 0.0F, 1.0F), Mth.clamp(ff2, 0.0F, 1.0F), state.open);
        collector.submitModel(this.bookModel, book, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
                EnchantTableRenderer.BOOK_TEXTURE, this.sprites, 0);
        poseStack.popPose();
    }
}
