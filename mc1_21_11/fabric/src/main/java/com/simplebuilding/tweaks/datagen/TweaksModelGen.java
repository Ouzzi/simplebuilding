package com.simplebuilding.tweaks.datagen;

import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

/**
 * Modelle des Simple-Tweaks-Teils (Simple Tweaks: ModModelProvider): alle Platten flach wie eine
 * gedrueckte Vanilla-Druckplatte ({@code pressure_plate_up}) mit ihrer eigenen Textur, die Items
 * flach. Der Echo-Kompass nimmt die Bilder des Bergungskompasses und zeigt zum verknuepften Leitstein.
 */
public final class TweaksModelGen {
    private TweaksModelGen() {
    }

    public static void blocks(BlockModelGenerators generator) {
        for (Block block : TweaksBlocks.all()) {
            Identifier model = ModelTemplates.PRESSURE_PLATE_UP.create(block, TextureMapping.defaultTexture(block), generator.modelOutput);
            generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model)));
            generator.registerSimpleItemModel(block, model);
        }
    }

    public static void items(ItemModelGenerators generator) {
        generator.generateFlatItem(TweaksItems.SPAWN_ELYTRA, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(TweaksItems.LASER_POINTER, ModelTemplates.FLAT_ITEM);
        generator.itemModelOutput.accept(TweaksItems.ECHO_COMPASS, ItemModelUtils.rangeSelect(
                new CompassAngle(true, CompassAngleState.CompassTarget.LODESTONE), 32.0F, recoveryCompassModels()));
    }

    /** Wie ItemModelGenerators#createCompassModels, aber mit den vorhandenen Vanilla-Bildern. */
    private static List<RangeSelectItemModel.Entry> recoveryCompassModels() {
        List<RangeSelectItemModel.Entry> overrides = new ArrayList<>();
        ItemModel.Unbaked base = ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/recovery_compass_16"));
        overrides.add(ItemModelUtils.override(base, 0.0F));
        for (int i = 1; i < 32; i++) {
            int index = Mth.positiveModulo(i - 16, 32);
            overrides.add(ItemModelUtils.override(ItemModelUtils.plainModel(
                    Identifier.withDefaultNamespace(String.format(Locale.ROOT, "item/recovery_compass_%02d", index))), i - 0.5F));
        }
        overrides.add(ItemModelUtils.override(base, 31.5F));
        return overrides;
    }
}
