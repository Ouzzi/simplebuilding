package com.simpleanimals.client;

import com.simpleanimals.AnimalVariants;
import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Hedgehog;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Hedgehog with coat variants by biome ({@link AnimalVariants}). */
public class HedgehogRenderer extends MobRenderer<Hedgehog, HedgehogRenderer.State, HedgehogModel> {
    private static final Identifier[] TEXTURES = AnimalVariants.HEDGEHOG.stream()
            .map(v -> SimpleAnimals.id("textures/entity/hedgehog/" + v.name() + ".png")).toArray(Identifier[]::new);

    public static class State extends LivingEntityRenderState {
        public int variant;
        public float curl;
    }

    public HedgehogRenderer(EntityRendererProvider.Context context) {
        super(context, new HedgehogModel(context.bakeLayer(HedgehogModel.LAYER)), 0.25F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURES[AnimalVariants.clamp(AnimalVariants.HEDGEHOG, state.variant)];
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
