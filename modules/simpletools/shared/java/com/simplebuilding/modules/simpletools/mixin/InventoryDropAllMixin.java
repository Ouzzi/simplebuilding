package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soulbound stacks sit out the death drop and go back into their slots of the dead player. */
@Mixin(Inventory.class)
public abstract class InventoryDropAllMixin {
    @Shadow @Final public Player player;
    @Unique private List<Soulbound.Kept> simpletools$kept = List.of();

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void simpletools$keepSoulbound(CallbackInfo ci) {
        simpletools$kept = Soulbound.dying(player) ? Soulbound.takeOut((Inventory) (Object) this) : List.of();
    }

    @Inject(method = "dropAll", at = @At("RETURN"))
    private void simpletools$putBack(CallbackInfo ci) {
        List<Soulbound.Kept> kept = simpletools$kept;
        simpletools$kept = List.of();
        if (!kept.isEmpty()) Soulbound.putBack((Inventory) (Object) this, kept);
    }
}
