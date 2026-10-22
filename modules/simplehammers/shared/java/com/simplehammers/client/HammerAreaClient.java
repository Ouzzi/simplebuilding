package com.simplehammers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplehammers.engine.HammerEnchantments;
import com.simplehammers.engine.SledgehammerUtils;
import com.simplehammers.item.SledgehammerItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The hammer's area on the client (Queue 738, docs/ai/PLAN-B-S9B-2026-10-22.md; until then SimpleBuilding's
 * {@code BlockHighlightRenderer}/{@code MultiBlockBreakingSupport}): while a sledgehammer is in the main hand an outline
 * with a faint fill marks every further block one swing takes along, and while mining those blocks show vanilla's breaking
 * cracks too. Both read the same list as the server's area break ({@link SledgehammerItem#getBlocksToBeDestroyed}).
 * Wired per loader: Fabric {@code LevelRenderEvents.COLLECT_SUBMITS}/{@code END_EXTRACTION}, NeoForge
 * {@code SubmitCustomGeometryEvent}/{@code ExtractLevelRenderStateEvent}, Forge two mixins at the same places.
 */
public final class HammerAreaClient {
    private static final float LINE_WIDTH = 4.0F;

    // Cracks: the area is only computed again when target block, tool, sneaking or off hand change.
    private static BlockPos lastMainPos;
    private static ItemStack lastTool = ItemStack.EMPTY;
    private static boolean lastSneaking;
    private static ItemStack lastOffhand = ItemStack.EMPTY;
    private static List<BlockPos> cachedArea = Collections.emptyList();

    private HammerAreaClient() {
    }

    /** Outline and fill of the area around the aimed block; {@code cameraPos} turns world into camera space. */
    public static void submitOutline(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !HammerVisuals.showArea()) return;
        ItemStack stack = player.getMainHandItem();
        if (!SledgehammerItem.isHammer(stack) || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos center = hit.getBlockPos();
        // A selection of another mod replaces the area (SimpleBuilding's octant) and is drawn by that mod already.
        if (!player.isShiftKeyDown() && SledgehammerUtils.octantSelection(player, center) != null) return;
        List<BlockPos> area = SledgehammerItem.getBlocksToBeDestroyed(1, center, player);
        List<AABB> boxes = new ArrayList<>();
        for (BlockPos pos : area) {
            if (pos.equals(center) || !SledgehammerUtils.shouldBreak(client.level, pos, center, stack)) continue;
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir()) continue;
            VoxelShape shape = state.getShape(client.level, pos);
            for (AABB box : shape.toAabbs()) boxes.add(box.move(pos));
        }
        if (boxes.isEmpty()) return;
        float fillAlpha = 0.3F * HammerVisuals.opacityPercent() / 100.0F;
        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
                for (AABB box : boxes) outline(pose, buffer, box.inflate(0.001), 0.3F);
            });
            if (fillAlpha > 0.0F) {
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
                    for (AABB box : boxes) fill(pose, buffer, box.inflate(0.003), fillAlpha);
                });
            }
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Extra breaking cracks on the area, after vanilla extracted its own crack. Drawn while vanilla draws one at the
     * aimed block (it mines on without {@code isDestroying} after the crosshair slipped off for a tick), never around a
     * block vanilla is not mining.
     */
    public static void extractCracks(LevelRenderState renderState, ClientLevel level) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (player == null || level == null || gameMode == null) {
            reset();
            return;
        }
        int stage = gameMode.getDestroyStage();
        ItemStack stack = player.getMainHandItem();
        if (stage < 0 || stage > 9 || !SledgehammerItem.isHammer(stack)) {
            reset();
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos mainPos = hit.getBlockPos();
        if (!vanillaCracks(renderState, mainPos)) return;
        boolean sneaking = player.isShiftKeyDown();
        ItemStack offhand = player.getOffhandItem();
        if (!(mainPos.equals(lastMainPos) && ItemStack.isSameItem(stack, lastTool) && sneaking == lastSneaking
                && ItemStack.isSameItemSameComponents(offhand, lastOffhand))) {
            cachedArea = SledgehammerItem.getBlocksToBeDestroyed(1, mainPos, player);
            lastMainPos = mainPos;
            lastTool = stack;
            lastSneaking = sneaking;
            lastOffhand = offhand.copy();
        }
        BlockState mainState = level.getBlockState(mainPos);
        int override = HammerEnchantments.level(stack, level, HammerEnchantments.OVERRIDE);
        for (BlockPos pos : cachedArea) {
            if (pos.equals(mainPos) || !SledgehammerUtils.shouldBreak(level, pos, mainState, stack, override)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) renderState.blockBreakingRenderStates.add(new BlockBreakingRenderState(pos, state, stage));
        }
    }

    private static boolean vanillaCracks(LevelRenderState renderState, BlockPos pos) {
        for (BlockBreakingRenderState state : renderState.blockBreakingRenderStates) {
            if (state.blockPos().equals(pos)) return true;
        }
        return false;
    }

    private static void reset() {
        lastMainPos = null;
        lastTool = ItemStack.EMPTY;
        lastSneaking = false;
        lastOffhand = ItemStack.EMPTY;
        cachedArea = Collections.emptyList();
    }

    private static void outline(PoseStack.Pose pose, VertexConsumer buffer, AABB b, float alpha) {
        double[][] edges = {
                {b.minX, b.minY, b.minZ, b.maxX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ},
                {b.maxX, b.minY, b.maxZ, b.minX, b.minY, b.maxZ}, {b.minX, b.minY, b.maxZ, b.minX, b.minY, b.minZ},
                {b.minX, b.maxY, b.minZ, b.maxX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ},
                {b.maxX, b.maxY, b.maxZ, b.minX, b.maxY, b.maxZ}, {b.minX, b.maxY, b.maxZ, b.minX, b.maxY, b.minZ},
                {b.minX, b.minY, b.minZ, b.minX, b.maxY, b.minZ}, {b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ},
                {b.maxX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ}, {b.minX, b.minY, b.maxZ, b.minX, b.maxY, b.maxZ}};
        for (double[] e : edges) {
            float nx = (float) (e[3] - e[0]), ny = (float) (e[4] - e[1]), nz = (float) (e[5] - e[2]);
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                nx /= length;
                ny /= length;
                nz /= length;
            }
            buffer.addVertex(pose, (float) e[0], (float) e[1], (float) e[2]).setColor(0.0F, 0.0F, 0.0F, alpha)
                    .setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
            buffer.addVertex(pose, (float) e[3], (float) e[4], (float) e[5]).setColor(0.0F, 0.0F, 0.0F, alpha)
                    .setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
        }
    }

    private static void fill(PoseStack.Pose pose, VertexConsumer buffer, AABB b, float alpha) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        quad(pose, buffer, alpha, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2); // down
        quad(pose, buffer, alpha, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1); // up
        quad(pose, buffer, alpha, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1); // north
        quad(pose, buffer, alpha, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2); // south
        quad(pose, buffer, alpha, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1); // west
        quad(pose, buffer, alpha, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2); // east
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float alpha, float... xyz) {
        for (int i = 0; i < 12; i += 3) buffer.addVertex(pose, xyz[i], xyz[i + 1], xyz[i + 2]).setColor(0.5F, 0.5F, 0.5F, alpha);
    }
}
