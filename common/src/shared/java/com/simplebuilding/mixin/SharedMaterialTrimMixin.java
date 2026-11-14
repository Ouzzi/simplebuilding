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
 * 26.3 (split stage 3d): the enderite ingot is SimpleLib's, since IG31-13 Simple Ores' ({@code simpleores:enderite_ingot}), the enderite armor trim
 * material stays SimpleBuilding's. When the ingot builds its components, it gets the trim material exactly as
 * SimpleBuilding's own ingot had it (26.2 registers that ingot itself and never reaches this). Matched by the item
 * model like {@link GlisteringMelonFoodMixin}, so the order SimpleLib/SimpleBuilding register in does not matter.
 */
@Mixin(Item.Properties.class)
public class SharedMaterialTrimMixin {
    private static final Identifier SIMPLEBUILDING$LIB_ENDERITE_INGOT = Identifier.fromNamespaceAndPath("simpleores", "enderite_ingot");
    /** N47-M2 (b-circ46): Astralit dust and Nihilit shard are Simple Ores' too; their trim materials stay SimpleBuilding's. */
    private static final Identifier SIMPLEBUILDING$ORES_ASTRALIT_DUST = Identifier.fromNamespaceAndPath("simpleores", "astralit_dust");
    private static final Identifier SIMPLEBUILDING$ORES_NIHILITH_SHARD = Identifier.fromNamespaceAndPath("simpleores", "nihilith_shard");

    @Inject(method = "finalizeInitializer", at = @At("HEAD"))
    private void simplebuilding$enderiteTrimMaterial(Component name, Identifier modelId,
                                                    CallbackInfoReturnable<DataComponentInitializers.Initializer<Item>> cir) {
        if (modelId.equals(SIMPLEBUILDING$LIB_ENDERITE_INGOT)) {
            ((Item.Properties) (Object) this).trimMaterial(ModTrimMaterials.ENDERITE);
        } else if (modelId.equals(SIMPLEBUILDING$ORES_ASTRALIT_DUST)) {
            ((Item.Properties) (Object) this).trimMaterial(ModTrimMaterials.ASTRALIT);
        } else if (modelId.equals(SIMPLEBUILDING$ORES_NIHILITH_SHARD)) {
            ((Item.Properties) (Object) this).trimMaterial(ModTrimMaterials.NIHILITH);
        }
    }
}
