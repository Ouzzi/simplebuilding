package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.EndPaintings;
import com.simplebuilding.modules.simpleend.EndRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An End painting drops (and is picked as) the End Painting item instead of Vanilla's painting. */
@Mixin(Painting.class)
public abstract class PaintingMixin {
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void simpleend$dropEndPainting(ServerLevel level, @Nullable Entity causedBy, CallbackInfo ci) {
        Painting self = (Painting) (Object) this;
        if (EndRegistry.END_PAINTING == null || !EndPaintings.isEnd(self.getVariant())) return;
        ci.cancel();
        if (!level.getGameRules().get(GameRules.ENTITY_DROPS)) return;
        self.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
        if (causedBy instanceof Player player && player.hasInfiniteMaterials()) return;
        ItemStack stack = new ItemStack(EndRegistry.END_PAINTING);
        stack.set(DataComponents.CUSTOM_NAME, self.getCustomName());
        self.spawnAtLocation(level, stack);
    }

    @Inject(method = "getPickResult", at = @At("HEAD"), cancellable = true)
    private void simpleend$pickEndPainting(CallbackInfoReturnable<ItemStack> cir) {
        Painting self = (Painting) (Object) this;
        if (EndRegistry.END_PAINTING != null && EndPaintings.isEnd(self.getVariant())) cir.setReturnValue(new ItemStack(EndRegistry.END_PAINTING));
    }
}
