package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/** Wild boar: stocky body with a bristle ridge, wedge head, snout and two tusks (layout: tools/gen_models.py). */
public class BoarModel extends QuadrupedModel<BoarRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleAnimals.id("boar"), "main");

    public BoarModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return BoarLayers.create();
    }
}
