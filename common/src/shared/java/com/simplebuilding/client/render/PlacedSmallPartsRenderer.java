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

    /** Ersatz-Stapel fuer Kerzen und Seegurken, je Item-Definition ({@code simplebuilding:placed_<kerze>[_lit]} ...). */
    private static final Map<String, ItemStack> BLOCK_MODELS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Was der Renderer je Bild braucht: Richtung, Anzahl, je Teil Modell und ob es aufrecht steht. */
    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public int count;
        public final PlacedSmallParts.Kind[] kinds = new PlacedSmallParts.Kind[PlacedSmallParts.MAX_PARTS];
        /** Nur Barren auf dem Fleck: sie liegen als kleiner Stapel (Queue N24). */
        public boolean ingotStack;
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

    /**
     * Der Ersatz-Stapel fuer eine Kerze bzw. Seegurke: die Item-Definition zeigt auf das Vanilla-Blockmodell einer
     * einzelnen Kerze ({@code minecraft:block/<farbe>_candle_one_candle[_lit]}) bzw. Seegurke
     * ({@code minecraft:block/[dead_]sea_pickle}); erzeugt von {@code tools/textures/placeables_v2_2026_10_03.py}.
     */
    private static ItemStack blockModel(ItemStack part, PlacedSmallParts.Kind kind, boolean lit, boolean wet) {
        String name = kind == PlacedSmallParts.Kind.CANDLE
                ? "placed_" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(part.getItem()).getPath() + (lit ? "_lit" : "")
                : wet ? "placed_sea_pickle" : "placed_dead_sea_pickle";
        return BLOCK_MODELS.computeIfAbsent(name, n -> {
            ItemStack stack = new ItemStack(part.getItem());
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, n));
            return stack;
        });
    }

    /** Der Ersatz-Stapel fuer einen 3D-Barren: Item-Definition {@code simplebuilding:placed_<barren>} (placed_ingots_2026_10_09.py). */
    private static ItemStack ingotModel(ItemStack part) {
        String name = "placed_" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(part.getItem()).getPath();
        return BLOCK_MODELS.computeIfAbsent(name, n -> {
            ItemStack stack = new ItemStack(part.getItem());
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, n));
            return stack;
        });
    }

    @Override
    public void extractRenderState(PlacedSmallPartsBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        boolean pile = blockState.getBlock() instanceof PlacedSmallPartsBlock;
        state.facing = pile ? blockState.getValue(PlacedSmallPartsBlock.FACING) : Direction.NORTH;
        boolean lit = pile && blockState.getValue(PlacedSmallPartsBlock.LIT);
        boolean wet = pile && blockState.getValue(PlacedSmallPartsBlock.WATERLOGGED);
        List<ItemStack> parts = blockEntity.parts();
        state.count = Math.min(parts.size(), PlacedSmallParts.MAX_PARTS);
        state.ingotStack = PlacedSmallParts.ingotStack(parts);
        int seed = (int) blockEntity.getBlockPos().asLong();
        for (int i = 0; i < state.items.length; i++) {
            if (i >= state.count) {
                state.items[i].clear();
                continue;
            }
            ItemStack part = parts.get(i);
            PlacedSmallParts.Kind kind = PlacedSmallParts.kind(part);
            state.kinds[i] = kind;
            ItemStack model = switch (kind) {
                case EGG -> eggModel(PlacedEggBlock.Egg.of(part));
                case CANDLE, PICKLE -> blockModel(part, kind, lit, wet);
                case INGOT -> ingotModel(part);
                case PLATE -> part;
            };
            this.itemModelResolver.updateForTopItem(state.items[i], model, ItemDisplayContext.NONE, blockEntity.getLevel(), null, seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.count; i++) {
            if (state.items[i].isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            PlacedSmallParts.place(new PoseOps(poseStack), state.facing, state.count, i, state.kinds[i], state.ingotStack);
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
