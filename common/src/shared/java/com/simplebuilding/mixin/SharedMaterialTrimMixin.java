package com.simplebuilding.mixin;

import com.simplebuilding.trim.ModTrimMaterials;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3 (split stage 3d): the enderite ingot is SimpleLib's ({@code simplelib:enderite_ingot}), the enderite armor trim
 * material stays SimpleBuilding's. When the ingot builds its components, it gets the trim material exactly as
 * SimpleBuilding's own ingot had it (26.2 registers that ingot itself and never reaches this). Matched by the item
 * model like {@link GlisteringMelonFoodMixin}, so the order SimpleLib/SimpleBuilding register in does not matter.
 */
@Mixin(Item.Properties.class)
public class SharedMaterialTrimMixin {
    private static final Identifier SIMPLEBUILDING$LIB_ENDERITE_INGOT = Identifier.fromNamespaceAndPath("simplelib", "enderite_ingot");

    @Inject(method = "finalizeInitializer", at = @At("HEAD"))
    private void simplebuilding$enderiteTrimMaterial(Component name, Identifier modelId,
                                                    CallbackInfoReturnable<DataComponentInitializers.Initializer<Item>> cir) {
        if (modelId.equals(SIMPLEBUILDING$LIB_ENDERITE_INGOT)) {
            ((Item.Properties) (Object) this).trimMaterial(ModTrimMaterials.ENDERITE);
        }
    }
}
