package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the tier shulker boxes exactly like vanilla's {@link ShulkerBoxRenderer} - the same model,
 * lid curve and rotation per facing, it even is a vanilla renderer doing the drawing - with the
 * texture of the tier and color from the shulker box atlas:
 * {@code simplebuilding:entity/shulker/<tier>} for an undyed box (vanilla's directory source picks
 * it up) and {@code simplebuilding:entity/shulker/<tier>_<color>} for a dyed one, which the
 * {@code paletted_permutations} source in {@code assets/minecraft/atlases/shulker_boxes.json} paints
 * from that one texture and sixteen palettes (no 17 x 3 hand-drawn textures).
 *
 * <p>The item uses vanilla's special model {@code minecraft:shulker_box} with the same sprite names,
 * selected by {@code minecraft:base_color}.
 */
public class TieredShulkerBoxRenderer implements BlockEntityRenderer<TieredShulkerBoxBlockEntity, TieredShulkerBoxRenderer.State> {
    private static final SpriteId[][] SPRITES = new SpriteId[ChestTier.values().length][DyeColor.values().length + 1];

    static {
        for (ChestTier tier : ChestTier.values()) {
            SPRITES[tier.ordinal()][0] = Sheets.SHULKER_MAPPER.apply(textureId(tier, null));
            for (DyeColor color : DyeColor.values()) {
                SPRITES[tier.ordinal()][color.getId() + 1] = Sheets.SHULKER_MAPPER.apply(textureId(tier, color));
            }
        }
    }

    private final ShulkerBoxRenderer vanilla;

    public TieredShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
        this.vanilla = new ShulkerBoxRenderer(context);
    }

    /** The sprite name under {@code entity/shulker/} of a tier and color: {@code reinforced}, {@code reinforced_red}. */
    public static Identifier textureId(ChestTier tier, @Nullable DyeColor color) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, color == null ? tier.textureName() : tier.textureName() + "_" + color.getName());
    }

    public static SpriteId spriteFor(ChestTier tier, @Nullable DyeColor color) {
        return SPRITES[tier.ordinal()][color == null ? 0 : color.getId() + 1];
    }

    /** Vanilla's state plus the tier. */
    public static class State extends ShulkerBoxRenderState {
        public ChestTier tier = ChestTier.REINFORCED;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TieredShulkerBoxBlockEntity box, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(box, state, partialTicks, cameraPosition, breakProgress);
        state.direction = box.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
        state.color = box.getColor();
        state.progress = box.getProgress(partialTicks);
        state.tier = box.tier();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(ShulkerBoxRenderer.modelTransform(state.direction));
        this.vanilla.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.progress, state.breakProgress,
                spriteFor(state.tier, state.color), 0);
        poseStack.popPose();
    }

    /**
     * Like NeoForge's patch of vanilla's renderer: the lid rises out of the block, so the box is drawn
     * while half a block beside the view. No {@code @Override} - only NeoForge's renderer interface
     * has this method; on Fabric it is unused.
     */
    public net.minecraft.world.phys.AABB getRenderBoundingBox(TieredShulkerBoxBlockEntity box) {
        net.minecraft.core.BlockPos pos = box.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5,
                pos.getX() + 1.5, pos.getY() + 1.5, pos.getZ() + 1.5);
    }
}
