package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.EndNetherMobs;
import com.simplebuilding.modules.simplemobs.trials.EvilBrewing;
import com.simplebuilding.modules.simplemobs.trials.SoulBrewing;
import com.simplebuilding.modules.simplemobs.trials.TrialKind;
import com.simplebuilding.modules.simplemobs.trials.Trials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BS27-5: soul charges and the Evil / Void brewing step of the Brewing Stand, see {@link EvilBrewing}. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandSoulMixin implements SoulBrewing {
    @Shadow private NonNullList<ItemStack> items;
    @Shadow private int brewTime;
    @Shadow private Item ingredient;
    @Unique private int simplemobs$soul;

    @Override
    public int simplemobs$soul() {
        return simplemobs$soul;
    }

    @Override
    public void simplemobs$setSoul(int charges) {
        simplemobs$soul = Math.max(0, charges);
    }

    @Override
    public boolean simplemobs$tick(Level level, BlockPos pos, BlockState state) {
        if (!Trials.evilOn()) return false;
        BlockEntity self = (BlockEntity) (Object) this;
        ItemStack fuel = items.get(EvilBrewing.FUEL_SLOT);
        if (simplemobs$soul <= 0 && fuel.is(EndNetherMobs.POWDER)) {
            fuel.shrink(1);
            simplemobs$soul = EvilBrewing.CHARGES;
            self.setChanged();
        }
        TrialKind kind = EvilBrewing.recipe(items);
        if (kind == null || !Trials.chainOn(kind)) return false;
        if (brewTime == 0) {
            if (simplemobs$soul <= 0) return false;       // no soul charge: Vanilla sees nothing it can brew and does nothing
            brewTime = EvilBrewing.BREW_TICKS;
            ingredient = items.get(EvilBrewing.INGREDIENT_SLOT).getItem();
            simplemobs$soul--;
        } else if (items.get(EvilBrewing.INGREDIENT_SLOT).getItem() != ingredient) {
            brewTime = 0;
        } else if (--brewTime <= 0) {
            brewTime = 0;
            EvilBrewing.finish(level, pos, items, kind);
        }
        self.setChanged();
        return true;
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void simplemobs$evilTick(ServerLevel level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity, CallbackInfo ci) {
        if (((SoulBrewing) (Object) entity).simplemobs$tick(level, pos, state)) ci.cancel();
    }

    /** The fuel slot also takes Soulfire Powder (hoppers ask the same method). */
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void simplemobs$soulFuel(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot == EvilBrewing.FUEL_SLOT && stack.is(EndNetherMobs.POWDER)) cir.setReturnValue(true);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void simplemobs$saveSoul(ValueOutput output, CallbackInfo ci) {
        if (simplemobs$soul > 0) output.putInt("simplemobs_soul", simplemobs$soul);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void simplemobs$loadSoul(ValueInput input, CallbackInfo ci) {
        simplemobs$soul = input.getIntOr("simplemobs_soul", 0);
    }
}
