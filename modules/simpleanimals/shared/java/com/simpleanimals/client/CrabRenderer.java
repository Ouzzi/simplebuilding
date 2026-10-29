package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Crab;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Crab with two shell colours (red, sand). */
public class CrabRenderer extends MobRenderer<Crab, CrabRenderer.State, CrabModel> {
    private static final Identifier[] TEXTURES = {SimpleAnimals.id("textures/entity/crab/red.png"), SimpleAnimals.id("textures/entity/crab/sand.png")};

    public static class State extends LivingEntityRenderState {
        public int variant;
    }

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER)), 0.3F);
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
    public void extractRenderState(Crab entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
    }
}
