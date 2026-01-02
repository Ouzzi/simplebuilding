package com.simplebuilding.mixin.client;

import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;
    @Shadow private ClientWorld world;

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        BlockHighlightRenderer.render(positionMatrix, camera);
    }

    @Inject(method = "renderBlockDamage", at = @At("TAIL"))
    private void renderAdditionalBlockDamage(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, WorldRenderState renderStates, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null || this.world == null) return;

        ItemStack stack = player.getMainHandStack();
        var blockRenderManager = client.getBlockRenderManager();
        var registryManager = world.getRegistryManager();
        var enchantLookup = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);

        // --- Enchantment Checks ---
        var stripMinerKey = enchantLookup.getOptional(ModEnchantments.STRIP_MINER);
        var veinMinerKey = enchantLookup.getOptional(ModEnchantments.VEIN_MINER);

        boolean hasStripMiner = stripMinerKey.isPresent() && EnchantmentHelper.getLevel(stripMinerKey.get(), stack) > 0;
        boolean hasVeinMiner = veinMinerKey.isPresent() && EnchantmentHelper.getLevel(veinMinerKey.get(), stack) > 0;
        boolean isSledgehammer = stack.getItem() instanceof SledgehammerItem;

        if (!isSledgehammer && !hasStripMiner && !hasVeinMiner) return;

        for (Long2ObjectMap.Entry<SortedSet<BlockBreakingInfo>> entry : this.blockBreakingProgressions.long2ObjectEntrySet()) {
            for (BlockBreakingInfo info : entry.getValue()) {
                if (info.getActorId() == player.getId()) {
                    BlockPos mainPos = info.getPos();
                    int stage = info.getStage();
                    if (stage < 0 || stage >= 10) continue;

                    BlockState mainState = world.getBlockState(mainPos);
                    List<BlockPos> connectedBlocks = new ArrayList<>();

                    // 1. Sledgehammer Logik (Bleibt wie gehabt)
                    if (isSledgehammer) {
                        connectedBlocks.addAll(SledgehammerItem.getBlocksToBeDestroyed(1, mainPos, player));
                    }
                    // 2. Strip Miner Logik - NEU: && player.isSneaking()
                    else if (hasStripMiner && stack.isIn(ItemTags.PICKAXES) && player.isSneaking()) {
                        connectedBlocks.addAll(getStripMinerBlocks(player, stack, mainPos, hasStripMiner ? EnchantmentHelper.getLevel(stripMinerKey.get(), stack) : 0));
                    }
                    // 3. Vein Miner Logik - NEU: && player.isSneaking()
                    else if (hasVeinMiner && (stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.AXES)) && player.isSneaking()) {
                        connectedBlocks.addAll(getVeinMinerBlocks(world, mainPos, mainState, hasVeinMiner ? EnchantmentHelper.getLevel(veinMinerKey.get(), stack) : 0, stack));
                    }

                    Vec3d cameraPos = renderStates.cameraRenderState.pos;
                    double camX = cameraPos.x;
                    double camY = cameraPos.y;
                    double camZ = cameraPos.z;

                    for (BlockPos targetPos : connectedBlocks) {
                        if (targetPos.equals(mainPos)) continue;

                        // Validierung
                        if (isSledgehammer && !SledgehammerUtils.shouldBreak(this.world, targetPos, mainPos, stack)) continue;
                        if (!isSledgehammer && !stack.getItem().isCorrectForDrops(stack, world.getBlockState(targetPos))) continue;

                        BlockState state = this.world.getBlockState(targetPos);
                        if (!state.isAir()) {
                            matrices.push();
                            matrices.translate((double)targetPos.getX() - camX, (double)targetPos.getY() - camY, (double)targetPos.getZ() - camZ);
                            RenderLayer layer = (RenderLayer) ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage);
                            VertexConsumer consumer = vertexConsumers.getBuffer(layer);
                            VertexConsumer overlayConsumer = new OverlayVertexConsumer(consumer, matrices.peek(), 1.0f);
                            blockRenderManager.renderDamage(state, targetPos, this.world, matrices, overlayConsumer);
                            matrices.pop();
                        }
                    }
                }
            }
        }
    }

    // --- Helper Methoden (Kopiert & angepasst aus den Event-Klassen) ---

    @Unique
    private List<BlockPos> getStripMinerBlocks(PlayerEntity player, ItemStack stack, BlockPos startPos, int level) {
        List<BlockPos> found = new ArrayList<>();
        int depth = (level == 3) ? 4 : level;
        Direction miningDirection = getMiningDirection(player);

        for (int i = 1; i <= depth; i++) {
            BlockPos targetPos = startPos.offset(miningDirection, i);
            BlockState targetState = world.getBlockState(targetPos);

            if (targetState.isAir() || targetState.getHardness(world, targetPos) < 0) break;
            if (!stack.getItem().isCorrectForDrops(stack, targetState)) break;
            found.add(targetPos);
        }
        return found;
    }

    @Unique
    private Direction getMiningDirection(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch < -60) return Direction.UP;
        if (pitch > 60) return Direction.DOWN;
        return player.getHorizontalFacing();
    }

    @Unique
    private List<BlockPos> getVeinMinerBlocks(ClientWorld world, BlockPos startPos, BlockState targetState, int level, ItemStack stack) {
        // Gleiche Validierung wie im Event
        boolean isPickaxe = stack.isIn(ItemTags.PICKAXES);
        boolean isAxe = stack.isIn(ItemTags.AXES);
        if (isPickaxe && !isOre(targetState)) return Collections.emptyList();
        if (isAxe && !targetState.isIn(BlockTags.LOGS)) return Collections.emptyList();

        int maxBlocks = switch (level) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 12;
            case 5 -> 18;
            default -> 18;
        };

        List<BlockPos> found = new ArrayList<>();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);
        int foundCount = 0;

        while (!queue.isEmpty() && foundCount < (maxBlocks - 1)) {
            BlockPos current = queue.poll();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos neighbor = current.add(x, y, z);
                        if (!visited.contains(neighbor)) {
                            BlockState neighborState = world.getBlockState(neighbor);
                            if (neighborState.getBlock() == targetState.getBlock()) {
                                visited.add(neighbor);
                                queue.add(neighbor);
                                found.add(neighbor);
                                foundCount++;
                                if (foundCount >= (maxBlocks - 1)) break;
                            }
                        }
                    }
                    if (foundCount >= (maxBlocks - 1)) break;
                }
                if (foundCount >= (maxBlocks - 1)) break;
            }
        }
        return found;
    }

    @Unique
    private boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES) ||
                state.isIn(BlockTags.IRON_ORES) ||
                state.isIn(BlockTags.COPPER_ORES) ||
                state.isIn(BlockTags.GOLD_ORES) ||
                state.isIn(BlockTags.REDSTONE_ORES) ||
                state.isIn(BlockTags.LAPIS_ORES) ||
                state.isIn(BlockTags.DIAMOND_ORES) ||
                state.isIn(BlockTags.EMERALD_ORES);
    }
}