package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.blocks.entity.custom.StorageCraftingTableBlockEntity;
import com.simplebuilding.version.McClientVersion;
import java.util.List;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the grid of a Storage Crafting Table flat on its top (queue N26): the nine stacks in the 3x3 raster of the
 * top texture, first row in the north, each the item model without display transform ({@link ItemDisplayContext#NONE})
 * laid down and pressed flat, so blocks look like tiles and items like the item sprite. Lit by the light above the
 * table (the table itself is solid). Loader neutral; each loader registers it.
 */
public class StorageCraftingTableRenderer implements BlockEntityRenderer<StorageCraftingTableBlockEntity, StorageCraftingTableRenderer.State> {
    /** Cell pitch and item size in blocks: 5 px cells around the centre, 4.5 px items. */
    private static final float PITCH = 5.0F / 16.0F;
    private static final float SIZE = 0.28F;
    /** Flattening of the item models (blocks become 1/16 thin tiles), and how far they float above the top. */
    private static final float FLAT = 0.22F;
    private static final float LIFT = 0.012F;

    private final ItemModelResolver itemModelResolver;

    public StorageCraftingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState[] items = new ItemStackRenderState[StorageCraftingTableBlockEntity.SIZE];

        public State() {
            for (int i = 0; i < this.items.length; i++) {
                this.items[i] = new ItemStackRenderState();
            }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StorageCraftingTableBlockEntity table, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, cameraPosition, breakProgress);
        if (table.getLevel() != null) {
            state.lightCoords = LightCoordsUtil.getLightCoords(table.getLevel(), table.getBlockPos().above());
        }
        List<ItemStack> items = table.items();
        int seed = (int) table.getBlockPos().asLong();
        for (int i = 0; i < state.items.length; i++) {
            ItemStack stack = i < items.size() ? items.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                state.items[i].clear();
            } else {
                this.itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.NONE, table.getLevel(), null, seed + i);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.items.length; i++) {
            if (state.items[i].isEmpty()) {
                continue;
            }
            int column = i % 3, row = i / 3;
            poseStack.pushPose();
            poseStack.translate(0.5F + (column - 1) * PITCH, 1.0F + LIFT, 0.5F + (row - 1) * PITCH);
            // Lay the model on its back: its front faces up, its top points north (away from row 1).
            McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(-90.0F));
            poseStack.scale(SIZE, SIZE, SIZE * FLAT);
            state.items[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
