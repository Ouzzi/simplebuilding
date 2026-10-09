package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.TrappedCopperChestBlock;
import com.simplebuilding.blocks.entity.custom.TrappedCopperChestBlockEntity;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Trapped copper chest (owner N16): Vanilla's chest model drawn like {@link TieredChestRenderer} - first with Vanilla's
 * copper chest sprite of the oxidation stage, then once more with this mod's trapped overlay of that stage
 * ({@code simplebuilding:trapped_copper[_stage][_left|_right]}, transparent except the subtle trapped hint). The model
 * is cut out, so the overlay only replaces its few pixels.
 */
public class TrappedCopperChestRenderer implements BlockEntityRenderer<TrappedCopperChestBlockEntity, TrappedCopperChestRenderer.State> {
    private static final String[] STAGES = {"", "_exposed", "_weathered", "_oxidized"};
    private static final String[] SHAPES = {"", "_left", "_right"};

    private final SpriteGetter sprites;
    private final MultiblockChestResources<ChestModel> models;

    public TrappedCopperChestRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.models = ChestRenderer.LAYERS.map(layer -> new ChestModel(context.bakeLayer(layer)));
    }

    private static int shape(ChestType type) {
        return switch (type) {
            case SINGLE -> 0;
            case LEFT -> 1;
            case RIGHT -> 2;
        };
    }

    /** Vanilla's copper chest sprite of {@code stage} (0..3) for a chest shape. */
    public static SpriteId copperSprite(int stage, ChestType type) {
        return Sheets.CHEST_MAPPER.apply(Identifier.withDefaultNamespace("copper" + STAGES[stage] + SHAPES[shape(type)]));
    }

    /** The trapped overlay of {@code stage} for a chest shape. */
    public static SpriteId overlaySprite(int stage, ChestType type) {
        return Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID,
                "trapped_copper" + STAGES[stage] + SHAPES[shape(type)]));
    }

    /** Vanilla's chest state plus the oxidation stage. */
    public static class State extends ChestRenderState {
        public int stage;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TrappedCopperChestBlockEntity chest, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = chest.getBlockState();
        state.type = blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        state.facing = blockState.hasProperty(ChestBlock.FACING) ? blockState.getValue(ChestBlock.FACING) : Direction.SOUTH;
        state.stage = blockState.getBlock() instanceof TrappedCopperChestBlock block ? block.getAge().ordinal() : 0;
        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combined;
        if (chest.getLevel() != null && blockState.getBlock() instanceof ChestBlock block) {
            combined = block.combine(blockState, chest.getLevel(), chest.getBlockPos(), true);
        } else {
            combined = DoubleBlockCombiner.Combiner::acceptNone;
        }
        state.open = combined.apply(ChestBlock.opennessCombiner(chest)).get(partialTicks);
        if (state.type != ChestType.SINGLE) {
            state.lightCoords = combined.apply(new BrightnessCombiner<>()).applyAsInt(state.lightCoords);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));
        float open = 1.0F - state.open;
        open = 1.0F - open * open * open;
        ChestModel model = this.models.select(state.type);
        com.simplebuilding.version.McClientVersion.submitChestModel(collector, model, open, poseStack,
                state.lightCoords, copperSprite(state.stage, state.type), this.sprites, state.breakProgress);
        com.simplebuilding.version.McClientVersion.submitChestModel(collector, model, open, poseStack,
                state.lightCoords, overlaySprite(state.stage, state.type), this.sprites, null);
        poseStack.popPose();
    }
}
