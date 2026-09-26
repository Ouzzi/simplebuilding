package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.blocks.custom.ModPistonHeadBlock;
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
 * Nur der voll beschriebene Methodenkopf: die Brueckenmethode mit {@code BlockEntity} traegt keinen
 * dieser Zugriffe.
 */
@Mixin(PistonHeadRenderer.class)
public class PistonHeadRendererMixin {

    private static final String EXTRACT = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;"
            + "Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;"
            + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V";

    @ModifyExpressionValue(method = EXTRACT, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
    private Block simplebuilding$ownHead(Block original, @Local(argsOnly = true) PistonMovingBlockEntity blockEntity) {
        Block moved = blockEntity.getMovedState().getBlock();
        if (moved instanceof ModPistonHeadBlock) {
            return moved;
        }
        Block own = ModPistonHeadBlock.headFor(moved);
        return own != null ? own : original;
    }

    @ModifyExpressionValue(method = EXTRACT, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;STICKY_PISTON:Lnet/minecraft/world/level/block/Block;"))
    private Block simplebuilding$ownStickyBase(Block original, @Local(argsOnly = true) PistonMovingBlockEntity blockEntity) {
        Block moved = blockEntity.getMovedState().getBlock();
        return ModPistonHeadBlock.headFor(moved) != null && ModPistonHeadBlock.typeFor(moved) == PistonType.STICKY ? moved : original;
    }
}
