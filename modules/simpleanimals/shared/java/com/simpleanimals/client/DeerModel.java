package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/** Deer: four long legs, a leaning neck, ears, and antlers that are hidden on does and fawns (layout: tools/gen_models.py). */
public class DeerModel extends QuadrupedModel<DeerRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleAnimals.id("deer"), "main");
    private final ModelPart antlerRight, antlerLeft;

    public DeerModel(ModelPart root) {
        super(root);
        this.antlerRight = this.head.getChild("antler_right");
        this.antlerLeft = this.head.getChild("antler_left");
    }

    public static LayerDefinition createBodyLayer() {
        return DeerLayers.create();
    }

    @Override
    public void setupAnim(DeerRenderer.State state) {
        super.setupAnim(state);
        this.antlerRight.visible = state.antlers;
        this.antlerLeft.visible = state.antlers;
    }
}
