package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.PlacedEggBlock;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.version.McClientVersion;
import java.util.EnumMap;
import java.util.List;
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
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeichnet ein Kleinteil-Haeufchen ({@link PlacedSmallPartsBlock}): jedes Teil an seinem festen Platz
 * ({@link PlacedSmallParts#place} - dieselbe Lage wie die Trefferform). Liegende Teile sind ihr Item-Modell ohne
 * Anzeige-Transformation ({@link ItemDisplayContext#NONE}), also die ausgestanzte Item-Textur als duenne Platte; Eier
 * sind das 3D-Ei {@code block/placed_egg_<farbe>}, gezeichnet ueber die Item-Definition
 * {@code simplebuilding:placed_egg_<farbe>} auf einem Ersatz-Stapel ({@code minecraft:item_model}). Loader-neutral;
 * registriert wird der Renderer je Loader.
 */
public class PlacedSmallPartsRenderer implements BlockEntityRenderer<PlacedSmallPartsBlockEntity, PlacedSmallPartsRenderer.State> {
    private static final Map<PlacedEggBlock.Egg, ItemStack> EGG_MODELS = new EnumMap<>(PlacedEggBlock.Egg.class);

    private final ItemModelResolver itemModelResolver;

    public PlacedSmallPartsRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    /** Was der Renderer je Bild braucht: Richtung, Anzahl, je Teil Modell und ob es ein Ei ist. */
    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public int count;
        public final boolean[] egg = new boolean[PlacedSmallParts.MAX_PARTS];
        public final ItemStackRenderState[] items = new ItemStackRenderState[PlacedSmallParts.MAX_PARTS];

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

    /** Der Ersatz-Stapel, der das 3D-Ei dieser Farbe zeichnet (die Item-Komponenten des Eis spielen keine Rolle). */
    private static ItemStack eggModel(PlacedEggBlock.Egg egg) {
        return EGG_MODELS.computeIfAbsent(egg, e -> {
            ItemStack stack = new ItemStack(e.item());
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "placed_egg_" + e.getSerializedName()));
            return stack;
        });
    }

    @Override
    public void extractRenderState(PlacedSmallPartsBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.getBlock() instanceof PlacedSmallPartsBlock ? blockState.getValue(PlacedSmallPartsBlock.FACING) : Direction.NORTH;
        List<ItemStack> parts = blockEntity.parts();
        state.count = Math.min(parts.size(), PlacedSmallParts.MAX_PARTS);
        int seed = (int) blockEntity.getBlockPos().asLong();
        for (int i = 0; i < state.items.length; i++) {
            if (i >= state.count) {
                state.items[i].clear();
                continue;
            }
            ItemStack part = parts.get(i);
            PlacedEggBlock.Egg egg = PlacedEggBlock.Egg.of(part);
            state.egg[i] = egg != null;
            this.itemModelResolver.updateForTopItem(state.items[i], egg != null ? eggModel(egg) : part, ItemDisplayContext.NONE,
                    blockEntity.getLevel(), null, seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.count; i++) {
            if (state.items[i].isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            PlacedSmallParts.place(new PoseOps(poseStack), state.facing, state.count, i, state.egg[i]);
            state.items[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    /** {@link PlacedSmallParts.Ops} auf den PoseStack. */
    private record PoseOps(PoseStack poseStack) implements PlacedSmallParts.Ops {
        @Override
        public void translate(float x, float y, float z) {
            poseStack.translate(x, y, z);
        }

        @Override
        public void rotateY(float radians) {
            McClientVersion.rotate(poseStack, Axis.YP.rotation(radians));
        }

        @Override
        public void rotateX(float radians) {
            McClientVersion.rotate(poseStack, Axis.XP.rotation(radians));
        }

        @Override
        public void scale(float x, float y, float z) {
            poseStack.scale(x, y, z);
        }
    }
}
