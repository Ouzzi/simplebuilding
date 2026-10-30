package com.simpledinos.client;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/** Client registration shared by the loaders: the custom model layers (the renderers are registered by each loader). */
public final class DinosClient {
    /** Model layers: {@code sink.accept(location, definition supplier)}. */
    public static void modelLayers(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> sink) {
        sink.accept(TrexModel.LAYER, TrexModel::createBodyLayer);
    }

    private DinosClient() {}
}
