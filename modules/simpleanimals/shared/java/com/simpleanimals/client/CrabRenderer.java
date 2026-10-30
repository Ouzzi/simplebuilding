package com.simpleanimals.client;

import com.simpleanimals.AnimalVariants;
import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Crab;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Crab with two shell colours (red, sand). */
public class CrabRenderer extends MobRenderer<Crab, CrabRenderer.State, CrabModel> {
    private static final Identifier[] TEXTURES = AnimalVariants.CRAB.stream()
            .map(v -> SimpleAnimals.id("textures/entity/crab/" + v.name() + ".png")).toArray(Identifier[]::new);

    public static class State extends LivingEntityRenderState {
        public int variant;
    }

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER)), 0.3F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURES[AnimalVariants.clamp(AnimalVariants.CRAB, state.variant)];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Crab entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
    }
}
