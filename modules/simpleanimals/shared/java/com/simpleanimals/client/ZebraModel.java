package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;

/**
 * Zebra and zorse: the compact horse model B (owner choice 02.11.): deeper body, shorter jointed legs with hooves, thick neck, big
 * head and ears. It keeps the Vanilla horse's part names and neck pivot, so the Vanilla animation (head, legs, tail) drives it
 * unchanged (layout: tools/gen_models.py, ZebraLayers).
 */
public class ZebraModel extends AbstractEquineModel<EquineRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleAnimals.id("zebra"), "main");

    public ZebraModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return ZebraLayers.create();
    }
}
