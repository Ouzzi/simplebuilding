package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
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
 * Zeichnet die Mod-Truhen genau wie Vanillas {@code ChestRenderer} - dieselben drei Modelle
 * (einzeln, linke und rechte Haelfte), dieselbe Deckelkurve, dasselbe Licht einer Doppeltruhe -
 * nur mit den Texturen der Stufe aus dem Truhen-Atlas
 * ({@code simplebuilding:textures/entity/chest/<stufe>[_left|_right].png}; Vanillas Atlas-Quelle
 * {@code entity/chest} sammelt alle Namensraeume ein). Keine zusaetzliche Arbeit je Tick: der
 * Deckel wird wie bei Vanilla von der Block-Entity getaktet.
 *
 * <p><b>Truhen-Optimierer</b> (Mods, die geschlossene Vanilla-Truhen als statisches Blockmodell
 * statt ueber den Block-Entity-Renderer zeichnen) greifen hier nicht: sie ersetzen nur Vanillas
 * Truhen-Renderer. Die Mod-Truhen zeichnen sich weiter selbst und brechen dabei nichts.
 */
public class TieredChestRenderer implements BlockEntityRenderer<TieredChestBlockEntity, TieredChestRenderer.State> {
    private static final SpriteId[][] SPRITES = new SpriteId[ChestTier.values().length][];
    private static final SpriteId[][] TRAPPED_SPRITES = new SpriteId[ChestTier.values().length][];

    static {
        for (ChestTier tier : ChestTier.values()) {
            String name = tier.textureName();
            SPRITES[tier.ordinal()] = new SpriteId[]{sprite(name), sprite(name + "_left"), sprite(name + "_right")};
            TRAPPED_SPRITES[tier.ordinal()] = new SpriteId[]{sprite(name + "_trapped"),
                    sprite(name + "_trapped_left"), sprite(name + "_trapped_right")};
        }
    }

    private final SpriteGetter sprites;
    private final MultiblockChestResources<ChestModel> models;

    public TieredChestRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.models = ChestRenderer.LAYERS.map(layer -> new ChestModel(context.bakeLayer(layer)));
    }

    private static SpriteId sprite(String name) {
        return Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }

    /** Der Sprite einer Stufe fuer eine Truhenform (einzeln, links, rechts). */
    public static SpriteId spriteFor(ChestTier tier, ChestType type) {
        return spriteFor(tier, type, false);
    }

    public static SpriteId spriteFor(ChestTier tier, ChestType type, boolean trapped) {
        SpriteId[] set = (trapped ? TRAPPED_SPRITES : SPRITES)[tier.ordinal()];
        return switch (type) {
            case SINGLE -> set[0];
            case LEFT -> set[1];
            case RIGHT -> set[2];
        };
    }

    /** Vanillas Zustand plus die Stufe. */
    public static class State extends ChestRenderState {
        public ChestTier tier = ChestTier.REINFORCED;
        public boolean trapped;
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
        state.facing = blockState.hasProperty(ChestBlock.FACING) ? blockState.getValue(ChestBlock.FACING) : Direction.SOUTH;
        state.tier = chest.tier();
        state.trapped = blockState.getBlock() instanceof TieredChestBlock block && block.isTrapped();
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
        poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));
        float open = 1.0F - state.open;
        open = 1.0F - open * open * open;
        com.simplebuilding.version.McClientVersion.submitChestModel(collector, this.models.select(state.type), open, poseStack,
                state.lightCoords, spriteFor(state.tier, state.type, state.trapped), this.sprites, state.breakProgress);
        poseStack.popPose();
    }
}
