package com.simplebuilding.mixin.client;

import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.MiningUtils;
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
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
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

    // --- Cache Felder ---
    @Unique private BlockPos simplebuilding$lastMainPos = null;
    @Unique private List<BlockPos> simplebuilding$cachedConnectedBlocks = new ArrayList<>();
    // Hilft zu erkennen, ob sich das Tool geändert hat (z.B. Wechsel auf andere Pickaxe)
    @Unique private ItemStack simplebuilding$lastToolStack = ItemStack.EMPTY;

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
        var registryManager = world.getRegistryManager();
        var enchantLookup = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);

        // --- Enchantment Checks ---
        var stripMinerKey = enchantLookup.getOptional(ModEnchantments.STRIP_MINER);
        var veinMinerKey = enchantLookup.getOptional(ModEnchantments.VEIN_MINER);

        boolean hasStripMiner = stripMinerKey.isPresent() && EnchantmentHelper.getLevel(stripMinerKey.get(), stack) > 0;
        boolean hasVeinMiner = veinMinerKey.isPresent() && EnchantmentHelper.getLevel(veinMinerKey.get(), stack) > 0;
        boolean isSledgehammer = stack.getItem() instanceof SledgehammerItem;

        // Early exit wenn keine relevanten Features aktiv sind
        if (!isSledgehammer && !hasStripMiner && !hasVeinMiner) {
            simplebuilding$lastMainPos = null; // Cache reset
            return;
        }

        boolean foundActiveBreak = false;

        for (Long2ObjectMap.Entry<SortedSet<BlockBreakingInfo>> entry : this.blockBreakingProgressions.long2ObjectEntrySet()) {
            for (BlockBreakingInfo info : entry.getValue()) {
                if (info.getActorId() == player.getId()) {
                    BlockPos mainPos = info.getPos();
                    int stage = info.getStage();
                    if (stage < 0 || stage >= 10) continue;

                    foundActiveBreak = true;
                    BlockState mainState = world.getBlockState(mainPos);
                    List<BlockPos> connectedBlocks;

                    // --- CACHING LOGIC ---
                    // Wir prüfen: Ist es der gleiche Block wie im letzten Frame?
                    // Und ist es das gleiche Item? (Falls Spieler Item swappt während Abbau - unwahrscheinlich aber möglich)
                    boolean isCacheValid = mainPos.equals(simplebuilding$lastMainPos)
                            && ItemStack.areItemsEqual(stack, simplebuilding$lastToolStack);

                    if (isCacheValid) {
                        connectedBlocks = simplebuilding$cachedConnectedBlocks;
                    } else {
                        // Cache Miss -> Neu berechnen
                        connectedBlocks = new ArrayList<>();

                        if (isSledgehammer) {
                            connectedBlocks.addAll(SledgehammerItem.getBlocksToBeDestroyed(1, mainPos, player));
                        }
                        else if (hasStripMiner && stack.isIn(ItemTags.PICKAXES) && player.isSneaking()) {
                            int level = EnchantmentHelper.getLevel(stripMinerKey.get(), stack);
                            connectedBlocks.addAll(MiningUtils.getStripMinerBlocks(world, mainPos, player, stack, level));
                        }
                        else if (hasVeinMiner && (stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.AXES)) && player.isSneaking()) {
                            int level = EnchantmentHelper.getLevel(veinMinerKey.get(), stack);
                            connectedBlocks.addAll(MiningUtils.getVeinMinerBlocks(world, mainPos, mainState, level, stack));
                        }

                        // Cache aktualisieren
                        simplebuilding$lastMainPos = mainPos;
                        simplebuilding$lastToolStack = stack;
                        simplebuilding$cachedConnectedBlocks = connectedBlocks;
                    }
                    // ---------------------

                    Vec3d cameraPos = renderStates.cameraRenderState.pos;
                    double camX = cameraPos.x;
                    double camY = cameraPos.y;
                    double camZ = cameraPos.z;

                    for (BlockPos targetPos : connectedBlocks) {
                        if (targetPos.equals(mainPos)) continue;

                        // Validierung (schnell)
                        if (isSledgehammer && !SledgehammerUtils.shouldBreak(this.world, targetPos, mainPos, stack)) continue;
                        // Bei Vein/Strip Miner wurde die Validierung schon in MiningUtils gemacht,
                        // aber wir prüfen sicherheitshalber ob der Block noch existiert (Air check)
                        if (!isSledgehammer && world.getBlockState(targetPos).isAir()) continue;

                        BlockState state = this.world.getBlockState(targetPos);
                        if (!state.isAir()) {
                            matrices.push();
                            matrices.translate((double)targetPos.getX() - camX, (double)targetPos.getY() - camY, (double)targetPos.getZ() - camZ);
                            RenderLayer layer = (RenderLayer) ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage);
                            VertexConsumer consumer = vertexConsumers.getBuffer(layer);
                            VertexConsumer overlayConsumer = new OverlayVertexConsumer(consumer, matrices.peek(), 1.0f);

                            // Rendern
                            client.getBlockRenderManager().renderDamage(state, targetPos, this.world, matrices, overlayConsumer);

                            matrices.pop();
                        }
                    }
                }
            }
        }

        // Wenn der Spieler nichts mehr abbaut, Cache leeren um Speicher freizugeben
        if (!foundActiveBreak) {
            simplebuilding$lastMainPos = null;
            simplebuilding$cachedConnectedBlocks = Collections.emptyList();
        }
    }
}