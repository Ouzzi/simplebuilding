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
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

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
        List<BlockPos> targetPositions = BuildingWandItem.getBuildingPositions(client.world, player, stack, centerPos, face, diameter, blockHit);

        if (targetPositions.isEmpty()) return true;

        BlockState originState = client.world.getBlockState(centerPos);
        if (originState.isAir()) return true;

        BlockState renderState = originState;

        ItemStack offHandStack = player.getOffHandStack();
        if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof BlockItem bi) {
            renderState = bi.getBlock().getDefaultState();
        }

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
        VertexConsumer ghostConsumer = new TranslucentVertexConsumer(baseConsumer, 230);

        BlockStateModel model = blockRenderManager.getModel(renderState);

        for (BlockPos pos : targetPositions) {
            if (!client.world.getBlockState(pos).isReplaceable()) continue;

            matrices.push();
            matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

            float scale = 0.5f;
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(scale, scale, scale);
            matrices.translate(-0.5, -0.5, -0.5);

            long seed = renderState.getRenderingSeed(pos);
            List<BlockModelPart> parts = model.getParts(Random.create(seed));

            blockRenderManager.getModelRenderer().render(
                    client.world,
                    parts,
                    renderState,
                    pos,
                    matrices,
                    ghostConsumer,
                    false,
                    OverlayTexture.DEFAULT_UV
            );

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

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            parent.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // Hier überschreiben wir den Alpha-Wert
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

        @Override
        public VertexConsumer texture(float u, float v) {
            parent.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            parent.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            parent.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            parent.normal(x, y, z);
            return this;
        }

        @Override
        public void quad(MatrixStack.Entry matrixEntry, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay) {
            // Hier nutzen wir unseren festen Alpha-Float-Wert
            parent.quad(matrixEntry, quad, red, green, blue, this.alphaFloat, light, overlay);
        }

        // Diese Methode wurde ergänzt:
        // Manche Versionen haben sie nicht, aber wenn dein Compiler meckert, muss sie rein.
        // Falls "lineWidth" in deiner Version nicht existiert, entferne sie wieder (aber laut Fehler brauchst du sie).
        // @Override (annotation weglassen, falls interface methode nicht existiert in manchen mappings)
        public VertexConsumer lineWidth(float width) {
            // Manche Implementierungen unterstützen das nicht oder werfen Fehler, aber wir leiten es einfach weiter.
            // In manchen Yarn Mappings heißt das ggf. anders, aber meistens ist es lineWidth.
            return this;
        }
    }
}