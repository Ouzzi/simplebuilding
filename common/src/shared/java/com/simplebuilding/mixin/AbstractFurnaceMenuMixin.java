package com.simplebuilding.mixin;

import com.simplebuilding.util.LongCookContainerData;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Laesst den Fortschrittspfeil auch bei Kochzeiten ueber 32767 Ticks stimmen.
 *
 * <p>{@code ClientboundContainerSetDataPacket} schickt Menuedaten als short. Die Datenslots jedes
 * Ofenmenues (Vanilla und Mod, alle drei Familien) lesen deshalb durch
 * {@link LongCookContainerData}, das lange Zeiten vor dem Senden gemeinsam herunterteilt; das
 * Verhaeltnis, aus dem der Client Pfeil und Flamme zeichnet, bleibt dabei erhalten. Die eigentlichen
 * Ofenwerte bleiben unberuehrt. Ziel ist der Konstruktor mit Container und Daten, der als einziger
 * {@code addDataSlots} aufruft; ein blosses {@code "<init>"} findet Mixin hier nicht.
 * MC 26.2: MenuType, ResourceKey, RecipeBookType, int, Inventory, Container, ContainerData.
 */
@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin {

    @ModifyArg(method = "<init>(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/inventory/RecipeBookType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractFurnaceMenu;addDataSlots(Lnet/minecraft/world/inventory/ContainerData;)V"))
    private ContainerData simplebuilding$syncLongCooks(ContainerData data) {
        return new LongCookContainerData(data);
    }
}
