package com.simpleovens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simpleovens.OvensRegistry;
import com.simpleovens.boat.OvenBoat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Furnace boat: Vanilla's boat or raft model and texture (nothing copied) plus the Vanilla furnace block where the chest
 * boat has its chest, facing forward; lit with a glow while the boat has fuel. The furnace is the block of the boat's tier (b-small04):
 * the plain Vanilla furnace or the Reinforced, Netherite or Enderite furnace.
 */
public final class OvenBoatRenderer extends BoatRenderer {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    private final BlockModelResolver blockModels;
    private final Block furnace;

    /** @param key the boat key of {@link OvensRegistry#boatKey}: the wood, with the tier prefix in front for the upper tiers. */
    public OvenBoatRenderer(EntityRendererProvider.Context context, String key) {
        super(context, layer(OvensRegistry.boatWood(key)));
        this.blockModels = context.getBlockModelResolver();
        var tier = OvensRegistry.boatTier(key);
        this.furnace = tier == null ? Blocks.FURNACE : OvensRegistry.FURNACES.get(tier);
    }

    /** Vanilla's model layer of the wood; the texture follows from it. */
    static ModelLayerLocation layer(String wood) {
        return switch (wood) {
            case "spruce" -> ModelLayers.SPRUCE_BOAT;
            case "birch" -> ModelLayers.BIRCH_BOAT;
            case "jungle" -> ModelLayers.JUNGLE_BOAT;
            case "acacia" -> ModelLayers.ACACIA_BOAT;
            case "dark_oak" -> ModelLayers.DARK_OAK_BOAT;
            case "mangrove" -> ModelLayers.MANGROVE_BOAT;
            case "cherry" -> ModelLayers.CHERRY_BOAT;
            case "pale_oak" -> ModelLayers.PALE_OAK_BOAT;
            case "bamboo" -> ModelLayers.BAMBOO_RAFT;
            default -> ModelLayers.OAK_BOAT;
        };
    }

    /** Render state with the furnace block (and whether this is the raft, which sits higher). */
    public static final class State extends BoatRenderState {
        final BlockModelRenderState furnace = new BlockModelRenderState();
        boolean lit;
        boolean raft;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof State s && entity instanceof OvenBoat boat) {
            s.lit = boat.isLit();
            s.raft = boat.isRaft();
            this.blockModels.update(s.furnace, this.furnace.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.NORTH)
                    .setValue(AbstractFurnaceBlock.LIT, s.lit), DISPLAY);
        }
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        super.submitTypeAdditions(state, poseStack, collector, lightCoords);
        if (!(state instanceof State s) || s.furnace.isEmpty()) return;
        poseStack.pushPose();
        // Undo the model frame (scale -1/-1/1, quarter turn): block space again, y up, the boat's front towards -z.
        // The furnace is the chest's 12 pixel size, 8/16 behind the middle.
        poseStack.rotateDegrees(Axis.YP, -90.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(-0.375F, s.raft ? 2.1F / 16.0F : -3.0F / 16.0F, 0.125F);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        int light = s.lit ? LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(lightCoords), 13), LightCoordsUtil.sky(lightCoords)) : lightCoords;
        s.furnace.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
