package com.simplemachines.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplemachines.piston.DoublePistonBlock;
import com.simplemachines.piston.DoublePistonHeadBlock;
import com.simplemachines.piston.DoublePistons;
import com.simplemachines.upgrade.ModPistonHeadBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.properties.PistonType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Der bewegte Kolbenkopf im Bild. Vanillas {@code PistonHeadRenderer#extractRenderState} kennt den
 * Kopf nur als {@code Blocks.PISTON_HEAD}:
 * <ul>
 *   <li>Beim <b>Ausfahren</b> ist der bewegte Block der Kopf selbst; nur fuer
 *       {@code is(Blocks.PISTON_HEAD)} schaltet Vanilla in der ersten Haelfte der Bewegung auf die
 *       kurze Form, sonst ragte die Stange hinten aus dem Kolben. Fuer einen Kopf der Mod steht an
 *       dieser Stelle deshalb sein eigener Block.</li>
 *   <li>Beim <b>Einfahren</b> ist der bewegte Block der Kolben, und Vanilla baut den Kopf, der
 *       zurueckgleitet, fest aus {@code Blocks.PISTON_HEAD} (klebrig, wenn der Kolben
 *       {@code Blocks.STICKY_PISTON} ist). Fuer die Kolben der Mod stehen dort ihr Kopf
 *       ({@link ModPistonHeadBlock#headFor}) und, fuer den verstaerkten klebrigen Kolben, der Kolben
 *       selbst an Stelle des klebrigen Vanilla-Kolbens.</li>
 * </ul>
 * Doppelkolben (DW04C-6): sein Kopf ist beim Ausfahren wie ein Mod-Kopf selbst der bewegte Block (mit Vanillas
 * {@code short}). Beim Einfahren von Stufe 1 auf 0 ist wie bei Vanilla die Basis der bewegte Block; Vanillas Zweig dafuer
 * setzte {@code EXTENDED} an einem fremden Block und stuerzte ab, also wird er fuer den Doppelkolben uebersprungen und am Ende
 * mit dessen ausgefahrener Basis und zurueckgleitendem Kopf gefuellt.
 * Nur der voll beschriebene Methodenkopf: die Brueckenmethode mit {@code BlockEntity} traegt keinen
 * dieser Zugriffe.
 */
@Mixin(PistonHeadRenderer.class)
public class PistonUpgradeHeadRendererMixin {
    @Shadow
    private static MovingBlockRenderState createMovingBlock(BlockPos pos, BlockState blockState, Holder<Biome> biome, ClientLevel level) {
        throw new AssertionError();
    }

    private static final String EXTRACT = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;"
            + "Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;"
            + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V";

    @ModifyExpressionValue(method = EXTRACT, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
    private Block simplemachines$ownHead(Block original, @Local(argsOnly = true) PistonMovingBlockEntity blockEntity) {
        Block moved = blockEntity.getMovedState().getBlock();
        if (moved instanceof ModPistonHeadBlock || moved instanceof DoublePistonHeadBlock) {
            return moved;
        }
        Block own = ModPistonHeadBlock.headFor(moved);
        return own != null ? own : original;
    }

    @ModifyExpressionValue(method = EXTRACT, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;STICKY_PISTON:Lnet/minecraft/world/level/block/Block;"))
    private Block simplemachines$ownStickyBase(Block original, @Local(argsOnly = true) PistonMovingBlockEntity blockEntity) {
        Block moved = blockEntity.getMovedState().getBlock();
        return ModPistonHeadBlock.headFor(moved) != null && ModPistonHeadBlock.typeFor(moved) == PistonType.STICKY ? moved : original;
    }

    @ModifyExpressionValue(method = EXTRACT, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;isSourcePiston()Z"))
    private boolean simplemachines$notVanillaSource(boolean original, @Local(argsOnly = true) PistonMovingBlockEntity blockEntity) {
        return original && !(blockEntity.getMovedState().getBlock() instanceof DoublePistonBlock);
    }

    @Inject(method = EXTRACT, at = @At("TAIL"))
    private void simplemachines$doublePistonRetract(PistonMovingBlockEntity blockEntity, PistonHeadRenderState state, float partialTicks,
                                                    Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfo ci) {
        BlockState moved = blockEntity.getMovedState();
        if (!(moved.getBlock() instanceof DoublePistonBlock base) || !blockEntity.isSourcePiston() || blockEntity.isExtending()
                || !(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }
        Direction f = moved.getValue(DirectionalBlock.FACING);
        BlockPos headPos = blockEntity.getBlockPos().relative(f);
        Holder<Biome> biome = level.getBiome(headPos);
        BlockState head = base.head().defaultBlockState().setValue(DirectionalBlock.FACING, f)
                .setValue(DoublePistonHeadBlock.SHORT, blockEntity.getProgress(partialTicks) >= 0.5F);
        state.block = createMovingBlock(headPos, head, biome, level);
        state.base = createMovingBlock(blockEntity.getBlockPos(), moved.setValue(DoublePistons.EXT, 1), biome, level);
    }
}
