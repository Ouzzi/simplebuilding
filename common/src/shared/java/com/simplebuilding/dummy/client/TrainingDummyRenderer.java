package com.simplebuilding.dummy.client;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.resources.Identifier;

/** Vanillas Ruestungsstaender-Modell mit Stroh-Textur; je Entity-Art eine Textur (Stroh-Staender, Trainingspuppe). */
public class TrainingDummyRenderer extends ArmorStandRenderer {
    private final Identifier texture;

    public TrainingDummyRenderer(EntityRendererProvider.Context context, String name, boolean stuffed) {
        super(context);
        this.texture = Identifier.fromNamespaceAndPath("simplebuilding", "textures/entity/training_dummy/" + name + ".png");
        if (stuffed) {
            this.addLayer(new DummyStuffingLayer(this));
        }
    }

    public static EntityRendererProvider<net.minecraft.world.entity.decoration.ArmorStand> straw() {
        return context -> new TrainingDummyRenderer(context, "straw_armor_stand", false);
    }

    public static EntityRendererProvider<net.minecraft.world.entity.decoration.ArmorStand> dummy() {
        return context -> new TrainingDummyRenderer(context, "training_dummy", true);
    }

    @Override
    public Identifier getTextureLocation(ArmorStandRenderState state) {
        return this.texture;
    }
}
