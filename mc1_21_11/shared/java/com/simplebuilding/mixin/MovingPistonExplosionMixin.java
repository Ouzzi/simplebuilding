package com.simplebuilding.mixin;

import com.simplebuilding.util.PistonBreach;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ein bewegter Block steckt zwei Ticks lang in einem {@code minecraft:moving_piston}, und der hat
 * Explosionswiderstand 0. Vanilla bewegt nie einen unzerstoerbaren Block, deshalb faellt das dort
 * nicht auf; seit die verstaerkten Kolben einen durchbrechbaren Block schieben ({@link PistonBreach}),
 * wuerde TNT in diesen zwei Ticks Grundgestein oder verstaerkten Tiefenschiefer loeschen. Solange der
 * bewegte Block zu dieser Klasse gehoert, ignoriert der bewegte Kolben die Explosion.
 *
 * <p>{@code MovingPistonBlock} ueberschreibt {@code onExplosionHit} nicht, deshalb sitzt der Haken
 * in {@code BlockBehaviour}. NeoForge ersetzt dort nur das Ende (onBlockExploded), der Kopf bleibt.
 */
@Mixin(BlockBehaviour.class)
public abstract class MovingPistonExplosionMixin {

    @Inject(method = "onExplosionHit", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$shieldMovingUnbreakables(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                                          BiConsumer<ItemStack, BlockPos> onHit, CallbackInfo ci) {
        if (state.getBlock() instanceof MovingPistonBlock
                && level.getBlockEntity(pos) instanceof PistonMovingBlockEntity moving
                && PistonBreach.isUnbreakableClass(moving.getMovedState(), level, pos)) {
            ci.cancel();
        }
    }
}
