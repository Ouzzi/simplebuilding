package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeichnet die Mod-Truhen genau wie Vanillas {@code ChestRenderer} (MC 1.21.11: drei Modelle,
 * Drehung ueber den Winkel der Blickrichtung, {@code Material}/{@code MaterialSet} statt der
 * 26.x-{@code SpriteId}s), nur mit den Texturen der Stufe aus dem Truhen-Atlas. Siehe den
 * 26.x-Zwilling fuer Truhen-Optimierer und Leistung.
 */
public class TieredChestRenderer implements BlockEntityRenderer<TieredChestBlockEntity, TieredChestRenderer.State> {
    private static final Material[][] MATERIALS = new Material[ChestTier.values().length][];

    static {
        for (ChestTier tier : ChestTier.values()) {
            String name = tier.textureName();
            MATERIALS[tier.ordinal()] = new Material[]{material(name), material(name + "_left"), material(name + "_right")};
        }
    }

    private final MaterialSet materials;
    private final ChestModel single;
    private final ChestModel left;
    private final ChestModel right;

    public TieredChestRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.single = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.left = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.right = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    private static Material material(String name) {
        return Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }

    /** Das Material einer Stufe fuer eine Truhenform (einzeln, links, rechts). */
    public static Material materialFor(ChestTier tier, ChestType type) {
        Material[] set = MATERIALS[tier.ordinal()];
        return switch (type) {
            case SINGLE -> set[0];
            case LEFT -> set[1];
            case RIGHT -> set[2];
        };
    }

    /** Vanillas Zustand plus die Stufe. */
    public static class State extends ChestRenderState {
        public ChestTier tier = ChestTier.REINFORCED;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TieredChestBlockEntity chest, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = chest.getBlockState();
        state.type = blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        state.angle = blockState.hasProperty(ChestBlock.FACING) ? blockState.getValue(ChestBlock.FACING).toYRot() : 0.0F;
        state.tier = chest.tier();
        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combined;
        if (chest.getLevel() != null && blockState.getBlock() instanceof TieredChestBlock block) {
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
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.angle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        float open = 1.0F - state.open;
        open = 1.0F - open * open * open;
        ChestModel model = switch (state.type) {
            case SINGLE -> this.single;
            case LEFT -> this.left;
            case RIGHT -> this.right;
        };
        Material material = materialFor(state.tier, state.type);
        collector.submitModel(model, open, poseStack, material.renderType(model::renderType), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, this.materials.get(material), 0, state.breakProgress);
        poseStack.popPose();
    }
}
