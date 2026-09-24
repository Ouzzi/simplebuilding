package com.simplebuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.blocks.custom.ReinforcedPistonBlock;
import com.simplebuilding.util.PistonBreach;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonStructureResolver.class)
public class PistonHandlerMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private BlockPos pistonPos;
    @Shadow @Final private BlockPos startPos;
    @Shadow @Final private List<BlockPos> toPush;

    /**
     * Der Redstoneblock, der den Durchbruch dieses Resolvers bezahlt, oder null, wenn der Resolver
     * nicht scharf ist. Scharf ist er nur beim Ausfahren eines verstaerkten Kolbens (normal oder
     * klebrig) mit einem durchbrechbaren Block direkt vorn und einem Redstoneblock daneben, siehe
     * {@link PistonBreach}. Einfahren schaltet nie scharf, deshalb zieht ein klebriger verstaerkter
     * Kolben einen solchen Block nie zurueck.
     */
    @Unique
    private @Nullable BlockPos simplebuilding$fuel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void simplebuilding$armBreach(Level level, BlockPos pistonPos, Direction direction, boolean extending, CallbackInfo ci) {
        if (extending && level.getBlockState(pistonPos).getBlock() instanceof ReinforcedPistonBlock
                && PistonBreach.isBreachable(level, this.startPos)) {
            this.simplebuilding$fuel = PistonBreach.findFuel(level, pistonPos, direction);
        }
    }

    // WICHTIG: Nur EINE Methode benutzen.
    // Diese ersetzt ALLE "12"er in der Methode "tryMove" durch 18, wenn es unser Piston ist.
    @ModifyConstant(method = "addBlockLine", constant = @Constant(intValue = 12))
    private int modifyPistonLimit(int originalLimit) {
        BlockState state = this.level.getBlockState(this.pistonPos);

        if (state.getBlock() instanceof ReinforcedPistonBlock) {
            return 18; // Das neue Limit
        }

        return originalLimit;
    }

    /**
     * Der eigentliche Durchbruch: Nur im scharfen Resolver und nur fuer den Block direkt vorn
     * ({@code startPos}) gilt statt Vanillas {@code isPushable} die Fassung ohne Haerte- und
     * Namensregel ({@link PistonBreach#isPushableAsBreach}). Das betrifft jeden Aufruf an dieser
     * Stelle, auch einen ueber einen Schleimblock-Zweig. Ein zweiter durchbrechbarer Block irgendwo
     * sonst bleibt fuer Vanilla unverschiebbar: in einer Schubreihe verweigert er den ganzen Schub,
     * neben einem geschobenen Schleimblock bleibt er einfach stehen. Er zaehlt wie jeder andere
     * Block zum Schublimit. Andere Kolben und alle anderen Aufrufer von {@code isPushable} sehen
     * davon nichts.
     */
    @WrapOperation(method = {"resolve", "addBlockLine"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;isPushable(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;ZLnet/minecraft/core/Direction;)Z"))
    private boolean simplebuilding$breachTheFront(BlockState state, Level level, BlockPos pos, Direction direction,
                                                   boolean allowDestroyable, Direction connectionDirection,
                                                   Operation<Boolean> original) {
        if (this.simplebuilding$fuel != null && pos.equals(this.startPos) && PistonBreach.isBreachable(state, level, pos)) {
            return PistonBreach.isPushableAsBreach(state, level, pos, direction, allowDestroyable, connectionDirection);
        }
        return original.call(state, level, pos, direction, allowDestroyable, connectionDirection);
    }

    /**
     * Schleimblock-Aufbauten koennen den bezahlenden Redstoneblock hinter oder neben dem Kolben in
     * die Schubliste ziehen. Dann wuerde er mitgeschoben statt verbraucht, und der Durchbruch waere
     * umsonst; so ein Schub wird verweigert.
     */
    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$keepTheFuelInPlace(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && this.simplebuilding$fuel != null && this.toPush.contains(this.simplebuilding$fuel)) {
            cir.setReturnValue(false);
        }
    }
}
