package com.simplebuilding.client.render;

import com.simplebuilding.items.custom.BuildingWandItem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildingWandOutlineRenderer {

    public static void register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(BuildingWandOutlineRenderer::onBlockOutline);
    }

    private static boolean onBlockOutline(WorldRenderContext context, net.minecraft.client.render.state.OutlineRenderState outlineRenderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) return true;

        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof BuildingWandItem wandItem)) return true;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return true;

        BlockPos centerPos = blockHit.getBlockPos();
        Direction face = blockHit.getSide();

        int diameter = wandItem.getWandSquareDiameter();

        // Hole Map (Pos -> BlockState)
        Map<BlockPos, BlockState> previewMap = BuildingWandItem.getPreviewStates(client.world, player, stack, centerPos, face, diameter);

        if (previewMap.isEmpty()) return true;

        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        BlockRenderManager blockRenderManager = client.getBlockRenderManager();
        VertexConsumerProvider consumers = context.consumers();

        matrices.push();

        VertexConsumer baseConsumer = consumers.getBuffer(RenderLayers.translucentMovingBlock());
        VertexConsumer ghostConsumer = new TranslucentVertexConsumer(baseConsumer, 180);

        // Eine wiederverwendbare Liste und Random Instanz
        List<BlockModelPart> parts = new ArrayList<>();
        Random random = Random.create();

        // Iteriere über die Map
        for (Map.Entry<BlockPos, BlockState> entry : previewMap.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState renderState = entry.getValue();

            if (!client.world.getBlockState(pos).isReplaceable()) continue;

            matrices.push();
            matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

            // Skalierung für "Ghost" Effekt
            float scale = 0.5f;
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(scale, scale, scale);
            matrices.translate(-0.5, -0.5, -0.5);

            // --- NEUE LOGIK FÜR 1.21 MODEL RENDERING ---

            // 1. Model holen
            BlockStateModel model = blockRenderManager.getModel(renderState);

            // 2. Seed setzen
            long seed = renderState.getRenderingSeed(pos);
            random.setSeed(seed);

            // 3. Parts generieren
            parts.clear();
            model.addParts(random, parts);

            // 4. Rendern mit der Liste von Parts
            blockRenderManager.getModelRenderer().render(
                    client.world,
                    parts,          // Liste der Teile
                    renderState,
                    pos,
                    matrices,
                    ghostConsumer,
                    false,          // kein Culling für Vorschau meist besser oder 'true' wenn gewünscht
                    OverlayTexture.DEFAULT_UV
            );
            // ---------------------------------------------

            matrices.pop();
        }

        matrices.pop();

        return true;
    }

    private static class TranslucentVertexConsumer implements VertexConsumer {
        private final VertexConsumer parent;
        private final int alphaInt;
        private final float alphaFloat;

        public TranslucentVertexConsumer(VertexConsumer parent, int alpha) {
            this.parent = parent;
            this.alphaInt = alpha;
            this.alphaFloat = alpha / 255.0f;
        }

        @Override public VertexConsumer vertex(float x, float y, float z) { parent.vertex(x, y, z); return this; }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            parent.color(red, green, blue, this.alphaInt);
            return this;
        }

        @Override
        public VertexConsumer color(int color) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            return this.color(r, g, b, 255);
        }

        @Override public VertexConsumer texture(float u, float v) { parent.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { parent.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { parent.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { parent.normal(x, y, z); return this; }

        @Override
        public void quad(MatrixStack.Entry matrixEntry, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay) {
            parent.quad(matrixEntry, quad, red, green, blue, this.alphaFloat, light, overlay);
        }

        public VertexConsumer lineWidth(float width) { return this; }
    }
}