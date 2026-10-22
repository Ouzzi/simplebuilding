package com.simplestorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplestorage.table.StorageCraftingTableBlockEntity;
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
 * laid down and pressed to {@link #THICKNESS} (SP22-4: 1.5 px, so they read from afar and from the side), blocks as
 * tiles and items as the item sprite. Lit by the light above the table (the table itself is solid). Loader neutral;
 * each loader registers it.
 */
public class StorageCraftingTableRenderer implements BlockEntityRenderer<StorageCraftingTableBlockEntity, StorageCraftingTableRenderer.State> {
    /** Cell pitch and item size in blocks: 5 px cells around the centre, 4.5 px items. */
    private static final float PITCH = 5.0F / 16.0F;
    private static final float SIZE = 0.28F;
    /** Height of every laid item in blocks (owner SP22-4: 1-2 px), and how far it floats above the top. */
    private static final float THICKNESS = 1.5F / 16.0F;
    private static final float LIFT = 0.003F;
    /** Depth of a flat item sprite model (Vanilla's generated items are 1 px deep); block models are 1 deep. */
    private static final float SPRITE_DEPTH = 1.0F / 16.0F;

    private final ItemModelResolver itemModelResolver;

    public StorageCraftingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState[] items = new ItemStackRenderState[StorageCraftingTableBlockEntity.SIZE];
        /** Per field: a block model (1 deep) rather than a flat sprite (1/16 deep). */
        public final boolean[] block = new boolean[StorageCraftingTableBlockEntity.SIZE];

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
                state.block[i] = state.items[i].usesBlockLight();
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
            // The model is centred on its depth: lift it by half the thickness so it stands on the top.
            poseStack.translate(0.5F + (column - 1) * PITCH, 1.0F + LIFT + THICKNESS / 2.0F, 0.5F + (row - 1) * PITCH);
            // Lay the model on its back: its front faces up, its top points north (away from row 1).
            poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
            poseStack.scale(SIZE, SIZE, THICKNESS / (state.block[i] ? 1.0F : SPRITE_DEPTH));
            state.items[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
