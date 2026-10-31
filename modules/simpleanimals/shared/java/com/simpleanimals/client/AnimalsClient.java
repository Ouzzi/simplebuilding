package com.simpleanimals.client;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/** Client registration shared by the loaders: the custom model layers (the renderers are registered by each loader). */
public final class AnimalsClient {
    /** Model layers: {@code sink.accept(location, definition supplier)}. */
    public static void modelLayers(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> sink) {
        sink.accept(DeerModel.LAYER, DeerModel::createBodyLayer);
        sink.accept(BoarModel.LAYER, BoarModel::createBodyLayer);
        sink.accept(HedgehogModel.LAYER, HedgehogModel::createBodyLayer);
        sink.accept(CrabModel.LAYER, CrabModel::createBodyLayer);
        sink.accept(CrabModel.FIDDLER_LAYER, CrabModel::createFiddlerLayer);
    }

    private AnimalsClient() {}
}
