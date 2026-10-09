package com.simplebuilding.mixin;

import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Glitzernde Melonenscheibe essbar (Queue Nachtrag 24): bekommt beim Bauen ihrer Komponenten Nahrung und Verzehr
 * ({@link com.simplebuilding.util.MaterialOctets#GLISTERING_MELON_SLICE_FOOD}). Am Item-Modell erkannt wie in
 * simplefun {@code EnchantabilityMixin}; Brauen und Rezepte bleiben unberuehrt.
 */
@Mixin(Item.Properties.class)
public class GlisteringMelonFoodMixin {

    @Inject(method = "finalizeInitializer", at = @At("HEAD"))
    private void simplebuilding$edibleGlisteringMelon(Component name, Identifier modelId,
                                                     CallbackInfoReturnable<DataComponentInitializers.Initializer<Item>> cir) {
        if (modelId.equals(Identifier.withDefaultNamespace("glistering_melon_slice"))) {
            ((Item.Properties) (Object) this).food(com.simplebuilding.util.MaterialOctets.GLISTERING_MELON_SLICE_FOOD);
        }
    }
}
