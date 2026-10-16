package com.simpletrims.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simpletrims.placed.PlacedTrimTemplateBlock;
import com.simpletrims.placed.PlacedTrimTemplateBlockEntity;
import com.simpletrims.placed.PlacedPlate;
import com.simpletrims.placed.PlacedSolids;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeichnet die abgelegte Schmiedevorlage ({@link PlacedTrimTemplateBlock}): das Item-Modell der Vorlage
 * ohne Anzeige-Transformation ({@link ItemDisplayContext#NONE}) - bei Vorlagen das ausgestanzte
 * {@code item/generated}, also die Item-Textur als Platte mit Raendern - auf {@link #SCALE} der
 * Blockflaeche, in der Dicke auf gut einen Pixel ({@link #THICKNESS_SCALE}) gestreckt und mit der
 * Rueckseite an Boden, Wand oder Decke.
 *
 * <p>Ausrichtung: an der Wand zeigt die Vorlage in {@code FACING} (Oberkante nach oben), am Boden
 * und an der Decke zeigt ihre Oberkante in die Blickrichtung beim Ablegen, man liest sie also
 * aufrecht. Dieselbe Lage rechnet {@link PlacedPlate#transform} fuer die pixelgenaue Trefferform -
 * wer hier etwas an der Lage aendert, muss es dort mitziehen. Loader-neutral; registriert wird der
 * Renderer je Loader.
 */
public class PlacedTrimTemplateRenderer implements BlockEntityRenderer<PlacedTrimTemplateBlockEntity, PlacedTrimTemplateRenderer.State> {
    /** Kantenlaenge der Vorlage in Blockbreiten; Lage und Trefferform rechnet {@link PlacedPlate}. */
    public static final float SCALE = PlacedPlate.SCALE;
    /** Streckung der Dicke: das Item-Modell ist 1/16 dick, gestreckt gut 1,4 Pixel. */
    public static final float THICKNESS_SCALE = PlacedPlate.THICKNESS_SCALE;
    /** Abstand zur Auflageflaeche gegen Z-Fighting. */
    private static final float GAP = PlacedPlate.GAP;

    /** Stand-in stack per item that draws the 3D model {@code simpletrims:placed_<path>} ({@link PlacedSolids}). */
    private static final java.util.Map<Item, ItemStack> SOLID_MODELS = new java.util.concurrent.ConcurrentHashMap<>();

    private final ItemModelResolver itemModelResolver;

    public PlacedTrimTemplateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    /** Was der Renderer je Bild braucht: Lage und das aufgeloeste Item-Modell. */
    public static class State extends BlockEntityRenderState {
        public AttachFace face = AttachFace.FLOOR;
        public Direction facing = Direction.NORTH;
        /** Draws the 2 px 3D model instead of the flat plate. */
        public boolean solid;
        /** The local player aims at this plate in tool stance: drawn at full brightness, pulsing (target highlight). */
        public boolean aimed;
        public final ItemStackRenderState item = new ItemStackRenderState();
    }

    private static ItemStack solidModel(ItemStack stored) {
        return SOLID_MODELS.computeIfAbsent(stored.getItem(), item -> {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.ITEM_MODEL, PlacedSolids.modelId(item));
            return stack;
        });
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlacedTrimTemplateBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        boolean flat = false;
        if (blockState.getBlock() instanceof PlacedTrimTemplateBlock) {
            state.face = blockState.getValue(PlacedTrimTemplateBlock.FACE);
            state.facing = blockState.getValue(PlacedTrimTemplateBlock.FACING);
            flat = blockState.getValue(PlacedTrimTemplateBlock.FLAT);
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.aimed = mc.player != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && hit.getBlockPos().equals(blockEntity.getBlockPos()) && com.simpletrims.placed.PlateHint.aimedAtStrikePlate(mc.player, hit);
        if (state.aimed) {
            // Pulse between full light and a bright dimmer step so the aimed plate stands out without hiding its texture.
            int pulse = 11 + (int) Math.round(4 * (0.5 + 0.5 * Math.sin((mc.level == null ? 0 : mc.level.getGameTime() + partialTicks) * 0.5)));
            state.lightCoords = LightCoordsUtil.pack(Math.max(pulse, LightCoordsUtil.block(state.lightCoords)),
                    Math.max(pulse, LightCoordsUtil.sky(state.lightCoords)));
        }
        ItemStack stored = blockEntity.getTemplate();
        state.solid = !flat && PlacedSolids.has(stored);
        this.itemModelResolver.updateForTopItem(state.item, state.solid ? solidModel(stored) : stored, ItemDisplayContext.NONE,
                blockEntity.getLevel(), null, (int) blockEntity.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        orient(poseStack, state.face, state.facing);
        // 3D model: it carries its own thickness (2 px); the flat plate is stretched to a little over 1 px.
        float stretch = state.solid ? 1.0F : THICKNESS_SCALE;
        float thickness = SCALE * (state.solid ? PlacedPlate.MODEL_THICKNESS : 1.0F) * stretch / 16.0F;
        poseStack.translate(0.0F, 0.0F, -(0.5F - thickness / 2.0F - GAP));
        poseStack.scale(SCALE, SCALE, SCALE * stretch);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /**
     * Dreht so, dass die Schauseite des Item-Modells (+Z) von der Auflage weg zeigt und seine
     * Oberkante (+Y) wie oben beschrieben liegt; der Ursprung wandert in die Blockmitte.
     */
    public static void orient(PoseStack poseStack, AttachFace face, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (face) {
            case WALL -> poseStack.rotate(Axis.YP.rotationDegrees(-facing.toYRot()));
            case FLOOR -> {
                poseStack.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
                poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
            }
            case CEILING -> {
                poseStack.rotate(Axis.YP.rotationDegrees(-facing.toYRot()));
                poseStack.rotate(Axis.XP.rotationDegrees(90.0F));
            }
        }
    }
}
