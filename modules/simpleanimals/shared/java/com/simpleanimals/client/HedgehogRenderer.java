package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Hedgehog;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Hedgehog with two coat variants (brown, grey) and the curl blend for the animation. */
public class HedgehogRenderer extends MobRenderer<Hedgehog, HedgehogRenderer.State, HedgehogModel> {
    private static final Identifier[] TEXTURES = {SimpleAnimals.id("textures/entity/hedgehog/brown.png"), SimpleAnimals.id("textures/entity/hedgehog/grey.png")};

    public static class State extends LivingEntityRenderState {
        public int variant;
        public float curl;
    }

    public HedgehogRenderer(EntityRendererProvider.Context context) {
        super(context, new HedgehogModel(context.bakeLayer(HedgehogModel.LAYER)), 0.25F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURES[state.variant == 1 ? 1 : 0];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Hedgehog entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
        state.curl = entity.curlAmount(partialTicks);
    }
}
