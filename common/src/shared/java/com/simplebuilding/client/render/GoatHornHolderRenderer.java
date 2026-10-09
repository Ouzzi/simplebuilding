package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.GoatHornHolderBlock;
import com.simplebuilding.blocks.custom.StandingRodBlock;
import com.simplebuilding.blocks.entity.custom.GoatHornHolderBlockEntity;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the item stuck into a goat horn holder ({@link GoatHornHolderBlock}; the horn itself is the block model): a
 * torch as its standing 3D torch (item definitions {@code simplebuilding:held_<torch>} pointing at the vanilla block
 * model, written by {@code tools/textures/goat_horn_holder_2026_10_09.py}), a rod as its standing column
 * ({@code simplebuilding:standing_<rod>}). It stands upright in the mouth, {@link GoatHornHolderBlock#DEPTH} pixels
 * deep. Other torches without a definition are drawn as their flat item. Loader-neutral; registered per loader.
 */
public class GoatHornHolderRenderer implements BlockEntityRenderer<GoatHornHolderBlockEntity, GoatHornHolderRenderer.State> {
    private static final Map<String, ItemStack> MODELS = new java.util.concurrent.ConcurrentHashMap<>();
    /** Vanilla torches with an item definition {@code simplebuilding:held_<id>}. */
    private static final java.util.Set<String> DEFINED_TORCHES = java.util.Set.of("torch", "soul_torch", "copper_torch", "redstone_torch");

    private final ItemModelResolver itemModelResolver;

    public GoatHornHolderRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        /** Bottom of the held model above the block's bottom, in pixels. */
        public float bottom;
        public final ItemStackRenderState item = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The stand-in stack that draws the held item standing (or the item itself). */
    static ItemStack model(ItemStack held) {
        Identifier id = BuiltInRegistries.ITEM.getKey(held.getItem());
        String name;
        StandingRodBlock.Rod rod = StandingRodBlock.Rod.of(held);
        if (rod != null) {
            name = "standing_" + rod.getSerializedName();
        } else if (Block.byItem(held.getItem()) instanceof BaseTorchBlock && "minecraft".equals(id.getNamespace())
                && DEFINED_TORCHES.contains(id.getPath())) {
            name = "held_" + id.getPath();
        } else {
            return held;
        }
        return MODELS.computeIfAbsent(name, n -> {
            ItemStack stack = new ItemStack(held.getItem());
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, n));
            return stack;
        });
    }

    @Override
    public void extractRenderState(GoatHornHolderBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        boolean wall = blockState.getBlock() instanceof GoatHornHolderBlock && blockState.getValue(GoatHornHolderBlock.FACE) == AttachFace.WALL;
        state.bottom = (wall ? GoatHornHolderBlock.WALL_MOUTH : GoatHornHolderBlock.FLOOR_MOUTH) - GoatHornHolderBlock.DEPTH;
        ItemStack held = blockEntity.held();
        if (held.isEmpty()) {
            state.item.clear();
            return;
        }
        this.itemModelResolver.updateForTopItem(state.item, model(held), ItemDisplayContext.NONE, blockEntity.getLevel(), null,
                (int) blockEntity.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // The model's middle (8, 8, 8) sits in the origin: its bottom goes to the mouth, its axis through x = z = 8.
        poseStack.translate(0.5F, state.bottom / 16.0F + 0.5F, 0.5F);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
