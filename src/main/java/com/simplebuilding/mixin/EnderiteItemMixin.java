package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class EnderiteItemMixin extends Entity {

    public EnderiteItemMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Shadow public abstract ItemStack getItem();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.level().isClientSide()) return;

        // Prüfen ob wir im Void sind (z.B. Y < -30 im End, normal ist 0)
        if (this.getY() < 0) {
            ItemStack stack = this.getItem();
            
            // Liste der geschützten Items
            boolean isEnderite = stack.getItem() == ModItems.ENDERITE_INGOT 
                    || stack.getItem() == ModItems.ENDERITE_SCRAP
                    || stack.getHoverName().getString().contains("Enderite"); // Einfacher Check für Tools/Rüstung

            if (isEnderite) {
                // Physik manipulieren: Schweben lassen
                this.setDeltaMovement(0, 0, 0);
                this.setNoGravity(true);
                
                // Teleportieren zu sicherem Y, wenn zu tief (optional, z.B. Y=5)
                if (this.getY() < -10) {
                    this.setPos(this.getX(), 5, this.getZ());
                    this.setDeltaMovement(0, 0, 0); // Reset Velocity nach TP
                }
            }
        }
    }
}