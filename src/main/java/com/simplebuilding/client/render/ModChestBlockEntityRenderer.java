package com.simplebuilding.client.render;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.ModChestBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.model.ChestBlockModel;
import net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ModChestBlockEntityRenderer implements BlockEntityRenderer<ModChestBlockEntity, ModChestBlockEntityRenderer.State> {

    private static final Identifier REINFORCED_TEXTURE = Identifier.of(Simplebuilding.MOD_ID, "textures/entity/chest/reinforced_chest.png");
    private static final Identifier NETHERITE_TEXTURE = Identifier.of(Simplebuilding.MOD_ID, "textures/entity/chest/netherite_chest.png");

    private final ChestBlockModel model;

    public ModChestBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        ModelPart modelPart = context.getLayerModelPart(EntityModelLayers.CHEST);
        this.model = new ChestBlockModel(modelPart);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(ModChestBlockEntity entity, State state, float tickDelta, Vec3d cameraPos, @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        // Super-Aufruf füllt lightmapCoordinates usw.
        BlockEntityRenderer.super.updateRenderState(entity, state, tickDelta, cameraPos, crumblingOverlay);

        BlockState blockState = entity.hasWorld() ? entity.getCachedState() : ModBlocks.REINFORCED_CHEST.getDefaultState().with(ChestBlock.FACING, Direction.SOUTH);

        Direction facing = blockState.contains(ChestBlock.FACING) ? blockState.get(ChestBlock.FACING) : Direction.SOUTH;
        state.rotation = -facing.getPositiveHorizontalDegrees();

        float progress = entity.getAnimationProgress(tickDelta);
        progress = 1.0F - progress;
        progress = 1.0F - progress * progress * progress;
        state.openFactor = progress;

        if (blockState.isOf(ModBlocks.NETHERITE_CHEST)) {
            state.texture = NETHERITE_TEXTURE;
        } else {
            state.texture = REINFORCED_TEXTURE;
        }
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.rotation));
        matrices.translate(-0.5F, -0.5F, -0.5F);

        RenderLayer renderLayer = RenderLayers.entityCutout(state.texture);

        queue.submitModel(
                this.model,
                state.openFactor,
                matrices,
                renderLayer,
                state.lightmapCoordinates, // Korrektes Licht nutzen!
                OverlayTexture.DEFAULT_UV,
                -1,   // Farbe (Weiß)
                null, // Sprite (null für direkte Textur)
                0,    // Glint
                state.crumblingOverlay
        );

        matrices.pop();
    }

    public static class State extends ChestBlockEntityRenderState {
        public Identifier texture;
        public float rotation;
        public float openFactor;
    }
}