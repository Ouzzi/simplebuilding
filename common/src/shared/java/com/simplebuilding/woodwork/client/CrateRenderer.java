package com.simplebuilding.woodwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.version.McClientVersion;
import com.simplebuilding.woodwork.CrateBlock;
import com.simplebuilding.woodwork.CrateBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws what lies in a crate (Queue N35, owner references crate_ref_apples/crate_ref_iso): the items themselves as
 * small 3D items - the flat sprite drawn about 6 px wide and a full pixel thick - lying loosely in the crate, every
 * stack one layer from the bottom up. Each layer holds up to four items in a 2 x 2 heap (fewer for a part stack),
 * shifted and turned a little per layer so the heap looks piled, and reaching the walls so the lower layers show
 * through the gaps between the slats, not only from above. Layers lie {@code depth / SLOTS} apart (more than the
 * 1 px thickness) and the items of one layer at slightly different heights, so nothing flickers. The food always
 * lies on the lowest inner side: the bottom of an upright crate, the lower side wall of a crate laid on its side,
 * the ground under an upside-down one ({@link CrateBlock#interior}). Performance: at most
 * {@link #MAX_ITEMS} items per crate, and beyond {@link #NEAR} blocks only the two top layers (the rest hides
 * behind them at that distance).
 *
 * <p>Labels (Queue N37, owner 2026-10-11 crate_side.png: "from the side you cannot tell what is inside"): on the
 * middle slat of every side wall, outside, the crate shows up to {@link #LABELS} different items of its content as small
 * flat sprites (a block item as a thin slice of its model) - from every side one sees what the crate holds. Side walls
 * are the horizontal ones across the opening's axis (four for an upright crate, two for one laid on its side).
 * Loader-neutral; registered per loader.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderer.State> {
    private static final float PX = 1.0F / 16.0F;
    /** Item size: a 16 px sprite drawn 5.8 px wide (was 3.8 px before N35). */
    private static final float SCALE = 0.36F;
    /** Items per layer (2 x 2) and the cap per crate. */
    private static final int PER_LAYER = 4;
    public static final int MAX_ITEMS = CrateBlockEntity.SLOTS * PER_LAYER;
    /** Within this many blocks every layer is drawn, further away only the two top layers. */
    private static final double NEAR = 16.0;
    /** Different items shown on each side wall. */
    public static final int LABELS = 3;
    /** Label size: a 16 px sprite drawn 4 px wide, as tall as the middle slat. */
    private static final float LABEL_SCALE = 0.25F;

    private final ItemModelResolver itemModelResolver;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        /** Layers to draw (bottom first); per layer the item count, its slot and height. */
        public int layers;
        public final int[] shown = new int[CrateBlockEntity.SLOTS];
        public final int[] slot = new int[CrateBlockEntity.SLOTS];
        public final float[] height = new float[CrateBlockEntity.SLOTS];
        public final ItemStackRenderState[] items = new ItemStackRenderState[CrateBlockEntity.SLOTS];
        /** Centre of the heap and the offset of the 2 x 2 cells from it (block-relative). */
        public float centreX;
        public float centreZ;
        public float cellX;
        public float cellZ;
        public int seed;
        /** Opening of the crate, and the labels on its side walls (different items, slot order). */
        public Direction facing = Direction.UP;
        public int labelCount;
        public final ItemStackRenderState[] labels = new ItemStackRenderState[LABELS];

        public State() {
            for (int i = 0; i < this.items.length; i++) {
                this.items[i] = new ItemStackRenderState();
            }
            for (int i = 0; i < this.labels.length; i++) {
                this.labels[i] = new ItemStackRenderState();
            }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrateBlockEntity crate, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crate, state, partialTicks, cameraPosition, breakProgress);
        state.seed = (int) crate.getBlockPos().asLong();
        state.layers = 0;
        BlockState block = crate.getBlockState();
        state.facing = block.hasProperty(CrateBlock.FACING) ? block.getValue(CrateBlock.FACING) : Direction.UP;
        double[] in = CrateBlock.interior(state.facing);
        state.centreX = (float) (in[0] + in[3]) / 2.0F * PX;
        state.centreZ = (float) (in[2] + in[5]) / 2.0F * PX;
        // Cells a quarter of the inner width from the centre: the items reach the walls (and their gaps).
        state.cellX = (float) (in[3] - in[0]) / 4.0F * PX;
        state.cellZ = (float) (in[5] - in[2]) / 4.0F * PX;
        float floor = (float) in[1] * PX;
        // Eight layers up to one pixel below the rim; an item's middle lies half its thickness above its layer.
        float spacing = (float) (in[4] - 1.0 - in[1]) * PX / CrateBlockEntity.SLOTS;
        boolean near = cameraPosition.closerThan(Vec3.atCenterOf(crate.getBlockPos()), NEAR);
        int filled = 0;
        for (int slot = 0; slot < CrateBlockEntity.SLOTS; slot++) {
            if (!crate.getItem(slot).isEmpty()) {
                filled++;
            }
        }
        int skip = near ? 0 : Math.max(0, filled - 2);
        int layer = 0;
        int index = 0;
        for (int slot = 0; slot < CrateBlockEntity.SLOTS; slot++) {
            ItemStack stack = crate.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (index++ < skip) {
                layer++;
                continue;
            }
            int i = state.layers;
            float share = (float) stack.getCount() / crate.getMaxStackSize(stack);
            state.shown[i] = Math.min(PER_LAYER, Math.max(1, (int) Math.ceil(PER_LAYER * share)));
            state.slot[i] = slot;
            state.height[i] = floor + 0.5F * PX + layer * spacing;
            this.itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.NONE, crate.getLevel(), null, state.seed + slot);
            state.layers++;
            layer++;
        }
        for (int i = state.layers; i < CrateBlockEntity.SLOTS; i++) {
            state.items[i].clear();
        }
        // Labels: the first LABELS different items (with their components), in slot order.
        java.util.List<ItemStack> kinds = new java.util.ArrayList<>(LABELS);
        for (int slot = 0; slot < CrateBlockEntity.SLOTS && kinds.size() < LABELS; slot++) {
            ItemStack stack = crate.getItem(slot);
            if (!stack.isEmpty() && kinds.stream().noneMatch(k -> ItemStack.isSameItemSameComponents(k, stack))) {
                kinds.add(stack);
            }
        }
        state.labelCount = kinds.size();
        for (int i = 0; i < LABELS; i++) {
            if (i < kinds.size()) {
                this.itemModelResolver.updateForTopItem(state.labels[i], kinds.get(i), ItemDisplayContext.NONE, crate.getLevel(), null, state.seed + 97 * i);
            } else {
                state.labels[i].clear();
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.layers; i++) {
            ItemStackRenderState item = state.items[i];
            if (item.isEmpty()) {
                continue;
            }
            int layerSeed = state.seed * 31 + state.slot[i] * 17;
            // The heap shifts by up to half a pixel per layer, so the layers do not stack like a grid.
            float shiftX = (((layerSeed >>> 4) & 3) - 1.5F) / 3.0F * PX;
            float shiftZ = (((layerSeed >>> 6) & 3) - 1.5F) / 3.0F * PX;
            // Flat sprites are a full pixel thick; block-like items keep their own proportions, a bit smaller.
            boolean block = item.usesBlockLight();
            for (int n = 0; n < state.shown[i]; n++) {
                int cell = state.shown[i] == 1 ? -1 : ORDER[n];
                float x = state.centreX + shiftX + (cell < 0 ? 0 : ((cell & 1) * 2 - 1) * state.cellX);
                float z = state.centreZ + shiftZ + (cell < 0 ? 0 : ((cell >> 1) * 2 - 1) * state.cellZ);
                int hash = layerSeed + n * 7919;
                float turn = (((hash >>> 3) & 3) * 90.0F) + ((((hash >>> 8) & 7) - 3.5F) * 2.5F);
                poseStack.pushPose();
                // Items of one layer lie 0.1 px apart in height, so overlapping edges never share a plane.
                poseStack.translate(x, state.height[i] + n * 0.1F * PX + (block ? SCALE * 0.4F : 0.0F), z);
                McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(turn));
                if (block) {
                    poseStack.scale(SCALE * 0.8F, SCALE * 0.8F, SCALE * 0.8F);
                } else {
                    McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(-90.0F));
                    poseStack.scale(SCALE, SCALE, 1.0F);
                }
                item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
        submitLabels(state, poseStack, collector);
    }

    /**
     * The labels on the side walls: per wall the items one after the other along the middle slat (4 px apart), the sprite's
     * front facing out, its back just on the wall. Upright on the wall: the world's up for a standing crate's walls and for
     * the horizontal walls of one laid on its side.
     */
    private static void submitLabels(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.labelCount == 0) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side.getAxis() == state.facing.getAxis()) {
                continue;
            }
            for (int i = 0; i < state.labelCount; i++) {
                ItemStackRenderState label = state.labels[i];
                if (label.isEmpty()) {
                    continue;
                }
                boolean block = label.usesBlockLight();
                // A flat sprite (1/16 deep in its model) drawn 0.5 px thick; a block item (a whole cube) as a 1 px slice.
                float depth = block ? PX : 0.5F;
                float half = (block ? 1.0F : 0.5F) * PX / 2.0F;
                float across = (i - (state.labelCount - 1) / 2.0F) * 4.0F * PX;
                poseStack.pushPose();
                poseStack.translate(0.5F, 0.5F, 0.5F);
                // Local +Z to the outside of this wall (Direction.toYRot: south 0, west 90, north 180, east 270).
                McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(-side.toYRot()));
                // A standing crate's middle slat runs across the wall, a laid one's (rotated with the model) up the wall.
                boolean laid = state.facing.getAxis().isHorizontal();
                poseStack.translate(laid ? 0.0F : across, laid ? across : 0.0F, 0.5F + half + 0.01F * PX);
                poseStack.scale(LABEL_SCALE, LABEL_SCALE, depth);
                label.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }

    /** Fill order of the 2 x 2 cells: diagonal first, so two items already spread over the crate. */
    private static final int[] ORDER = {0, 3, 1, 2};
}
