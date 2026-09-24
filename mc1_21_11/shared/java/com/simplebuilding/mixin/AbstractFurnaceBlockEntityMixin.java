package com.simplebuilding.mixin;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Speichert die vier Ofen-Zeiten als int statt als short.
 *
 * <p>Vanilla schreibt Brenn- und Kochzeiten mit {@code putShort} und liest sie mit
 * {@code getShortOr}; alles ueber 32767 Ticks wird dabei abgeschnitten. Das Enderit-Schrott-Rezept
 * kocht 72000 Ticks: nach einem Neuladen des Chunks stand die Gesamtzeit auf 6464, ein Kochvorgang
 * endete dann viel zu frueh, blieb in einem Vanilla-Schmelzofen fuer immer haengen oder wurde in
 * einem Mod-Ofen sofort fertig. NeoForge (26.2 und 21.11) patcht genau das schon selbst, Forge
 * ebenso; noetig ist das Mixin fuer Fabric, auf den anderen Ladern schreibt es dieselben Werte
 * unter denselben Schluesseln noch einmal. Welten wandern damit sauber zwischen den Ladern.
 *
 * <p>Gelesen und geschrieben wird ueber {@code dataAccess} (Indizes 0-3 wie im Menue), damit das
 * Mixin auf beiden MC-Linien gleich bleibt - die Felder selbst sind auf 26.2 privat und auf 1.21.11
 * paketprivat.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow @Final protected ContainerData dataAccess;

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void simplebuilding$saveTimersAsInt(ValueOutput output, CallbackInfo ci) {
        output.putInt("lit_time_remaining", this.dataAccess.get(0));
        output.putInt("lit_total_time", this.dataAccess.get(1));
        output.putInt("cooking_time_spent", this.dataAccess.get(2));
        output.putInt("cooking_total_time", this.dataAccess.get(3));
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void simplebuilding$loadTimersAsInt(ValueInput input, CallbackInfo ci) {
        this.dataAccess.set(0, input.getIntOr("lit_time_remaining", this.dataAccess.get(0)));
        this.dataAccess.set(1, input.getIntOr("lit_total_time", this.dataAccess.get(1)));
        this.dataAccess.set(2, input.getIntOr("cooking_time_spent", this.dataAccess.get(2)));
        this.dataAccess.set(3, input.getIntOr("cooking_total_time", this.dataAccess.get(3)));
    }
}
