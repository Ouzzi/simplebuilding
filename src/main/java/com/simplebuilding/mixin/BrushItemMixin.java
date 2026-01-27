package com.simplebuilding.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushItem.class)
public class BrushItemMixin {

    // Verhindert, dass der Pinsel die Benutzung abbricht, wenn man auf ein Item Frame zielt
    @Inject(method = "usageTick", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$allowBrushingFrames(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        // Wir prüfen, worauf der Spieler schaut (Reichweite ca. 5 Blöcke, wie beim normalen Brush)
        HitResult hit = user.raycast(5.0d, 0.0f, false);
        
        // Wenn es ein Entity ist und dieses Entity ein Item Frame ist...
        if (hit.getType() == HitResult.Type.ENTITY) {
            if (world.getOtherEntities(user, user.getBoundingBox().expand(5.0), e -> e instanceof ItemFrameEntity).stream().anyMatch(e -> e.getBoundingBox().intersects(hit.getPos(), hit.getPos().add(0.1, 0.1, 0.1)))) {
                // ...dann brechen wir die Vanilla-Logik ab (die sonst "releaseUseItem" aufrufen würde)
                // Dadurch läuft die "Use"-Animation einfach weiter.
                ci.cancel();
            }
        }
    }
}