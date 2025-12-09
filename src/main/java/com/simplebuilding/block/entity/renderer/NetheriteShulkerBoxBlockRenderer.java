package com.simplebuilding.block.entity.renderer;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.model.NetheriteShulkerBoxModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.ShulkerBoxBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.Identifier;

public class NetheriteShulkerBoxBlockRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity, ShulkerBoxBlockEntityRenderState> {

    // Textur Definition
    public static final Identifier TEXTURE_ID = Identifier.of(Simplebuilding.MOD_ID, "entity/shulker/netherite_shulker");
    public static final SpriteIdentifier NETHERITE_TEXTURE = new SpriteIdentifier(
            TexturedRenderLayers.SHULKER_BOXES_ATLAS_TEXTURE,
            TEXTURE_ID
    );

    private final SpriteHolder materials;
    private final NetheriteShulkerBoxModel model;

    public NetheriteShulkerBoxBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        this.materials = ctx.spriteHolder();
        // Wir laden das Vanilla Shulker Model Part (es hat 'lid' und 'base')
        this.model = new NetheriteShulkerBoxModel(ctx.loadedEntityModels().getModelPart(EntityModelLayers.SHULKER_BOX));
    }

    @Override
    public ShulkerBoxBlockEntityRenderState createRenderState() {
        return new ShulkerBoxBlockEntityRenderState();
    }

    @Override
    public void updateRenderState(ShulkerBoxBlockEntity entity, ShulkerBoxBlockEntityRenderState state, float tickDelta, net.minecraft.util.math.Vec3d vec3d, net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        // Standard Update Logik (kopiert aus Superklasse, da wir Interface implementieren)
        BlockEntityRenderer.super.updateRenderState(entity, state, tickDelta, vec3d, crumblingOverlay);

        // Wichtige Daten in den State kopieren
        state.facing = entity.getCachedState().get(net.minecraft.block.ShulkerBoxBlock.FACING);
        state.animationProgress = entity.getAnimationProgress(tickDelta);
        // state.dyeColor ist uns egal, wir nutzen eh immer Netherite
    }

    @Override
    public void render(ShulkerBoxBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState) {

        // 1. Transformations-Logik (Skalierung, Rotation)
        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.scale(0.9995F, 0.9995F, 0.9995F);
        matrices.multiply(state.facing.getRotationQuaternion());
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0F, -1.0F, 0.0F);

        // 2. Model Animation updaten
        this.model.setAngles(state.animationProgress);

        // 3. Das Model an die Render-Queue übergeben
        // Das ersetzt den alten "vertexConsumer.vertex(...)" Code
        queue.submitModel(
                this.model,           // Das Model
                state.animationProgress, // Ein Parameter für das Model (wird von Model.render genutzt, hier float)
                matrices,             // Matrix Stack
                NETHERITE_TEXTURE.getRenderLayer(this.model::getLayer), // RenderLayer bestimmen
                state.lightmapCoordinates, // Licht
                OverlayTexture.DEFAULT_UV, // Overlay
                -1,                   // Farbe (Weiß/Keine Tönung)
                this.materials.getSprite(NETHERITE_TEXTURE), // Das Sprite aus dem Atlas holen!
                0,                    // Glint (Schimmern)
                state.crumblingOverlay // Abbau-Animation
        );

        matrices.pop();
    }
}