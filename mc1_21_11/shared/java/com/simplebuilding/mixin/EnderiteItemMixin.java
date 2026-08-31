package com.simplebuilding.mixin;

import com.simplebuilding.util.ModTags;
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

        // Der Void beginnt unterhalb der Bauhoehe der Dimension, nicht bei Y=0. Feste Konstanten
        // (frueher 0 bzw. -10) stammen aus der Zeit vor 1.18: seitdem liegt der Overworld-Boden
        // bei -64, wodurch jedes Enderite-Item unterhalb von Y=0 schweben blieb und ab Y=-10 nach
        // Y=5 teleportiert wurde - also mitten im normalen Abbaubereich.
        int minY = this.level().getMinY();
        if (this.getY() >= minY) return;

        ItemStack stack = this.getItem();

        // Geschuetzt wird ueber den Item-Tag simplebuilding:void_protected. Der frueher hier
        // stehende Check auf den Anzeigenamen ("Enderite" in getHoverName()) war sprachabhaengig:
        // in jeder nicht-englischen Lokalisierung griff der Schutz nicht, und ein im Amboss
        // umbenanntes Fremditem wurde faelschlich geschuetzt.
        boolean isVoidProtected = stack.is(ModTags.Items.VOID_PROTECTED);

        if (isVoidProtected) {
            // Physik manipulieren: Schweben lassen
            this.setDeltaMovement(0, 0, 0);
            this.setNoGravity(true);

            // Teleportieren zu sicherem Y, wenn zu tief (Vanilla loescht das Item bei minY - 64)
            if (this.getY() < minY - 10) {
                this.setPos(this.getX(), minY + 5, this.getZ());
                this.setDeltaMovement(0, 0, 0); // Reset Velocity nach TP
            }
        }
    }
}